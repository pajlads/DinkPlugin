package dinkplugin.notifiers;

import dinkplugin.message.Embed;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.WorldUtils;
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
import net.runelite.api.vars.AccountType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
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
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DeathNotifier extends BaseNotifier {

    private static final BiPredicate<Player, Actor> INTERACTING;
    private static final Predicate<NPCComposition> NPC_PREDICATE;
    private static final Function<Player, Comparator<Player>> PK_COMPARATOR; // less is better (i.e., use min)
    private static final Function<NPCManager, Comparator<NPC>> NPC_COMPARATOR; // less is worse (i.e., use max)

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

    private WeakReference<Actor> lastIncoming = new WeakReference<>(null);

    @Override
    public boolean isEnabled() {
        return config.notifyDeath() && super.isEnabled() && !WorldUtils.isSafeArea(client);
    }

    @Override
    protected String getWebhookUrl() {
        return config.deathWebhook();
    }

    public void onActorDeath(ActorDeath event) {
        Actor actor = event.getActor();
        boolean self = client.getLocalPlayer() == actor;

        if (self && isEnabled())
            handleNotify();

        if (self || actor == lastTarget.get())
            lastTarget = new WeakReference<>(null);

        if (self || actor == lastIncoming.get())
            lastIncoming = new WeakReference<>(null);
    }

    public void onInteraction(InteractingChanged event) {
        Actor source = event.getSource();
        Actor target = event.getTarget();
        if (source == client.getLocalPlayer() && target != null && target.getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(target);
        } else if (target == client.getLocalPlayer() && source != null && source.getCombatLevel() > 0) {
            lastIncoming = new WeakReference<>(source);
        }
    }

    private void handleNotify() {
        Collection<Item> items = ItemUtils.getItems(client);
        List<Pair<Item, Long>> itemsByPrice = getPricedItems(itemManager, items);

        int keepCount = getKeepCount();
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(itemsByPrice, keepCount);
        List<Pair<Item, Long>> keptItems = split.getLeft();
        List<Pair<Item, Long>> lostItems = split.getRight();

        long losePrice = lostItems.stream()
            .map(Pair::getRight)
            .reduce(Long::sum)
            .orElse(0L);

        int valueThreshold = config.deathMinValue();
        if (losePrice < valueThreshold) {
            log.debug("Skipping death notification; total value of lost items {} is below minimum lost value {}", losePrice, valueThreshold);
            return;
        }

        Actor killer = identifyKiller();
        String killerName = killer != null ? StringUtils.defaultIfEmpty(killer.getName(), "?") : null;
        boolean pk = killer instanceof Player;
        boolean npc = killer instanceof NPC;
        String notifyMessage = buildMessage(killerName, pk, npc, losePrice);

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
            lostStacks
        );

        createMessage(config.deathSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .extra(extra)
            .embeds(keptItemEmbeds)
            .type(NotificationType.DEATH)
            .build());
    }

    private String buildMessage(String killer, boolean pk, boolean npc, long losePrice) {
        boolean pvp = pk && config.deathNotifPvpEnabled();
        String template;
        if (pvp)
            template = config.deathNotifPvpMessage();
        else
            template = config.deathNotifyMessage();
        String notifyMessage = template
            .replace("%USERNAME%", Utils.getPlayerName(client))
            .replace("%VALUELOST%", String.valueOf(losePrice));
        if (pvp) {
            notifyMessage = notifyMessage.replace("%PKER%", killer);
        } else if (npc) {
            notifyMessage = notifyMessage.replace("%NPC%", killer);
        }
        return notifyMessage;
    }

    /**
     * @return the number of items the player would keep on an unsafe death
     */
    private int getKeepCount() {
        if (client.getAccountType() == AccountType.ULTIMATE_IRONMAN)
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
        boolean pvpEnabled = !WorldUtils.isPvpSafeZone(client) && (client.getVarbitValue(Varbits.IN_WILDERNESS) > 0 || WorldUtils.isPvpWorld(client.getWorldType()));
        Player localPlayer = client.getLocalPlayer();

        // O(1) fast path based on last outbound interaction
        Actor lastTarget = this.lastTarget.get();
        if (checkLastInteraction(localPlayer, lastTarget, pvpEnabled))
            return lastTarget;

        // O(1) fast path based on last inbound interaction
        Actor lastIncoming = this.lastIncoming.get();
        if (checkLastInteraction(localPlayer, lastIncoming, pvpEnabled))
            return lastIncoming;

        Predicate<Actor> interacting = a -> INTERACTING.test(localPlayer, a);

        // find another player interacting with us (that is preferably not a friend or clan member)
        if (pvpEnabled) {
            Optional<Player> likelyPker = Arrays.stream(client.getCachedPlayers())
                .filter(interacting)
                .min(PK_COMPARATOR.apply(localPlayer)); // O(n)
            if (likelyPker.isPresent())
                return likelyPker.get();
        }

        // otherwise search through NPCs interacting with us
        return Arrays.stream(client.getCachedNPCs())
            .filter(interacting)
            .filter(npc -> NPC_PREDICATE.test(npc.getTransformedComposition()))
            .max(NPC_COMPARATOR.apply(npcManager)) // O(n)
            .orElse(null);
    }

    private static boolean checkLastInteraction(Player localPlayer, Actor actor, boolean pvpEnabled) {
        if (!INTERACTING.test(localPlayer, actor))
            return false;

        if (actor instanceof Player) {
            Player other = (Player) actor;
            return pvpEnabled && !other.isClanMember() && !other.isFriend() && !other.isFriendsChatMember();
        }

        if (actor instanceof NPC) {
            NPCComposition npc = ((NPC) actor).getTransformedComposition();
            return NPC_PREDICATE.test(npc) && hasAttackOption(npc.getActions());
        }

        log.warn("Encountered unknown type of Actor; was neither Player nor NPC!");
        return false;
    }

    private static boolean hasAttackOption(String[] actions) {
        if (actions != null) {
            for (String action : actions) {
                if ("Attack".equalsIgnoreCase(action))
                    return true;
            }
        }
        return false;
    }

    /**
     * @param itemManager {@link ItemManager}
     * @param items       the items whose prices should be queried
     * @return pairs of the passed items to their price, sorted by most expensive first
     */
    @NotNull
    private static List<Pair<Item, Long>> getPricedItems(ItemManager itemManager, Collection<Item> items) {
        return items.stream()
            .map(item -> Pair.of(item, ItemUtils.getPrice(itemManager, item.getId()) * item.getQuantity()))
            .sorted((a, b) -> Math.toIntExact(b.getRight() - a.getRight()))
            .collect(Collectors.toList());
    }

    /**
     * Takes the complete list of items in the player's inventory and assigns them to separate lists,
     * depending on whether they would be kept or lost upon an unsafe death.
     *
     * @param itemsByPrice inventory items transformed by {@link #getPricedItems(ItemManager, Collection)}
     * @param keepCount    the number of items kept on death
     * @param <K>          the type of each entry; item paired with its unit price
     * @return the kept items on death (left) and lost items on death (right), in stable order, in separate lists
     */
    @NotNull
    @VisibleForTesting
    static <K extends Pair<Item, Long>> Pair<List<K>, List<K>> splitItemsByKept(List<K> itemsByPrice, int keepCount) {
        final List<K> keep = new ArrayList<>(keepCount);
        final List<K> lost = new ArrayList<>(Math.max(itemsByPrice.size() - keepCount, 0));

        int kept = 0;
        for (K item : itemsByPrice) {
            int id = item.getKey().getId();

            if (id == ItemID.OLD_SCHOOL_BOND || id == ItemID.OLD_SCHOOL_BOND_UNTRADEABLE) {
                // deliberately do not increment kept
                keep.add(item);
                continue;
            }

            if (kept < keepCount && !ItemUtils.isItemNeverKeptOnDeath(id)) {
                keep.add(item);
                kept++;
            } else {
                lost.add(item);
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

        NPC_PREDICATE = comp -> comp != null && comp.isInteractible() && !comp.isFollower() && comp.getCombatLevel() > 0;

        PK_COMPARATOR = localPlayer -> Comparator
            .comparing(Player::isClanMember) // prefer not in clan
            .thenComparing(Player::isFriend) // prefer not friend
            .thenComparing(Player::isFriendsChatMember) // prefer not fc
            .thenComparingInt(p -> Math.abs(localPlayer.getCombatLevel() - p.getCombatLevel())) // prefer similar level
            .thenComparing(p -> p.getOverheadIcon() == null) // prefer praying
            .thenComparingInt(p -> -p.getCombatLevel()); // prefer higher level for a given absolute level gap

        NPC_COMPARATOR = npcManager -> Comparator.comparing(
            NPC::getTransformedComposition,
            Comparator.nullsFirst(
                Comparator
                    .comparing(
                        (NPCComposition comp) -> comp.getStringValue(ParamID.NPC_HP_NAME),
                        Comparator.comparing(StringUtils::isNotEmpty) // prefer has name in hit points UI
                    )
                    .thenComparing(
                        NPCComposition::getActions,
                        Comparator.comparing(DeathNotifier::hasAttackOption)
                    )
                    .thenComparingInt(NPCComposition::getCombatLevel) // prefer high level
                    .thenComparingInt(NPCComposition::getSize) // prefer large
                    .thenComparing(NPCComposition::isMinimapVisible) // prefer visible on minimap
                    .thenComparing(
                        Comparator.nullsFirst(
                            Comparator.comparing(comp -> npcManager.getHealth(comp.getId())) // prefer high max health
                        )
                    )
            )
        );
    }
}
