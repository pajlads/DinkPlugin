package dinkplugin.notifiers;

import dinkplugin.util.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.Actor;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return config.notifyDeath() && super.isEnabled() && !Utils.isSafeArea(client);
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
        Actor pker = identifyPker();

        Collection<Item> items = Utils.getItems(client);
        List<Pair<Item, Long>> itemsByPrice = getPricedItems(itemManager, items);

        int keepCount = getKeepCount();
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(itemsByPrice, keepCount);
        List<Pair<Item, Long>> keptItems = split.getLeft();
        List<Pair<Item, Long>> lostItems = split.getRight();

        long losePrice = lostItems.stream()
            .map(Pair::getRight)
            .reduce(Long::sum)
            .orElse(0L);

        String notifyMessage = buildMessage(pker, losePrice);

        int[] topLostItemIds = lostItems.stream()
            .map(Pair::getLeft)
            .mapToInt(Item::getId)
            .distinct()
            .limit(3)
            .toArray();

        List<SerializedItemStack> topLostStacks = getTopLostStacks(itemManager, lostItems, topLostItemIds);
        List<SerializedItemStack> keptStacks = keptItems.stream()
            .map(Pair::getKey)
            .map(item -> Utils.stackFromItem(itemManager, item))
            .collect(Collectors.toList());
        List<NotificationBody.Embed> keptItemEmbeds;
        if (config.deathEmbedKeptItems()) {
            keptItemEmbeds = Utils.buildEmbeds(
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
            topLostStacks
        );

        createMessage(config.deathSendImage(), NotificationBody.builder()
            .content(notifyMessage)
            .extra(extra)
            .embeds(keptItemEmbeds)
            .screenshotFile("deathImage.png")
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
        if (Utils.isPvpSafeZone(client))
            return null;

        // must be in wildness or pvp world to be pk'd
        if (client.getVarbitValue(Varbits.IN_WILDERNESS) <= 0 && !Utils.isPvpWorld(client.getWorldType()))
            return null;

        Player localPlayer = client.getLocalPlayer();

        Actor lastTarget = this.lastTarget.get();
        if (lastTarget != null && !lastTarget.isDead() && lastTarget.getInteracting() == localPlayer) {
            if (lastTarget instanceof Player)
                return (Player) lastTarget;
            else
                return null; // we likely died to this NPC rather than a player
        }

        for (Player other : client.getPlayers()) {
            if (other.getInteracting() == localPlayer) {
                return other;
            }
        }
        return null;
    }

    /**
     * @param itemManager {@link ItemManager}
     * @param items       the items whose prices should be queried
     * @return pairs of the passed items to their price, sorted by most expensive first
     */
    @NotNull
    private static List<Pair<Item, Long>> getPricedItems(ItemManager itemManager, Collection<Item> items) {
        return items.stream()
            .map(item -> Pair.of(item, Utils.getPrice(itemManager, item.getId()) * item.getQuantity()))
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

            if (kept < keepCount && !Utils.isItemNeverKeptOnDeath(id)) {
                keep.add(item);
                kept++;
            } else {
                lost.add(item);
            }
        }

        return Pair.of(keep, lost);
    }

    /**
     * Converts the top lost item id array to the associated item stacks,
     * while reflecting the cumulative item quantity across inventory slots.
     *
     * @param itemManager    {@link ItemManager}
     * @param lostItems      the items that would be lost on death
     * @param topLostItemIds a distinct set of the most valuable item id's that are being lost
     * @return the reduced {@link SerializedItemStack}'s associated with topLostItemIds
     */
    @NotNull
    private static List<SerializedItemStack> getTopLostStacks(ItemManager itemManager, List<Pair<Item, Long>> lostItems, int[] topLostItemIds) {
        Map<Integer, Item> reducedLostItems = Utils.reduceItems(lostItems.stream().map(Pair::getLeft).collect(Collectors.toList()));
        return Arrays.stream(topLostItemIds)
            .mapToObj(reducedLostItems::get)
            .filter(Objects::nonNull)
            .map(item -> Utils.stackFromItem(itemManager, item))
            .collect(Collectors.toList());
    }

}
