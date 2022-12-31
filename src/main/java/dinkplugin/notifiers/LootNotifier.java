package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
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
    private ItemManager itemManager;

    @Override
    public boolean isEnabled() {
        return config.notifyLoot() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.lootWebhook();
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (isEnabled())
            this.handleNotify(event.getItems(), event.getNpc().getName());
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (Utils.isCastleWars(client) || Utils.isLastManStanding(client) || Utils.isSoulWars(client))
            return;

        if (config.includePlayerLoot() && isEnabled())
            this.handleNotify(event.getItems(), event.getPlayer().getName());
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            this.handleNotify(lootReceived.getItems(), lootReceived.getName());
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper) {
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<NotificationBody.Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        StringBuilder lootMessage = new StringBuilder();
        long totalStackValue = 0;
        boolean sendMessage = false;

        for (ItemStack item : reduced) {
            int itemId = item.getId();
            int quantity = item.getQuantity();
            int price = itemManager.getItemPrice(itemId);
            long totalPrice = (long) price * quantity;
            ItemComposition itemComposition = itemManager.getItemComposition(itemId);
            if (totalPrice >= minValue) {
                sendMessage = true;
                if (lootMessage.length() > 0) lootMessage.append("\n");
                lootMessage.append(String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice)));
                if (icons) embeds.add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(ItemUtils.getItemImageUrl(itemId))));
            }
            serializedItems.add(new SerializedItemStack(itemId, quantity, price, itemComposition.getName()));
            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            boolean screenshot = config.lootSendImage() && totalStackValue >= config.lootImageMinValue();
            String notifyMessage = StringUtils.replaceEach(
                config.lootNotifyMessage(),
                new String[] { "%USERNAME%", "%LOOT%", "%TOTAL_VALUE%", "%SOURCE%" },
                new String[] { Utils.getPlayerName(client), lootMessage.toString(), QuantityFormatter.quantityToStackSize(totalStackValue), dropper }
            );
            createMessage(screenshot,
                NotificationBody.builder()
                    .content(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper))
                    .screenshotFile("lootImage.png")
                    .type(NotificationType.LOOT)
                    .build()
            );
        }
    }
}
