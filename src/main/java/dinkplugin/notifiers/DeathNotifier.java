package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import net.runelite.api.Actor;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        if (event.getSource() == plugin.getClient().getLocalPlayer()) {
            lastTarget = new WeakReference<>(event.getTarget());
        }
    }

    private void handleNotify() {
        Player localPlayer = plugin.getClient().getLocalPlayer();
        Actor pker = identifyPker();

        List<Pair<Item, Long>> itemsByPrice = getPricedItems();

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

        List<NotificationBody.Embed> lostItemEmbeds = itemsByPrice.stream()
            .skip(keepCount)
            .limit(3)
            .map(Pair::getLeft)
            .mapToInt(Item::getId)
            .mapToObj(Utils::getItemImageUrl)
            .map(NotificationBody.UrlEmbed::new)
            .map(NotificationBody.Embed::new)
            .collect(Collectors.toList());

        createMessage(DinkPluginConfig::deathSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new DeathNotificationData(losePrice, pker != null, pker != null ? pker.getName() : null))
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

    private List<Pair<Item, Long>> getPricedItems() {
        ItemContainer inventory = plugin.getClient().getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = plugin.getClient().getItemContainer(InventoryID.EQUIPMENT);
        if (inventory == null || equipment == null) return Collections.emptyList();
        return Stream.concat(Arrays.stream(inventory.getItems()), Arrays.stream(equipment.getItems()))
            .map(item -> Pair.of(item, (long) (plugin.getItemManager().getItemPrice(item.getId())) * (long) (item.getQuantity())))
            .sorted((item1, item2) -> Math.toIntExact(item2.getRight() - item1.getRight()))
            .collect(Collectors.toList());
    }

}
