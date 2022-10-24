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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClueNotifier extends BaseNotifier {
    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails\\.");

    private final Map<Integer, Integer> clueItems = new HashMap<>();
    private boolean clueCompleted = false;
    private String clueCount = "";
    private String clueType = "";

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
                String numberCompleted = clueMatcher.group("scrollCount");
                String scrollType = clueMatcher.group("scrollType");

                if (clueCompleted) {
                    this.handleNotify(numberCompleted, scrollType);
                    clueCompleted = false;
                } else {
                    clueType = scrollType;
                    clueCount = numberCompleted;
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

                if (children == null) {
                    return;
                }

                for (Widget child : children) {
                    if (child == null) {
                        continue;
                    }

                    int quantity = child.getItemQuantity();
                    int itemId = child.getItemId();

                    if (itemId > -1 && quantity > 0) {
                        clueItems.put(itemId, quantity);
                    }
                }

                if (clueCompleted) {
                    this.handleNotify(clueCount, clueType);
                    clueCompleted = false;
                } else {
                    clueCompleted = true;
                }
            }
        }
    }

    private void handleNotify(String numberCompleted, String clueType) {
        NotificationBody<ClueNotificationData> messageBody = new NotificationBody<>();

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
            lootMessage.append(getItem(itemId, clueItems.get(itemId), messageBody));

            ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);
            itemStacks.add(new SerializedItemStack(itemId, quantity, price, itemComposition.getName()));
        }

        if (totalPrice < plugin.getConfig().clueMinValue()) {
            return;
        }

        String notifyMessage = StringUtils.replaceEach(
            plugin.getConfig().clueNotifyMessage(),
            new String[] { "%USERNAME%", "%CLUE%", "%COUNT%", "%TOTAL_VALUE%", "%LOOT%" },
            new String[] { Utils.getPlayerName(plugin.getClient()), clueType, numberCompleted, QuantityFormatter.quantityToStackSize(totalPrice), lootMessage.toString() }
        );
        messageBody.setContent(notifyMessage);
        messageBody.setExtra(new ClueNotificationData(clueType, Integer.parseInt(numberCompleted), itemStacks));
        messageBody.setType(NotificationType.CLUE);
        createMessage(DinkPluginConfig::clueSendImage, messageBody);
    }

    private String getItem(int itemId, int quantity, NotificationBody<ClueNotificationData> messageBody) {
        int price = plugin.getItemManager().getItemPrice(itemId);
        long totalPrice = (long) price * quantity;
        ItemComposition itemComposition = plugin.getItemManager().getItemComposition(itemId);

        if (plugin.getConfig().clueShowItems()) {
            messageBody.getEmbeds().add(new NotificationBody.Embed(new NotificationBody.UrlEmbed(Utils.getItemImageUrl(itemId))));
        }
        return String.format("%s x %s (%s)", quantity, itemComposition.getName(), QuantityFormatter.quantityToStackSize(totalPrice));
    }
}
