package dinkplugin.notifiers;

import dinkplugin.domain.ClueTier;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.ClueNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class ClueNotifier extends BaseNotifier {
    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails\\.");
    private final AtomicInteger badTicks = new AtomicInteger(); // used to prevent notifs from using stale data
    private volatile String clueCount = "";
    private volatile String clueType = "";

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
                String tier = clueMatcher.group("scrollType");
                if (checkClueTier(tier)) {
                    // game message always occurs before widget load; save this data
                    this.clueCount = clueMatcher.group("scrollCount");
                    this.clueType = tier;
                }
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
            badTicks.getAndIncrement();

        // Clear data if 2 ticks pass with only partial parsing (both events should occur within same tick)
        if (badTicks.get() > 1)
            reset();
    }

    private void handleNotify(Map<Integer, Integer> clueItems) {
        StringBuilder lootMessage = new StringBuilder();
        AtomicLong totalPrice = new AtomicLong();
        List<SerializedItemStack> itemStacks = new ArrayList<>(clueItems.size());
        List<Embed> embeds = new ArrayList<>(config.clueShowItems() ? clueItems.size() : 0);

        clueItems.forEach((itemId, quantity) -> {
            if (lootMessage.length() > 0) lootMessage.append('\n');
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, itemId, quantity);
            totalPrice.addAndGet(stack.getTotalPrice());
            itemStacks.add(stack);
            lootMessage.append(getItemMessage(stack, embeds));
        });

        if (totalPrice.get() >= config.clueMinValue()) {
            boolean screenshot = config.clueSendImage() && totalPrice.get() >= config.clueImageMinValue();

            String cluePlaceholder = placeholder.asPlaceholder(clueType, "Clue scroll (" + clueType + ")");
            String notifyMessage = StringUtils.replaceEach(
                config.clueNotifyMessage(),
                new String[]{"%USERNAME%", "%CLUE%", "%COUNT%", "%TOTAL_VALUE%", "%LOOT%"},
                new String[]{Utils.getPlayerName(client),
                    cluePlaceholder,
                    clueCount, QuantityFormatter.quantityToStackSize(totalPrice.get()), lootMessage.toString()}
            );
            createMessage(screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .extra(new ClueNotificationData(clueType, Integer.parseInt(clueCount), itemStacks))
                    .type(NotificationType.CLUE)
                    .embeds(embeds)
                    .build()
            );
        }

        this.reset();
    }

    private String getItemMessage(SerializedItemStack item, Collection<Embed> embeds) {
        if (config.clueShowItems())
            embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
        return ItemUtils.formatStack(item, this.placeholder);
    }

    private boolean checkClueTier(String clueType) {
        ClueTier tier = ClueTier.parse(clueType);
        if (tier == null) {
            log.warn("Failed to parse clue tier: {}", clueType);
            return true; // permissive approach
        }
        return tier.ordinal() >= config.clueMinTier().ordinal();
    }

    public void reset() {
        this.clueCount = "";
        this.clueType = "";
        this.badTicks.set(0);
    }
}
