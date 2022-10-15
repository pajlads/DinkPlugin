package dinkplugin;

import lombok.Getter;
import net.runelite.api.ItemComposition;
import net.runelite.client.util.QuantityFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClueNotifier extends BaseNotifier {
    @Getter
    private final Map<Integer, Integer> clueItems = new HashMap<>();

    private NotificationBody<ClueNotificationData> messageBody;

    public ClueNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String numberCompleted, String clueType) {
        messageBody = new NotificationBody<>();
        StringBuilder lootMessage = new StringBuilder();
        long totalPrice = 0;
        List<SerializedItemStack> itemStacks = new ArrayList<>();

        for (Integer itemId : clueItems.keySet()) {
            if (lootMessage.length() > 0) {
                lootMessage.append("\n");
            }
            int quantity = clueItems.get(itemId);
            int price = plugin.itemManager.getItemPrice(itemId);
            totalPrice += (long) price * quantity;
            lootMessage.append(getItem(itemId, clueItems.get(itemId)));

            ItemComposition itemComposition = plugin.itemManager.getItemComposition(itemId);
            itemStacks.add(new SerializedItemStack(itemId, quantity, price, itemComposition.getName()));
        }

        if (totalPrice < plugin.config.clueMinValue()) {
            return;
        }

        String notifyMessage = plugin.config.clueNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%CLUE%", clueType)
            .replaceAll("%COUNT%", numberCompleted)
            .replaceAll("%LOOT%", lootMessage.toString());
        messageBody.setContent(notifyMessage);
        ClueNotificationData extra = new ClueNotificationData();
        extra.setClueType(clueType);
        extra.setNumberCompleted(Integer.parseInt(numberCompleted));
        extra.setItems(itemStacks);
        messageBody.setExtra(extra);
        messageBody.setType(NotificationType.CLUE);
        plugin.messageHandler.createMessage(plugin.config.clueSendImage(), messageBody);
    }

    public String getItem(int itemId, int quantity) {
        int price = plugin.itemManager.getItemPrice(itemId);
        long totalPrice = (long) price * quantity;
        ItemComposition itemComposition = plugin.itemManager.getItemComposition(itemId);

        if (plugin.config.clueShowItems()) {
            messageBody.getEmbeds().add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
        }
        return String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice));
    }
}
