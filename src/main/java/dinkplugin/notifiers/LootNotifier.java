package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
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
import java.util.Collections;
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

    public void onWidgetLoaded(WidgetLoaded event) {
        // special case: runelite client & loot tracker do not handle unsired loot at the time of writing
        if (event.getGroupId() == WidgetID.DIALOG_SPRITE_GROUP_ID && isEnabled()) {
            Widget spriteWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE);
            if (spriteWidget == null || spriteWidget.getItemId() < 0) {
                spriteWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_SPRITE);
            }

            if (spriteWidget != null && spriteWidget.getItemId() >= 0) {
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
                if (textWidget != null && StringUtils.containsIgnoreCase(textWidget.getText(), "The Font consumes the Unsired")) {
                    ItemStack item = new ItemStack(
                        spriteWidget.getId(),
                        Math.max(spriteWidget.getItemQuantity(), 1),
                        client.getLocalPlayer().getLocalLocation()
                    );
                    this.handleNotify(Collections.singletonList(item), "The Font of Consumption");
                }
            }
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper) {
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = Utils.reduceItemStack(items);
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
                if (icons) embeds.add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
            }
            serializedItems.add(new SerializedItemStack(itemId, quantity, price, itemComposition.getName()));
            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            String notifyMessage = StringUtils.replaceEach(
                config.lootNotifyMessage(),
                new String[] { "%USERNAME%", "%LOOT%", "%TOTAL_VALUE%", "%SOURCE%" },
                new String[] { Utils.getPlayerName(client), lootMessage.toString(), QuantityFormatter.quantityToStackSize(totalStackValue), dropper }
            );
            createMessage(DinkPluginConfig::lootSendImage, NotificationBody.builder()
                .content(notifyMessage)
                .embeds(embeds)
                .extra(new LootNotificationData(serializedItems, dropper))
                .screenshotFile("lootImage.png")
                .type(NotificationType.LOOT)
                .build());
        }
    }
}
