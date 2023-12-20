package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GroupStorageNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.annotations.Interface;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
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

/**
 * Tracks when items are deposited or withdrawn to GIM shared storage.
 * <p>
 * This is achieved by comparing snapshots of the player's inventory
 * when the storage is opened and when the transaction is saved.
 * When the difference between these two snapshots is non-empty,
 * we fire a notification (given the configured min value is satisfied).
 */
@Singleton
public class GroupStorageNotifier extends BaseNotifier {

    /**
     * The Group ID for tracking when GIM shared storage is opened.
     */
    static final @VisibleForTesting int GROUP_STORAGE_WIDGET_GROUP = InterfaceID.GROUP_STORAGE;

    /**
     * The Group ID of the widget that appears over the chat box
     * when the storage transaction is saved/committed.
     */
    static final @VisibleForTesting @Interface int GROUP_STORAGE_SAVING_WIDGET_ID = 293;

    /**
     * The message to indicate that a list of deposits or withdrawals is empty.
     */
    static final @VisibleForTesting String EMPTY_TRANSACTION = "N/A";

    /**
     * Adds two integers, but yields null if the sum is zero
     * (which removes the entry via Map#merge in {@link GroupStorageNotifier#computeDifference(Map, Map)}).
     */
    private static final BinaryOperator<Integer> SUM;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

    /**
     * Items in the player's inventory when the group storage was opened.
     * Entries map item id to total quantity (across stacks).
     */
    private Map<Integer, Integer> initialInventory = Collections.emptyMap();

    @Override
    public boolean isEnabled() {
        return config.notifyGroupStorage() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.groupStorageWebhook();
    }

    public void reset() {
        clientThread.invoke(() -> initialInventory = Collections.emptyMap());
    }

    public void onWidgetLoad(WidgetLoaded event) {
        if (event.getGroupId() != GROUP_STORAGE_WIDGET_GROUP)
            return;

        clientThread.invokeLater(() -> {
            ItemContainer inv = getInventory();
            if (inv == null)
                return false;

            initialInventory = reduce(inv.getItems());
            return true;
        });
    }

    public void onWidgetClose(WidgetClosed event) {
        if (event.getGroupId() != GROUP_STORAGE_SAVING_WIDGET_ID)
            return;

        Widget widget = client.getWidget(GROUP_STORAGE_SAVING_WIDGET_ID, 1);
        if (widget == null)
            return;

        if (isEnabled() && StringUtils.containsIgnoreCase(widget.getText(), "Saving")) {
            ItemContainer inv = getInventory();
            if (inv != null) {
                Map<Integer, Integer> updatedInventory = reduce(inv.getItems());
                Map<Integer, Integer> delta = computeDifference(initialInventory, updatedInventory);
                if (!delta.isEmpty()) {
                    handleNotify(delta);
                }
            }
        }

        this.reset();
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
        if (debits < config.groupStorageMinValue() && credits < config.groupStorageMinValue())
            return;

        // Sort lists so more valuable item transactions are at the top
        Comparator<SerializedItemStack> valuable = Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed();
        deposits.sort(valuable);
        withdrawals.sort(valuable);

        // Convert lists to strings
        BiFunction<Collection<SerializedItemStack>, String, String> formatItems = (items, linePrefix) -> {
            if (items.isEmpty()) return EMPTY_TRANSACTION;
            return items.stream()
                .map(n -> ItemUtils.formatStack(n, config.groupStorageIncludePrice()))
                .collect(Collectors.joining('\n' + linePrefix, linePrefix, ""));
        };
        String depositString = formatItems.apply(deposits, "+ ");
        String withdrawalString = formatItems.apply(withdrawals, "- ");

        // Build content
        String playerName = client.getLocalPlayer().getName();
        String content = StringUtils.replaceEach(config.groupStorageNotifyMessage(),
            new String[] { "%USERNAME%", "%DEPOSITED%", "%WITHDRAWN%" },
            new String[] { playerName, depositString, withdrawalString }
        );
        Template formattedText = Template.builder()
            .template("$s$")
            .replacementBoundary("$")
            .replacement("$s$", Replacements.ofBlock("diff", content))
            .build();

        // Populate metadata
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            deposits,
            withdrawals,
            netValue,
            getGroupName(),
            config.groupStorageIncludePrice()
        );

        // Fire notification (delayed by a tick for screenshotHideChat reliability)
        clientThread.invokeAtTickEnd(() -> createMessage(config.groupStorageSendImage(),
            NotificationBody.builder()
                .type(NotificationType.GROUP_STORAGE)
                .text(formattedText)
                .playerName(playerName)
                .extra(extra)
                .build())
        );
    }

    /**
     * @return the name of the ironman group
     */
    private String getGroupName() {
        if (!config.groupStorageIncludeClan()) return null;
        ClanChannel channel = client.getClanChannel(ClanID.GROUP_IRONMAN);
        return channel != null ? channel.getName() : null;
    }

    /**
     * Upon opening the group's shared storage, the inventory is replaced
     * by a temporary container ({@link InventoryID#GROUP_STORAGE_INV}),
     * which reflects intermediate changes before the transaction is committed.
     * We prefer reading from this "fake" inventory when available.
     * On storage open, it is a server-validated copy of the local inventory.
     * On save, it has modifications that may not yet be reflected in the real inventory.
     *
     * @return the player's inventory
     */
    private ItemContainer getInventory() {
        ItemContainer inv = client.getItemContainer(InventoryID.GROUP_STORAGE_INV);
        return inv != null ? inv : client.getItemContainer(InventoryID.INVENTORY);
    }

    /**
     * @param items array of items (e.g., in the player's inventory)
     * @return mappings of item id to total quantity of the item (across stacks)
     */
    private Map<Integer, Integer> reduce(Item[] items) {
        return Arrays.stream(items)
            .filter(Objects::nonNull)
            .filter(item -> item.getId() >= 0)
            .filter(item -> item.getQuantity() > 0)
            .collect(Collectors.toMap(this::getItemId, Item::getQuantity, Integer::sum));
    }

    /**
     * @param item the item whose ID to query
     * @return the canonical item ID
     */
    private int getItemId(Item item) {
        int id = item.getId();
        if (ItemUtils.COIN_VARIATIONS.contains(id))
            return ItemID.COINS; // use single ID for all coins
        return itemManager.canonicalize(id); // un-noted, un-placeholdered, un-worn
    }

    /**
     * @param before the reduced item mappings when the group storage was first opened
     * @param after  the reduced item mappings after the save operation
     * @param <K>    generic key to merge the maps (always Integer in this notifier)
     * @return mappings of item id to change in quantity, excluding items with no change
     */
    private static <K> Map<K, Integer> computeDifference(Map<K, Integer> before, Map<K, Integer> after) {
        Map<K, Integer> delta = new HashMap<>(after);
        before.forEach((id, quantity) -> delta.merge(id, -quantity, SUM));
        return delta;
    }

    static {
        SUM = (a, b) -> {
            int sum = a + b;
            return sum != 0 ? sum : null;
        };
    }
}
