package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.ClueNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.QuantityFormatter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClueNotifier extends BaseNotifier {
    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails\\.");

    private final Map<Integer, Integer> clueItems = new HashMap<>();
    private boolean clueCompleted = false;
    private String clueCount = "";
    private String clueType = "";
    private int badTicks = 0; // used to prevent notifs from using stale data

    public ClueNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyClue() && super.isEnabled();
    }

    public void onChatMessage(String chatMessage) {
        if (isEnabled()) {
            Matcher clueMatcher = CLUE_SCROLL_REGEX.matcher(chatMessage);
            if (clueMatcher.find()) {
                clueCount = clueMatcher.group("scrollCount");
                clueType = clueMatcher.group("scrollType");

                if (clueCompleted) {
                    this.handleNotify();
                } else {
                    clueCompleted = true;
                }
            }
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == WidgetID.CLUE_SCROLL_REWARD_GROUP_ID && isEnabled()) {
            Widget clue = plugin.getClient().getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER);
            if (clue != null) {
                clueItems.clear();
                Widget[] children = clue.getChildren();
                if (children == null) return;

                for (Widget child : children) {
                    if (child == null) continue;

                    int quantity = child.getItemQuantity();
                    int itemId = child.getItemId();
                    if (itemId > -1 && quantity > 0) {
                        clueItems.merge(itemId, quantity, Integer::sum);
                    }
                }

                if (clueCompleted) {
                    this.handleNotify();
                } else {
                    clueCompleted = true;
                }
            }
        }
    }

    public void onTick() {
        // Track how many ticks occur where we only have partial clue data
        if (clueCount.isEmpty() != clueItems.isEmpty()) // XOR
            badTicks++;

        // Clear data if over 8 ticks pass with only partial parsing
        if (badTicks > 8)
            reset();
    }

    private void handleNotify() {
        StringBuilder lootMessage = new StringBuilder();
        AtomicLong totalPrice = new AtomicLong();
        List<SerializedItemStack> itemStacks = new ArrayList<>(clueItems.size());
        List<NotificationBody.Embed> embeds = new ArrayList<>(plugin.getConfig().clueShowItems() ? clueItems.size() : 0);

        clueItems.forEach((itemId, quantity) -> {
            if (lootMessage.length() > 0) lootMessage.append('\n');

            int price = plugin.getItemManager().getItemPrice(itemId);
            ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);
            SerializedItemStack stack = new SerializedItemStack(itemId, quantity, price, itemComposition.getName());

            totalPrice.addAndGet(stack.getTotalPrice());
            itemStacks.add(stack);
            lootMessage.append(getItemMessage(stack, embeds));
        });

        if (totalPrice.get() >= plugin.getConfig().clueMinValue()) {
            String notifyMessage = StringUtils.replaceEach(
                plugin.getConfig().clueNotifyMessage(),
                new String[] { "%USERNAME%", "%CLUE%", "%COUNT%", "%TOTAL_VALUE%", "%LOOT%" },
                new String[] { Utils.getPlayerName(plugin.getClient()), clueType, clueCount, QuantityFormatter.quantityToStackSize(totalPrice.get()), lootMessage.toString() }
            );
            createMessage(DinkPluginConfig::clueSendImage, NotificationBody.builder()
                .content(notifyMessage)
                .extra(new ClueNotificationData(clueType, Integer.parseInt(clueCount), itemStacks))
                .type(NotificationType.CLUE)
                .embeds(embeds)
                .build());
        }

        this.reset();
    }

    private String getItemMessage(SerializedItemStack item, Collection<NotificationBody.Embed> embeds) {
        if (plugin.getConfig().clueShowItems())
            embeds.add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(item.getId()))));
        return String.format("%s x %s (%s)", item.getQuantity(), item.getName(), QuantityFormatter.quantityToStackSize(item.getTotalPrice()));
    }

    private void reset() {
        this.clueCompleted = false;
        this.clueCount = "";
        this.clueType = "";
        this.clueItems.clear();
        this.badTicks = 0;
    }
}
