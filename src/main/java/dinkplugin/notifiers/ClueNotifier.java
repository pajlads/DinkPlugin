package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.ClueNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.QuantityFormatter;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ClueNotifier extends BaseNotifier {
    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails\\.");
    private String clueCount = "";
    private String clueType = "";
    private int badTicks = 0; // used to prevent notifs from using stale data

    @Inject
    private ItemManager itemManager;

    @Override
    public boolean isEnabled() {
        return config.notifyClue() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.clueWebhook();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled()) {
            Matcher clueMatcher = CLUE_SCROLL_REGEX.matcher(chatMessage);
            if (clueMatcher.find()) {
                // game message always occurs before widget load; save this data
                this.clueCount = clueMatcher.group("scrollCount");
                this.clueType = clueMatcher.group("scrollType");
            }
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == WidgetID.CLUE_SCROLL_REWARD_GROUP_ID && isEnabled()) {
            Widget clue = client.getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER);
            if (clue != null && !clueType.isEmpty()) {
                Widget[] children = clue.getChildren();
                if (children == null) return;

                Map<Integer, Integer> clueItems = new HashMap<>();
                for (Widget child : children) {
                    if (child == null) continue;

                    int quantity = child.getItemQuantity();
                    int itemId = child.getItemId();
                    if (itemId > -1 && quantity > 0) {
                        clueItems.merge(itemId, quantity, Integer::sum);
                    }
                }

                this.handleNotify(clueItems);
            }
        }
    }

    public void onTick() {
        // Track how many ticks occur where we only have partial clue data
        if (!clueType.isEmpty())
            badTicks++;

        // Clear data if 2 ticks pass with only partial parsing (both events should occur within same tick)
        if (badTicks > 1)
            reset();
    }

    private void handleNotify(Map<Integer, Integer> clueItems) {
        StringBuilder lootMessage = new StringBuilder();
        AtomicLong totalPrice = new AtomicLong();
        List<SerializedItemStack> itemStacks = new ArrayList<>(clueItems.size());
        List<NotificationBody.Embed> embeds = new ArrayList<>(config.clueShowItems() ? clueItems.size() : 0);

        clueItems.forEach((itemId, quantity) -> {
            if (lootMessage.length() > 0) lootMessage.append('\n');

            int price = itemManager.getItemPrice(itemId);
            ItemComposition itemComposition = itemManager.getItemComposition(itemId);
            SerializedItemStack stack = new SerializedItemStack(itemId, quantity, price, itemComposition.getName());

            totalPrice.addAndGet(stack.getTotalPrice());
            itemStacks.add(stack);
            lootMessage.append(getItemMessage(stack, embeds));
        });

        if (totalPrice.get() >= config.clueMinValue()) {
            boolean screenshot = config.clueSendImage() && totalPrice.get() >= config.clueImageMinValue();
            String notifyMessage = StringUtils.replaceEach(
                config.clueNotifyMessage(),
                new String[] { "%USERNAME%", "%CLUE%", "%COUNT%", "%TOTAL_VALUE%", "%LOOT%" },
                new String[] { Utils.getPlayerName(client), clueType, clueCount, QuantityFormatter.quantityToStackSize(totalPrice.get()), lootMessage.toString() }
            );
            createMessage(screenshot,
                NotificationBody.builder()
                    .content(notifyMessage)
                    .extra(new ClueNotificationData(clueType, Integer.parseInt(clueCount), itemStacks))
                    .screenshotFile("clueImage.png")
                    .type(NotificationType.CLUE)
                    .embeds(embeds)
                    .build()
            );
        }

        this.reset();
    }

    private String getItemMessage(SerializedItemStack item, Collection<NotificationBody.Embed> embeds) {
        if (config.clueShowItems())
            embeds.add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(ItemUtils.getItemImageUrl(item.getId()))));
        return String.format("%s x %s (%s)", item.getQuantity(), item.getName(), QuantityFormatter.quantityToStackSize(item.getTotalPrice()));
    }

    public void reset() {
        this.clueCount = "";
        this.clueType = "";
        this.badTicks = 0;
    }
}
