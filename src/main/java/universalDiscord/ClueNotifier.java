package universalDiscord;

import net.runelite.api.ItemComposition;
import net.runelite.client.util.QuantityFormatter;

import javax.annotation.Nullable;
import java.util.HashMap;

public class ClueNotifier extends BaseNotifier{
    public HashMap<Integer, Integer> clueItems = new HashMap<Integer, Integer>();

    private DiscordMessageBody messageBody;

    public ClueNotifier(UniversalDiscordPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String numberCompleted, String clueType) {
        messageBody = new DiscordMessageBody();
        StringBuilder lootMessage = new StringBuilder();

        for(Integer itemId : clueItems.keySet()) {
            if(lootMessage.length() > 0) {
                lootMessage.append("\n");
            }
            lootMessage.append(getItem(itemId, clueItems.get(itemId)));
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

        if(totalPrice >= plugin.config.clueMinValue()) {
            if(plugin.config.clueShowItems()) {
                messageBody.getEmbeds().add(new DiscordMessageBody.Embed(new DiscordMessageBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
            }
            return String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice));
        } else {
            return "";
        }
    }
}
