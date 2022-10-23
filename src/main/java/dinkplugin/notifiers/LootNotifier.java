package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LootNotifier extends BaseNotifier {

    @Inject
    public LootNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (plugin.getConfig().notifyLoot()) {
            this.handleNotify(event.getItems(), event.getNpc().getName());
        }
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        this.handleNotify(event.getItems(), event.getPlayer().getName());
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!plugin.getConfig().notifyLoot()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            this.handleNotify(lootReceived.getItems(), lootReceived.getName());
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper) {
        if (plugin.isIgnoredWorld()) return;

        boolean sendMessage = false;
        NotificationBody<LootNotificationData> messageBody = new NotificationBody<>();
        StringBuilder lootMessage = new StringBuilder();
        int minValue = plugin.getConfig().minLootValue();
        long totalStackValue = 0;
        List<SerializedItemStack> serializedItems = new ArrayList<>();
        for (ItemStack item : Utils.reduceItemStack(items)) {
            int itemId = item.getId();
            int quantity = item.getQuantity();
            int price = plugin.getItemManager().getItemPrice(itemId);
            long totalPrice = (long) price * quantity;

            ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);
            if (totalPrice >= minValue) {
                if (totalStackValue != 0) {
                    lootMessage.append("\n");
                }
                sendMessage = true;
                lootMessage.append(String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice)));
                if (plugin.getConfig().lootIcons()) {
                    messageBody.getEmbeds().add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
                }
            }
            serializedItems.add(new SerializedItemStack(item.getId(), item.getQuantity(), price, itemComposition.getName()));

            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            String lootString = lootMessage.toString();
            String notifyMessage = plugin.getConfig().lootNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%LOOT%", lootString)
                .replaceAll("%TOTAL_VALUE%", QuantityFormatter.quantityToStackSize(totalStackValue))
                .replaceAll("%SOURCE%", dropper);
            messageBody.setContent(notifyMessage);
            LootNotificationData extra = new LootNotificationData();
            extra.setItems(serializedItems);
            extra.setSource(dropper);
            messageBody.setExtra(extra);
            messageBody.setType(NotificationType.LOOT);
            messageHandler.createMessage(plugin.getConfig().lootSendImage(), messageBody);
        }
    }
}
