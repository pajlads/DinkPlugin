package universalDiscord;

import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.QuantityFormatter;

import javax.inject.Inject;
import java.util.Collection;


public class LootNotifier extends BaseNotifier {
    private boolean sendMessage = false;

    @Inject
    public LootNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(Collection<ItemStack> items, String dropper) {
        DiscordMessageBody messageBody = new DiscordMessageBody();
        StringBuilder lootMessage = new StringBuilder();
        int minValue = plugin.config.minLootValue();
        long totalStackValue = 0;

        for (ItemStack item : Utils.reduceItemStack(items)) {
            int itemId = item.getId();
            int quantity = item.getQuantity();
            int price = plugin.itemManager.getItemPrice(itemId);
            long totalPrice = (long) price * quantity;

            if (totalPrice >= minValue) {
                if(totalStackValue != 0) {
                    lootMessage.append("\n");
                }
                sendMessage = true;
                ItemComposition itemComposition = plugin.itemManager.getItemComposition(itemId);
                lootMessage.append(String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice)));
                if(plugin.config.lootIcons()) {
                    messageBody.getEmbeds().add(new DiscordMessageBody.Embed(new DiscordMessageBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
                }
            }

            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            sendMessage = false;
            String lootString = lootMessage.toString();
            String notifyMessage = plugin.config.lootNotifyMessage()
                    .replaceAll("%USERNAME%", Utils.getPlayerName())
                    .replaceAll("%LOOT%", lootString)
                    .replaceAll("%SOURCE%", dropper);
            plugin.messageHandler.createMessage(notifyMessage, plugin.config.lootSendImage(), messageBody);
        }
    }
}
