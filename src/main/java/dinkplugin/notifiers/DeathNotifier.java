package dinkplugin.notifiers;

import dinkplugin.domain.AccountType;
import dinkplugin.domain.Danger;
import dinkplugin.domain.ExceptionalDeath;
import dinkplugin.message.Embed;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Region;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DeathNotifier extends BaseNotifier {

    private static final String ATTACK_OPTION = "Attack";

    private static final String TOA_DEATH_MSG = "You failed to survive the Tombs of Amascut";

    private static final String TOB_DEATH_MSG = "Your party has failed";

    private static final String FORTIS_DOOM_MSG = "You have been doomed!";

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bclientscript,tob_hud_portal%5D.cs2">CS2 Reference</a>
     */
    private static final int TOB_HUB_PORTAL_SCRIPT = 2307;

    /**
     * Checks whether the actor is alive and interacting with the specified player.
     */
    private static final BiPredicate<Player, Actor> INTERACTING;

    /**
     * Checks whether a NPC is a valid candidate to be our killer.
     */
    private static final Predicate<NPCComposition> NPC_VALID;

    /**
     * Orders NPCs by their likelihood of being our killer.
     */
    private static final BiFunction<NPCManager, Player, Comparator<NPC>> NPC_COMPARATOR;

    /**
     * Orders actors by their likelihood of being the killer of the specified player.
     */
    private static final Function<Player, Comparator<Player>> PK_COMPARATOR;

    /**
     * User-specified Region IDs where death notifications should not be triggered.
     */
    private final Collection<Integer> ignoredRegions = new HashSet<>();

    @Inject
    private ItemManager itemManager;

    @Inject
    private NPCManager npcManager;

    /**
     * Tracks the last {@link Actor} our local player interacted with,
     * for the purposes of attributing deaths to particular {@link Player}'s.
     * <p>
     * Note: this is wrapped in a weak reference to allow garbage collection,
     * for example if the {@link Actor} despawns.
     * As a result, the underlying reference can be null.
     *
     * @see #identifyKiller()
     */
    private WeakReference<Actor> lastTarget = new WeakReference<>(null);

    @Override
    public boolean isEnabled() {
        return config.notifyDeath() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.deathWebhook();
    }

    public void init() {
        setIgnoredRegions(config.deathIgnoredRegions());
    }

    public void reset() {
        setIgnoredRegions(null);
    }

    public void onConfigChanged(String key, String value) {
        if ("deathIgnoredRegions".equals(key)) {
            setIgnoredRegions(value);
        }
    }

    public void onActorDeath(ActorDeath actor) {
        boolean self = client.getLocalPlayer() == actor.getActor();

        if (self && isEnabled())
            handleNotify(null);

        if (self || actor.getActor() == lastTarget.get())
            lastTarget = new WeakReference<>(null);
    }

    public void onGameMessage(String message) {
        var player = client.getLocalPlayer();
        if (message.equals(FORTIS_DOOM_MSG) && player.getHealthRatio() > 0 && WorldUtils.getLocation(client, player).getRegionID() == WorldUtils.FORTIS_REGION && isEnabled()) {
            // https://github.com/pajlads/DinkPlugin/issues/472
            // Doom modifier can kill the player without health reaching zero, so ActorDeath isn't fired
            handleNotify(Danger.DANGEROUS);
            return;
        }

        if (shouldNotifyExceptionalDangerousDeath(ExceptionalDeath.TOA) && message.contains(TOA_DEATH_MSG)) {
            // https://github.com/pajlads/DinkPlugin/issues/316
            // though, hardcore (group) ironmen just use the normal ActorDeath trigger for TOA
            handleNotify(Danger.DANGEROUS);
        }
    }

    public void onScript(ScriptPreFired event) {
        if (event.getScriptId() == TOB_HUB_PORTAL_SCRIPT && event.getScriptEvent() != null &&
            shouldNotifyExceptionalDangerousDeath(ExceptionalDeath.TOB)) {
            Object[] args = event.getScriptEvent().getArguments();
            if (args != null && args.length > 1) {
                Object text = args[1];
                if (text instanceof String && ((String) text).contains(TOB_DEATH_MSG)) {
                    // https://oldschool.runescape.wiki/w/Theatre_of_Blood#Death_within_the_Theatre
                    handleNotify(Danger.DANGEROUS);
                }
            }
        }
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() == client.getLocalPlayer() && event.getTarget() != null && event.getTarget().getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(event.getTarget());
        }
    }

    private void handleNotify(Danger dangerOverride) {
        int regionId = WorldUtils.getLocation(client).getRegionID();
        if (ignoredRegions.contains(regionId))
            return;

        Danger danger = dangerOverride != null ? dangerOverride : WorldUtils.getDangerLevel(client, regionId, config.deathSafeExceptions());
        if (danger == Danger.SAFE && config.deathIgnoreSafe())
            return;

        Collection<Item> items = ItemUtils.getItems(client);
        List<Pair<Item, Long>> itemsByPrice = getPricedItems(itemManager, items);

        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split;
        if (danger == Danger.DANGEROUS) {
            int keepCount = getKeepCount();
            split = splitItemsByKept(itemsByPrice, keepCount);
        } else {
            split = Pair.of(itemsByPrice, Collections.emptyList());
        }
        List<Pair<Item, Long>> keptItems = split.getLeft();
        List<Pair<Item, Long>> lostItems = split.getRight();

        long losePrice = lostItems.stream()
            .mapToLong(pair -> pair.getValue() * pair.getKey().getQuantity())
            .sum();

        int valueThreshold = config.deathMinValue();
        if (danger == Danger.DANGEROUS && losePrice < valueThreshold) {
            log.debug("Skipping death notification; total value of lost items {} is below minimum lost value {}", losePrice, valueThreshold);
            return;
        }

        Actor killer = identifyKiller();
        boolean pk = killer instanceof Player;
        boolean npc = killer instanceof NPC;
        String killerName = killer != null ? StringUtils.defaultIfEmpty(killer.getName(), "?") : null;
        Template notifyMessage = buildMessage(killerName, losePrice, pk, npc);

        List<SerializedItemStack> lostStacks = getStacks(itemManager, lostItems, true);
        List<SerializedItemStack> keptStacks = getStacks(itemManager, keptItems, false);
        List<Embed> keptItemEmbeds;
        if (config.deathEmbedKeptItems()) {
            keptItemEmbeds = ItemUtils.buildEmbeds(
                keptItems.stream()
                    .map(Pair::getKey)
                    .mapToInt(Item::getId)
                    .distinct()
                    .toArray()
            );
        } else {
            keptItemEmbeds = Collections.emptyList();
        }

        DeathNotificationData extra = new DeathNotificationData(
            losePrice,
            pk,
            pk ? killerName : null,
            killerName,
            npc ? ((NPC) killer).getId() : null,
            keptStacks,
            lostStacks,
            Region.of(client, regionId)
        );

        createMessage(config.deathSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .extra(extra)
            .embeds(keptItemEmbeds)
            .type(NotificationType.DEATH)
            .build());
    }

    private Template buildMessage(String killer, long losePrice, boolean pk, boolean npc) {
        boolean pvp = pk && config.deathNotifPvpEnabled();
        String template;
        if (pvp)
            template = config.deathNotifPvpMessage();
        else
            template = config.deathNotifyMessage();

        Template.TemplateBuilder builder = Template.builder()
            .template(template)
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%VALUELOST%", Replacements.ofText(String.valueOf(losePrice)));
        if (pvp) {
            builder.replacement("%PKER%", Replacements.ofText(killer));
        } else if (npc) {
            builder.replacement("%NPC%", Replacements.ofWiki(killer));
        }
        return builder.build();
    }

    private boolean shouldNotifyExceptionalDangerousDeath(ExceptionalDeath death) {
        if (!config.deathIgnoreSafe()) {
            // safe death notifications are enabled => we already notified
            return false;
        }

        if (config.deathSafeExceptions().contains(death)) {
            // safe deaths are ignored, but this death is exceptional => we already notified
            return false;
        }

        if (Utils.getAccountType(client).isHardcore() && death != ExceptionalDeath.FIGHT_CAVE) {
            // the PvM death is actually dangerous since hardcore => we already notified
            return false;
        }

        // notifier must be enabled to dink when the actually dangerous death occurs
        return isEnabled();
    }

    @Synchronized
    private void setIgnoredRegions(@Nullable String configValue) {
        ignoredRegions.clear();
        ConfigUtil.readDelimited(configValue).forEach(str -> {
            try {
                int regionId = Integer.parseInt(str);
                ignoredRegions.add(regionId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse death ignored region as integer: {}", str);
            }
        });
        log.debug("Updated ignored regions to: {}", ignoredRegions);
    }

    /**
     * @return the number of items the player would keep on an unsafe death
     */
    private int getKeepCount() {
        if (Utils.getAccountType(client) == AccountType.ULTIMATE_IRONMAN)
            return 0;

        int keepCount;
        if (client.getLocalPlayer().getSkullIcon() == null)
            keepCount = 3;
        else
            keepCount = 0;
        if (client.isPrayerActive(Prayer.PROTECT_ITEM))
            keepCount++;
        return keepCount;
    }

    /**
     * @return the inferred {@link Actor} who killed us, or null if not killed by an external source
     */
    @Nullable
    private Actor identifyKiller() {
        // must be in unsafe wildness or pvp world to be pk'd
        boolean pvpEnabled = !WorldUtils.isPvpSafeZone(client) &&
            (client.getVarbitValue(Varbits.IN_WILDERNESS) > 0 || WorldUtils.isPvpWorld(client.getWorldType()));

        Player localPlayer = client.getLocalPlayer();
        Predicate<Actor> interacting = a -> INTERACTING.test(localPlayer, a);

        // O(1) fast path based on last outbound interaction
        Actor lastTarget = this.lastTarget.get();
        if (checkLastInteraction(localPlayer, lastTarget, pvpEnabled))
            return lastTarget;

        // find another player interacting with us (that is preferably not a friend or clan member)
        if (pvpEnabled) {
            Optional<Player> pker = Arrays.stream(client.getCachedPlayers())
                .filter(interacting)
                .min(PK_COMPARATOR.apply(localPlayer)); // O(n)
            if (pker.isPresent())
                return pker.get();
        }

        // otherwise search through NPCs interacting with us
        return Arrays.stream(client.getCachedNPCs())
            .filter(interacting)
            .filter(npc -> NPC_VALID.test(npc.getTransformedComposition()))
            .min(NPC_COMPARATOR.apply(npcManager, localPlayer)) // O(n)
            .orElse(null);
    }

    /**
     * @param localPlayer {@link net.runelite.api.Client#getLocalPlayer()}
     * @param actor       the {@link Actor} that is a candidate killer from {@link #lastTarget}
     * @param pvpEnabled  whether a player could be our killer (e.g., in wilderness)
     * @return whether the specified actor is the likely killer of the local player
     */
    private static boolean checkLastInteraction(Player localPlayer, Actor actor, boolean pvpEnabled) {
        if (!INTERACTING.test(localPlayer, actor))
            return false;

        if (actor instanceof Player) {
            Player other = (Player) actor;
            return pvpEnabled && !other.isClanMember() && !other.isFriend() && !other.isFriendsChatMember();
        }

        if (actor instanceof NPC) {
            NPCComposition npc = ((NPC) actor).getTransformedComposition();
            return NPC_VALID.test(npc) && ArrayUtils.contains(npc.getActions(), ATTACK_OPTION);
        }

        log.warn("Encountered unknown type of Actor; was neither Player nor NPC!");
        return false;
    }

    /**
     * @param itemManager {@link ItemManager}
     * @param items       the items whose prices should be queried
     * @return pairs of the passed items to their price, sorted by most expensive unit price first
     */
    @NotNull
    private static List<Pair<Item, Long>> getPricedItems(ItemManager itemManager, Collection<Item> items) {
        return items.stream()
            .map(item -> Pair.of(item, ItemUtils.getPrice(itemManager, item.getId())))
            .sorted(Comparator.<Pair<Item, Long>>comparingLong(Pair::getValue).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Takes the complete list of items in the player's inventory and assigns them to separate lists,
     * depending on whether they would be kept or lost upon an unsafe death.
     *
     * @param itemsByPrice inventory items transformed by {@link #getPricedItems(ItemManager, Collection)}
     * @param keepCount    the number of items kept on death
     * @return the kept items on death (left) and lost items on death (right), in stable order, in separate lists
     */
    @NotNull
    @VisibleForTesting
    static Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> splitItemsByKept(List<Pair<Item, Long>> itemsByPrice, int keepCount) {
        final List<Pair<Item, Long>> keep = new ArrayList<>(keepCount);
        final List<Pair<Item, Long>> lost = new ArrayList<>(Math.max(itemsByPrice.size() - keepCount, 0));

        int kept = 0;
        for (Pair<Item, Long> item : itemsByPrice) {
            int id = item.getKey().getId();

            if (id == ItemID.OLD_SCHOOL_BOND || id == ItemID.OLD_SCHOOL_BOND_UNTRADEABLE) {
                // deliberately do not increment kept
                keep.add(item);
                continue;
            }

            boolean neverKept = ItemUtils.isItemNeverKeptOnDeath(id);
            for (int i = 0; i < item.getKey().getQuantity(); i++) {
                if (kept < keepCount && !neverKept) {
                    keep.add(Pair.of(new Item(id, 1), item.getValue()));
                    kept++;
                } else {
                    lost.add(Pair.of(new Item(id, item.getKey().getQuantity() - i), item.getValue()));
                    break;
                }
            }
        }

        return Pair.of(keep, lost);
    }

    /**
     * Converts {@code pricedItems} into {@link SerializedItemStack} with optional reduction
     * (to reflect the cumulative item quantity across inventory slots).
     *
     * @param itemManager {@link ItemManager}
     * @param pricedItems the items to be converted into {@link SerializedItemStack}
     * @param reduce      whether multiple stacks of the same item should be aggregated to a single stack
     * @return the (optionally reduced) {@link SerializedItemStack}'s associated with {@code pricedItems}
     */
    @NotNull
    private static List<SerializedItemStack> getStacks(ItemManager itemManager, List<Pair<Item, Long>> pricedItems, boolean reduce) {
        Collection<Item> items = pricedItems.stream().map(Pair::getLeft).collect(Collectors.toList());
        if (reduce) {
            items = ItemUtils.reduceItems(items).values();
        }
        return items.stream()
            .map(item -> ItemUtils.stackFromItem(itemManager, item))
            .collect(Collectors.toList());
    }

    static {
        INTERACTING = (localPlayer, a) -> a != null && !a.isDead() && a.getInteracting() == localPlayer;

        NPC_VALID = comp -> comp != null && comp.isInteractible() && !comp.isFollower() && comp.getCombatLevel() > 0;

        NPC_COMPARATOR = (npcManager, localPlayer) -> Comparator
            .comparing(
                NPC::getTransformedComposition,
                Comparator.nullsFirst(
                    Comparator
                        .comparing(
                            (NPCComposition comp) -> comp.getStringValue(ParamID.NPC_HP_NAME),
                            Comparator.comparing(StringUtils::isNotEmpty) // prefer has name in hit points UI
                        )
                        .thenComparing(comp -> ArrayUtils.contains(comp.getActions(), ATTACK_OPTION)) // prefer explicitly attackable
                        .thenComparingInt(NPCComposition::getCombatLevel) // prefer high level
                        .thenComparingInt(NPCComposition::getSize) // prefer large
                        .thenComparing(NPCComposition::isMinimapVisible) // prefer visible on minimap
                        .thenComparing(
                            // prefer high max health
                            comp -> npcManager.getHealth(comp.getId()),
                            Comparator.nullsFirst(Comparator.naturalOrder())
                        )
                )
            )
            .thenComparingInt(p -> -localPlayer.getLocalLocation().distanceTo(p.getLocalLocation())) // prefer nearby
            .reversed(); // for consistency with PK_COMPARATOR such that Stream#min should be used in #identifyKiller

        PK_COMPARATOR = localPlayer -> Comparator
            .comparing(Player::isClanMember) // prefer not in clan
            .thenComparing(Player::isFriend) // prefer not friend
            .thenComparing(Player::isFriendsChatMember) // prefer not fc
            .thenComparingInt(p -> Math.abs(localPlayer.getCombatLevel() - p.getCombatLevel())) // prefer similar level
            .thenComparingInt(p -> -p.getCombatLevel()) // prefer higher level for a given absolute level gap
            .thenComparing(p -> p.getOverheadIcon() == null) // prefer praying
            .thenComparing(p -> p.getTeam() == localPlayer.getTeam()) // prefer different team cape
            .thenComparingInt(p -> localPlayer.getLocalLocation().distanceTo(p.getLocalLocation())); // prefer nearby
    }
}
