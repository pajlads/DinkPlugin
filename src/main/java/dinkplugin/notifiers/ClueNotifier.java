package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.ClueNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
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
        if (plugin.isIgnoredWorld()) return;
        messageBody = new NotificationBody<>();

        StringBuilder lootMessage = new StringBuilder();
        long totalPrice = 0;
        List<SerializedItemStack> itemStacks = new ArrayList<>();

        for (Integer itemId : clueItems.keySet()) {
            if (lootMessage.length() > 0) {
                lootMessage.append("\n");
            }
            int quantity = clueItems.get(itemId);
            int price = plugin.getItemManager().getItemPrice(itemId);
            totalPrice += (long) price * quantity;
            lootMessage.append(getItem(itemId, clueItems.get(itemId)));

            ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);
            itemStacks.add(new SerializedItemStack(itemId, quantity, price, itemComposition.getName()));
        }

        if (totalPrice < config.clueMinValue()) {
            return;
        }

        String notifyMessage = config.clueNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%CLUE%", clueType)
            .replaceAll("%COUNT%", numberCompleted)
            .replaceAll("%TOTAL_VALUE%", QuantityFormatter.quantityToStackSize(totalPrice))
            .replaceAll("%LOOT%", lootMessage.toString());
        messageBody.setContent(notifyMessage);
        ClueNotificationData extra = new ClueNotificationData();
        extra.setClueType(clueType);
        extra.setNumberCompleted(Integer.parseInt(numberCompleted));
        extra.setItems(itemStacks);
        messageBody.setExtra(extra);
        messageBody.setType(NotificationType.CLUE);
        messageHandler.createMessage(config.clueSendImage(), messageBody);
    }

    public String getItem(int itemId, int quantity) {
        int price = plugin.getItemManager().getItemPrice(itemId);
        long totalPrice = (long) price * quantity;
        ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);

        if (config.clueShowItems()) {
            messageBody.getEmbeds().add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
        }
        return String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice));
    }
}
