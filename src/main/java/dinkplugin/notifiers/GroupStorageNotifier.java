package dinkplugin.notifiers;

import dinkplugin.message.Field;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.GroupStorageNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Singleton
public class GroupStorageNotifier extends BaseNotifier {
    static final @VisibleForTesting int GROUP_STORAGE_LOADER_ID = 293;
    static final @VisibleForTesting String EMPTY_TRANSACTION = "N/A";
    private static final BinaryOperator<Integer> SUM;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

    private Map<Integer, Integer> initial = Collections.emptyMap();

    @Override
    public boolean isEnabled() {
        return config.notifyBank() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.bankWebhook();
    }

    public void onWidgetLoad(WidgetLoaded event) {
        if (event.getGroupId() != WidgetID.GROUP_STORAGE_GROUP_ID)
            return;

        clientThread.invokeLater(() -> {
            ItemContainer inv = getInventory();
            if (inv == null)
                return false;

            initial = reduce(inv.getItems());
            return true;
        });
    }

    public void onWidgetClose(WidgetClosed event) {
        if (event.getGroupId() != GROUP_STORAGE_LOADER_ID)
            return;

        Widget widget = client.getWidget(GROUP_STORAGE_LOADER_ID, 1);
        if (widget == null)
            return;

        if (isEnabled() && StringUtils.containsIgnoreCase(widget.getText(), "Saving")) {
            ItemContainer inv = getInventory();
            if (inv != null) {
                Map<Integer, Integer> modified = reduce(inv.getItems());
                Map<Integer, Integer> delta = computeDifference(initial, modified);
                if (!delta.isEmpty()) {
                    handleNotify(delta);
                }
            }
        }

        initial = Collections.emptyMap();
    }

    private void handleNotify(Map<Integer, Integer> inventoryChanges) {
        // Calculate transaction information
        List<SerializedItemStack> deposits = new ArrayList<>();
        List<SerializedItemStack> withdrawals = new ArrayList<>();
        long debits = 0, credits = 0;
        for (Map.Entry<Integer, Integer> entry : inventoryChanges.entrySet()) {
            int diff = entry.getValue(); // positive=withdraw, negative=deposit
            SerializedItemStack item = ItemUtils.stackFromItem(itemManager, entry.getKey(), Math.abs(diff));
            long stackPrice = item.getTotalPrice();
            if (diff < 0) {
                deposits.add(item);
                debits += stackPrice;
            } else {
                withdrawals.add(item);
                credits += stackPrice;
            }
        }
        long netValue = debits - credits;

        // Ensure transaction is large enough to be logged
        if (debits < config.bankMinValue() && credits < config.bankMinValue())
            return;

        // Sort lists so more valuable item transactions are at the top
        Comparator<SerializedItemStack> valuable = Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed();
        deposits.sort(valuable);
        withdrawals.sort(valuable);

        // Convert lists to strings
        BiFunction<Collection<SerializedItemStack>, String, String> formatItems = (items, linePrefix) -> {
            if (items.isEmpty()) return EMPTY_TRANSACTION;
            return items.stream()
                .map(ItemUtils::formatStack)
                .collect(Collectors.joining('\n' + linePrefix, linePrefix, ""));
        };
        String depositString = formatItems.apply(deposits, "+ ");
        String withdrawalString = formatItems.apply(withdrawals, "- ");

        // Build content
        String playerName = client.getLocalPlayer().getName();
        String content = StringUtils.replaceEach(config.bankNotifyMessage(),
            new String[] { "%USERNAME%", "%DEBITS%", "%CREDITS%" },
            new String[] { playerName, depositString, withdrawalString }
        );
        String formattedText = config.discordRichEmbeds() ? Field.formatBlock("diff", content) : content;

        // Populate metadata
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            deposits,
            withdrawals,
            netValue
        );

        // Fire notification
        createMessage(config.bankSendImage(), NotificationBody.builder()
            .type(NotificationType.GROUP_STORAGE)
            .text(formattedText)
            .playerName(playerName)
            .extra(extra)
            .build());
    }

    private ItemContainer getInventory() {
        ItemContainer inv = client.getItemContainer(InventoryID.GROUP_STORAGE_INV);
        return inv != null ? inv : client.getItemContainer(InventoryID.INVENTORY);
    }

    private static Map<Integer, Integer> reduce(Item[] items) {
        return Arrays.stream(items)
            .filter(Objects::nonNull)
            .filter(item -> item.getId() >= 0)
            .filter(item -> item.getQuantity() > 0)
            .collect(Collectors.toMap(GroupStorageNotifier::getId, Item::getQuantity, Integer::sum));
    }

    private static int getId(Item item) {
        int id = item.getId();
        if (id == ItemID.COINS_995 || id == ItemID.COINS_8890 || id == ItemID.COINS_6964)
            return ItemID.COINS;
        return id;
    }

    private static <K> Map<K, Integer> computeDifference(Map<K, Integer> before, Map<K, Integer> after) {
        Map<K, Integer> delta = new HashMap<>(after);
        before.forEach((id, quantity) -> delta.merge(id, -quantity, SUM));
        return delta;
    }

    static {
        // Integer::sum but 0 yields null (which removes the entry in Map#merge)
        SUM = (a, b) -> {
            int sum = a + b;
            return sum != 0 ? sum : null;
        };
    }
}
