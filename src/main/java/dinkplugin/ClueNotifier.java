package universalDiscord;

import net.runelite.api.ItemComposition;
import net.runelite.client.util.QuantityFormatter;

import java.util.HashMap;

public class ClueNotifier extends BaseNotifier{
    public HashMap<Integer, Integer> clueItems = new HashMap<Integer, Integer>();

    private DiscordMessageBody messageBody;

    public ClueNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String numberCompleted, String clueType) {
        messageBody = new DiscordMessageBody();
        StringBuilder lootMessage = new StringBuilder();
        long totalPrice = 0;

        for(Integer itemId : clueItems.keySet()) {
            if(lootMessage.length() > 0) {
                lootMessage.append("\n");
            }
            int quantity = clueItems.get(itemId);
            int price = plugin.itemManager.getItemPrice(itemId);
            totalPrice += (long) price * quantity;
            lootMessage.append(getItem(itemId, clueItems.get(itemId)));
        }

        if(totalPrice < plugin.config.clueMinValue()) {
            return;
        }

        String notifyMessage = plugin.config.clueNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%CLUE%", clueType)
                .replaceAll("%COUNT%", numberCompleted)
                .replaceAll("%LOOT%", lootMessage.toString());
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.clueSendImage(), null);
    }

    public String getItem(int itemId, int quantity) {
        int price = plugin.itemManager.getItemPrice(itemId);
        long totalPrice = (long) price * quantity;
        ItemComposition itemComposition = plugin.itemManager.getItemComposition(itemId);

        if(plugin.config.clueShowItems()) {
            messageBody.getEmbeds().add(new DiscordMessageBody.Embed(new DiscordMessageBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
        }
        return String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice));
    }
}
