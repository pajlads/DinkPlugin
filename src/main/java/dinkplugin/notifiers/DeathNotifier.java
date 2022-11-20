package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
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

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeathNotifier extends BaseNotifier {

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

    @Inject
    public DeathNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyDeath() && super.isEnabled() && !Utils.isSafeArea(plugin.getClient());
    }

    public void onActorDeath(ActorDeath actor) {
        boolean self = plugin.getClient().getLocalPlayer() == actor.getActor();

        if (self && isEnabled())
            handleNotify();

        if (self || actor.getActor() == lastTarget.get())
            lastTarget = new WeakReference<>(null);
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() == plugin.getClient().getLocalPlayer() && event.getTarget() != null && event.getTarget().getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(event.getTarget());
        }
    }

    private void handleNotify() {
        Actor pker = identifyPker();

        Collection<Item> items = Utils.getItems(plugin.getClient());
        List<Pair<Item, Long>> itemsByPrice = getPricedItems(plugin.getItemManager(), items);

        int keepCount = getKeepCount();

        long losePrice = itemsByPrice.stream()
            .skip(keepCount)
            .filter(pair -> pair.getLeft().getId() != ItemID.OLD_SCHOOL_BOND && pair.getLeft().getId() != ItemID.OLD_SCHOOL_BOND_UNTRADEABLE)
            .map(Pair::getRight)
            .reduce(Long::sum)
            .orElse(0L);

        String notifyMessage = buildMessage(pker, losePrice);

        int[] topLostItemIds = itemsByPrice.stream()
            .skip(keepCount)
            .map(Pair::getLeft)
            .mapToInt(Item::getId)
            .filter(id -> id != ItemID.OLD_SCHOOL_BOND && id != ItemID.OLD_SCHOOL_BOND_UNTRADEABLE)
            .distinct()
            .limit(3)
            .toArray();

        List<NotificationBody.Embed> lostItemEmbeds = Utils.buildEmbeds(topLostItemIds);
        List<SerializedItemStack> topLostStacks = getTopLostStacks(plugin.getItemManager(), itemsByPrice, keepCount, topLostItemIds);
        List<SerializedItemStack> keptStacks = getKeptStacks(plugin.getItemManager(), itemsByPrice, keepCount);

        DeathNotificationData extra = new DeathNotificationData(
            losePrice,
            pker != null,
            pker != null ? pker.getName() : null,
            keptStacks,
            topLostStacks
        );

        createMessage(DinkPluginConfig::deathSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(extra)
            .embeds(lostItemEmbeds)
            .type(NotificationType.DEATH)
            .build());
    }

    private String buildMessage(Actor pker, long losePrice) {
        DinkPluginConfig config = plugin.getConfig();
        boolean pvp = pker != null && config.deathNotifPvpEnabled();
        String template;
        if (pvp)
            template = config.deathNotifPvpMessage();
        else
            template = config.deathNotifyMessage();
        String notifyMessage = template
            .replace("%USERNAME%", Utils.getPlayerName(plugin.getClient()))
            .replace("%VALUELOST%", String.valueOf(losePrice));
        if (pvp) {
            notifyMessage = notifyMessage.replace("%PKER%", String.valueOf(pker.getName()));
        }
        return notifyMessage;
    }

    private int getKeepCount() {
        if (plugin.getClient().getAccountType() == AccountType.ULTIMATE_IRONMAN)
            return 0;

        int keepCount;
        if (plugin.getClient().getLocalPlayer().getSkullIcon() == null)
            keepCount = 3;
        else
            keepCount = 0;
        if (plugin.getClient().isPrayerActive(Prayer.PROTECT_ITEM))
            keepCount++;
        return keepCount;
    }

    private Player identifyPker() {
        // cannot be pk'd in safe zone
        if (Utils.isPvpSafeZone(plugin.getClient()))
            return null;

        // must be in wildness or pvp world to be pk'd
        if (plugin.getClient().getVarbitValue(Varbits.IN_WILDERNESS) <= 0 && !Utils.isPvpWorld(plugin.getClient().getWorldType()))
            return null;

        Player localPlayer = plugin.getClient().getLocalPlayer();

        Actor lastTarget = this.lastTarget.get();
        if (lastTarget != null && !lastTarget.isDead() && lastTarget.getInteracting() == localPlayer) {
            if (lastTarget instanceof Player)
                return (Player) lastTarget;
            else
                return null; // we likely died to this NPC rather than a player
        }

        for (Player other : plugin.getClient().getPlayers()) {
            if (other.getInteracting() == localPlayer) {
                return other;
            }
        }
        return null;
    }

    private static List<Pair<Item, Long>> getPricedItems(ItemManager itemManager, Collection<Item> items) {
        return items.stream()
            .map(item -> Pair.of(item, Utils.getPrice(itemManager, item.getId()) * item.getQuantity()))
            .sorted((a, b) -> Math.toIntExact(b.getRight() - a.getRight()))
            .collect(Collectors.toList());
    }

    private static Map<Integer, Item> reduceLostItems(List<Pair<Item, Long>> itemsByPrice, int keepCount) {
        List<Item> lostItems = itemsByPrice.stream()
            .skip(keepCount)
            .map(Pair::getLeft)
            .collect(Collectors.toList());
        return Utils.reduceItems(lostItems);
    }

    private static List<SerializedItemStack> getTopLostStacks(ItemManager itemManager, List<Pair<Item, Long>> itemsByPrice, int keepCount, int[] topLostItemIds) {
        Map<Integer, Item> reducedLostItems = reduceLostItems(itemsByPrice, keepCount);
        return Arrays.stream(topLostItemIds)
            .mapToObj(reducedLostItems::get)
            .filter(Objects::nonNull)
            .map(item -> Utils.stackFromItem(itemManager, item))
            .collect(Collectors.toList());
    }

    private static List<SerializedItemStack> getKeptStacks(ItemManager itemManager, List<Pair<Item, Long>> itemsByPrice, int keepCount) {
        List<SerializedItemStack> kept = new LinkedList<>();

        itemsByPrice.stream()
            .map(Pair::getLeft)
            .filter(item -> !Utils.isItemNeverKeptOnDeath(item.getId()))
            .filter(item -> item.getId() != ItemID.OLD_SCHOOL_BOND && item.getId() != ItemID.OLD_SCHOOL_BOND_UNTRADEABLE)
            .limit(keepCount)
            .map(item -> Utils.stackFromItem(itemManager, item))
            .forEach(kept::add);

        itemsByPrice.stream()
            .map(Pair::getLeft)
            .filter(item -> item.getId() == ItemID.OLD_SCHOOL_BOND || item.getId() == ItemID.OLD_SCHOOL_BOND_UNTRADEABLE)
            .map(item -> Utils.stackFromItem(itemManager, item))
            .forEach(kept::add);

        return kept;
    }

}
