package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        return plugin.getConfig().notifyDeath() && super.isEnabled();
    }

    public void onActorDeath(ActorDeath actor) {
        if (isEnabled() && plugin.getClient().getLocalPlayer() == actor.getActor()) {
            this.handleNotify();
        }
        lastTarget = new WeakReference<>(null);
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() == plugin.getClient().getLocalPlayer() && event.getTarget() != null && event.getTarget().getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(event.getTarget());
        }
    }

    private void handleNotify() {
        Player localPlayer = plugin.getClient().getLocalPlayer();
        Actor pker = identifyPker();

        Collection<Item> items = getItems(plugin.getClient());
        List<Pair<Item, Long>> itemsByPrice = getPricedItems(plugin.getItemManager(), items);

        int keepCount;
        if (localPlayer.getSkullIcon() == null)
            keepCount = 3;
        else
            keepCount = 0;
        if (plugin.getClient().isPrayerActive(Prayer.PROTECT_ITEM))
            keepCount++;

        long losePrice = itemsByPrice.stream()
            .skip(keepCount)
            .map(Pair::getRight)
            .reduce(Long::sum)
            .orElse(0L);

        boolean pvpNotif = pker != null && plugin.getConfig().deathNotifPvpEnabled();
        String template;
        if (pvpNotif)
            template = plugin.getConfig().deathNotifPvpMessage();
        else
            template = plugin.getConfig().deathNotifyMessage();
        String notifyMessage = template
            .replace("%USERNAME%", Utils.getPlayerName(plugin.getClient()))
            .replace("%VALUELOST%", String.valueOf(losePrice));
        if (pvpNotif) {
            notifyMessage = notifyMessage.replace("%PKER%", String.valueOf(pker.getName()));
        }

        int[] topLostItemIds = itemsByPrice.stream()
            .skip(keepCount)
            .map(Pair::getLeft)
            .mapToInt(Item::getId)
            .distinct()
            .limit(3)
            .toArray();

        Map<Integer, Item> reducedLostItems = reduceLostItems(itemsByPrice, keepCount);
        List<SerializedItemStack> topLostStacks = Arrays.stream(topLostItemIds)
            .mapToObj(reducedLostItems::get)
            .filter(Objects::nonNull)
            .map(item -> stackFromItem(plugin.getItemManager(), item))
            .collect(Collectors.toList());

        List<NotificationBody.Embed> lostItemEmbeds = Arrays.stream(topLostItemIds)
            .mapToObj(Utils::getItemImageUrl)
            .map(NotificationBody.UrlEmbed::new)
            .map(NotificationBody.Embed::new)
            .collect(Collectors.toList());

        List<SerializedItemStack> keptStacks = itemsByPrice.stream()
            .limit(keepCount)
            .map(Pair::getLeft)
            .map(item -> stackFromItem(plugin.getItemManager(), item))
            .collect(Collectors.toList());

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

    private Player identifyPker() {
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

    private static Collection<Item> getItems(Client client) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (inventory == null || equipment == null) return Collections.emptyList();
        return Utils.concat(inventory.getItems(), equipment.getItems());
    }

    private static List<Pair<Item, Long>> getPricedItems(ItemManager itemManager, Collection<Item> items) {
        return items.stream()
            .map(item -> Pair.of(item, (long) (itemManager.getItemPrice(item.getId())) * (long) (item.getQuantity())))
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

    private static SerializedItemStack stackFromItem(ItemManager itemManager, Item item) {
        int id = item.getId();
        int quantity = item.getQuantity();
        int price = itemManager.getItemPrice(id);
        ItemComposition composition = itemManager.getItemComposition(id);
        return new SerializedItemStack(id, quantity, price, composition.getName());
    }

}
