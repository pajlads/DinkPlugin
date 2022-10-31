package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeathNotifier extends BaseNotifier {

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
    }

    private void handleNotify() {
        Player localPlayer = plugin.getClient().getLocalPlayer();
        Actor pker = null;
        for (Player other : plugin.getClient().getPlayers()) {
            if (other.getInteracting() == localPlayer) {
                pker = other;
                break;
            }
        }
        ItemContainer inventory = plugin.getClient().getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = plugin.getClient().getItemContainer(InventoryID.EQUIPMENT);

        assert inventory != null;
        assert equipment != null;
        List<Pair<Item, Integer>> itemsByPrice = Stream.concat(Arrays.stream(inventory.getItems()), Arrays.stream(equipment.getItems()))
            .map(item -> Pair.of(item, plugin.getItemManager().getItemPrice(item.getId()) * item.getQuantity()))
            .sorted((item1, item2) -> item2.getRight() - item1.getRight())
            .collect(Collectors.toList());

        int keepCount = 3;
        if (localPlayer.getSkullIcon() != null) {
            keepCount = 0;
        }
        if (plugin.getClient().isPrayerActive(Prayer.PROTECT_ITEM)) {
            keepCount += 1;
        }
        Integer losePrice = itemsByPrice.stream().skip(keepCount).map(Pair::getRight).reduce(Integer::sum).orElse(0);

        String template = plugin.getConfig().deathNotifyMessage();
        if (pker != null && plugin.getConfig().deathNotifPvpEnabled()) {
            template = plugin.getConfig().deathNotifPvpMessage();
        }
        String notifyMessage = template
            .replace("%USERNAME%", Utils.getPlayerName(plugin.getClient()))
            .replace("%VALUELOST%", losePrice.toString());
        if (pker != null && plugin.getConfig().deathNotifPvpEnabled()) {
            // player name hopefully isn't null
            notifyMessage = notifyMessage
                .replace("%PKER%", Objects.requireNonNull(pker.getName()));
        }

        createMessage(DinkPluginConfig::deathSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new DeathNotificationData(losePrice, pker != null, pker != null ? pker.getName() : null))
            .type(NotificationType.DEATH)
            .build());
    }
}
