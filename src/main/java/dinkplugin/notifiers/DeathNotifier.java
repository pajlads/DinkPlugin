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
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.vars.AccountType;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DeathNotifier extends BaseNotifier {

    @Inject
    private ItemManager itemManager;

    /**
     * Tracks the last {@link Actor} our local player interacted with,
     * for the purposes of attributing deaths to particular {@link Player}'s.
     * <p>
     * Note: this is wrapped in a weak reference to allow garbage collection,
     * for example if the {@link Actor} despawns.
     * As a result, the underlying reference can be null.
     *
     * @see #identifyPker()
     */
    private WeakReference<Actor> lastTarget = new WeakReference<>(null);

    @Override
    public boolean isEnabled() {
        return config.notifyDeath() && super.isEnabled() && isDangerous(client);
    }

    @Override
    protected String getWebhookUrl() {
        return config.deathWebhook();
    }

    public void onActorDeath(ActorDeath actor) {
        boolean self = client.getLocalPlayer() == actor.getActor();

        if (self && isEnabled())
            handleNotify();

        if (self || actor.getActor() == lastTarget.get())
            lastTarget = new WeakReference<>(null);
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() == client.getLocalPlayer() && event.getTarget() != null && event.getTarget().getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(event.getTarget());
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

        Actor pker = identifyPker();
        String notifyMessage = buildMessage(pker, losePrice);

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
            pker != null,
            pker != null ? pker.getName() : null,
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

    private String buildMessage(Actor pker, long losePrice) {
        boolean pvp = pker != null && config.deathNotifPvpEnabled();
        String template;
        if (pvp)
            template = config.deathNotifPvpMessage();
        else
            template = config.deathNotifyMessage();
        String notifyMessage = template
            .replace("%USERNAME%", Utils.getPlayerName(client))
            .replace("%VALUELOST%", String.valueOf(losePrice));
        if (pvp) {
            notifyMessage = notifyMessage.replace("%PKER%", String.valueOf(pker.getName()));
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
     * @return the inferred {@link Player} who killed us, or null if not pk'd
     */
    @Nullable
    private Player identifyPker() {
        // cannot be pk'd in safe zone
        if (WorldUtils.isPvpSafeZone(client))
            return null;

        // must be in wildness or pvp world or LMS to be pk'd
        if (client.getVarbitValue(Varbits.IN_WILDERNESS) <= 0 && !WorldUtils.isPvpWorld(client.getWorldType()) && !WorldUtils.isLastManStanding(client))
            return null;

        Player localPlayer = client.getLocalPlayer();

        Actor lastTarget = this.lastTarget.get();
        if (lastTarget != null && !lastTarget.isDead() && lastTarget.getInteracting() == localPlayer) {
            if (lastTarget instanceof Player) {
                Player last = (Player) lastTarget;
                if (!last.isClanMember() && !last.isFriend() && !last.isFriendsChatMember())
                    return last;
            } else if (lastTarget.getCombatLevel() > 0) {
                return null; // we likely died to this NPC rather than a player
            }
        }

        // find another player interacting with us (that is preferably not a friend or clan member)
        return client.getPlayers().stream()
            .filter(other -> other.getInteracting() == localPlayer)
            .min(
                Comparator.comparing(Player::isDead) // prefer alive
                    .thenComparing(Player::isClanMember) // prefer not in clan
                    .thenComparing(Player::isFriend) // prefer not friend
                    .thenComparing(Player::isFriendsChatMember) // prefer not fc
                    .thenComparingInt(p -> Math.abs(localPlayer.getCombatLevel() - p.getCombatLevel())) // prefer similar level
                    .thenComparing(p -> p.getOverheadIcon() == null) // prefer skulled
                    .thenComparingInt(p -> -p.getCombatLevel()) // prefer higher level for a given absolute level gap
            )
            .orElse(null);
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

    /**
     * @param client {@link Client}
     * @return whether the player is not in a safe area (excluding inferno and fight cave)
     */
    private static boolean isDangerous(Client client) {
        if (!WorldUtils.isSafeArea(client))
            return true; // normally dangerous

        // inferno and fight cave are technically safe, but we want death notification regardless
        int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        return WorldUtils.isInferno(regionId) || WorldUtils.isTzHaarFightCave(regionId);
    }

}
