package dinkplugin.notifiers;

import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
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

@Slf4j
public class LootNotifier extends BaseNotifier {

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientThread clientThread;

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
            this.handleNotify(event.getItems(), event.getNpc().getName(), LootRecordType.NPC);
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (WorldUtils.isSafeArea(client))
            return;

        if (config.includePlayerLoot() && isEnabled())
            this.handleNotify(event.getItems(), event.getPlayer().getName(), LootRecordType.PLAYER);
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            if (!config.lootIncludeClueScrolls() && StringUtils.startsWithIgnoreCase(lootReceived.getName(), "Clue Scroll")) {
                // skip clue scroll loot, depending on config
                return;
            }

            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (!isEnabled()) return;

        // special case: runelite client & loot tracker do not handle unsired loot at the time of writing
        if (event.getGroupId() == WidgetID.DIALOG_SPRITE_GROUP_ID) {
            clientThread.invokeAtTickEnd(() -> {
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
                if (textWidget != null && StringUtils.containsIgnoreCase(textWidget.getText(), "The Font consumes the Unsired")) {
                    Widget spriteWidget = firstWithItem(WidgetInfo.DIALOG_SPRITE, WidgetInfo.DIALOG_SPRITE_SPRITE, WidgetInfo.DIALOG_SPRITE_TEXT);
                    if (hasItem(spriteWidget)) {
                        ItemStack item = new ItemStack(
                            spriteWidget.getItemId(),
                            1,
                            client.getLocalPlayer().getLocalLocation()
                        );
                        this.handleNotify(Collections.singletonList(item), "The Font of Consumption", LootRecordType.EVENT);
                    } else {
                        Widget widget = client.getWidget(WidgetInfo.DIALOG_SPRITE);
                        log.warn(
                            "Failed to locate widget with item for Unsired loot. Children: {} - Nested: {} - Sprite: {} - Model: {}",
                            widget != null && widget.getDynamicChildren() != null ? widget.getDynamicChildren().length : -1,
                            widget != null && widget.getNestedChildren() != null ? widget.getNestedChildren().length : -1,
                            widget != null ? widget.getSpriteId() : -1,
                            widget != null ? widget.getModelId() : -1
                        );
                    }
                }
            });
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper, LootRecordType type) {
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        StringBuilder lootMessage = new StringBuilder();
        long totalStackValue = 0;
        boolean sendMessage = false;
        SerializedItemStack max = null;

        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();
            if (totalPrice >= minValue) {
                sendMessage = true;
                if (lootMessage.length() > 0) lootMessage.append("\n");
                lootMessage.append(ItemUtils.formatStack(stack, placeholder));
                if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
                if (max == null || totalPrice > max.getTotalPrice()) {
                    max = stack;
                }
            }
            serializedItems.add(stack);
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
                    .text(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper, type))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(max.getId()))
                    .build()
            );
        }
    }

    private Widget firstWithItem(WidgetInfo... widgets) {
        for (WidgetInfo info : widgets) {
            Widget widget = client.getWidget(info);
            if (hasItem(widget)) {
                log.debug("Obtained item from widget via {}", info);
                return widget;
            }
        }
        return null;
    }

    private static boolean hasItem(Widget widget) {
        return widget != null && widget.getItemId() >= 0;
    }

}
