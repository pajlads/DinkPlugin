package dinkplugin;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class LootNotifier extends BaseNotifier {
    private boolean sendMessage = false;

    @Inject
    public LootNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(Collection<ItemStack> items, String dropper) {
        if (plugin.isIgnoredWorld()) return;
        NotificationBody<LootNotificationData> messageBody = new NotificationBody<>();
        StringBuilder lootMessage = new StringBuilder();
        int minValue = plugin.config.minLootValue();
        long totalStackValue = 0;
        List<SerializedItemStack> serializedItems = new ArrayList<>();
        for (ItemStack item : Utils.reduceItemStack(items)) {
            int itemId = item.getId();
            int quantity = item.getQuantity();
            int price = plugin.itemManager.getItemPrice(itemId);
            long totalPrice = (long) price * quantity;

            ItemComposition itemComposition = plugin.itemManager.getItemComposition(itemId);
            if (totalPrice >= minValue) {
                if (totalStackValue != 0) {
                    lootMessage.append("\n");
                }
                sendMessage = true;
                lootMessage.append(String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice)));
                if (plugin.config.lootIcons()) {
                    messageBody.getEmbeds().add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
                }
            }
            serializedItems.add(new SerializedItemStack(item.getId(), item.getQuantity(), price, itemComposition.getName()));

            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            sendMessage = false;
            String lootString = lootMessage.toString();
            String notifyMessage = plugin.config.lootNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%LOOT%", lootString)
                .replaceAll("%SOURCE%", dropper);
            messageBody.setContent(notifyMessage);
            LootNotificationData extra = new LootNotificationData();
            extra.setItems(serializedItems);
            extra.setSource(dropper);
            messageBody.setExtra(extra);
            messageBody.setType(NotificationType.LOOT);
            plugin.messageHandler.createMessage(plugin.config.lootSendImage(), messageBody);
        }
    }
}
