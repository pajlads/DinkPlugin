package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
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
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LootNotifier extends BaseNotifier {

    @Inject
    public LootNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyLoot() && super.isEnabled();
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (isEnabled()) {
            this.handleNotify(event.getItems(), event.getNpc().getName());
        }
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (super.isEnabled()) {
            this.handleNotify(event.getItems(), event.getPlayer().getName());
        }
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            this.handleNotify(lootReceived.getItems(), lootReceived.getName());
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper) {
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
            String notifyMessage = StringUtils.replaceEach(
                plugin.getConfig().lootNotifyMessage(),
                new String[] { "%USERNAME%", "%LOOT%", "%TOTAL_VALUE%", "%SOURCE%" },
                new String[] { Utils.getPlayerName(plugin.getClient()), lootMessage.toString(), QuantityFormatter.quantityToStackSize(totalStackValue), dropper }
            );
            messageBody.setContent(notifyMessage);
            messageBody.setExtra(new LootNotificationData(serializedItems, dropper));
            messageBody.setType(NotificationType.LOOT);
            createMessage(DinkPluginConfig::lootSendImage, messageBody);
        }
    }
}
