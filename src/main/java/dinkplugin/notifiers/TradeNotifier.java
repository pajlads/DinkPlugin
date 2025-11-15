package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.notifiers.data.TradeNotificationData;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.annotations.VarCStr;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.QuantityFormatter;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class TradeNotifier extends BaseNotifier {
    @VisibleForTesting
    public static final String TRADE_ACCEPTED_MESSAGE = "Accepted trade.";
    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bclientscript%2Ctrade_partner_set%5D.cs2#L3">CS2 Reference</a>
     */
    @VisibleForTesting
    static final @VarCStr int TRADE_COUNTERPARTY_VAR = 357;

    @VisibleForTesting
    static final int INV_TRADE_OTHER = InventoryID.TRADEOFFER | 0x8000;

    @Inject
    private ClientThread clientThread;

    @Inject
    private DrawManager drawManager;

    @Inject
    private ImageCapture imageCapture;

    @Inject
    private ItemManager itemManager;

    private final AtomicReference<Image> image = new AtomicReference<>();

    @Override
    public boolean isEnabled() {
        return config.notifyTrades() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.tradeWebhook();
    }

    public void reset() {
        image.lazySet(null);
    }

    public void onTradeMessage(String message) {
        if (!TRADE_ACCEPTED_MESSAGE.equals(message) || !isEnabled()) {
            this.reset();
            return;
        }

        ItemContainer tradeInv = client.getItemContainer(InventoryID.TRADEOFFER);
        ItemContainer otherInv = client.getItemContainer(INV_TRADE_OTHER);
        if (tradeInv == null && otherInv == null) {
            log.debug("Could not find traded items!");
            this.reset();
            return;
        }
        Item[] trade = tradeInv != null ? tradeInv.getItems() : new Item[0];
        Item[] other = otherInv != null ? otherInv.getItems() : new Item[0];
        long receiveValue = getTotalValue(other);
        long giveValue = getTotalValue(trade);
        if (receiveValue + giveValue < config.tradeMinValue()) {
            this.reset();
            return;
        }
        List<SerializedItemStack> received = getItems(other);
        List<SerializedItemStack> disbursed = getItems(trade);

        String localPlayer = client.getLocalPlayer().getName();
        String counterparty = Utils.sanitize(client.getVarcStrValue(TRADE_COUNTERPARTY_VAR));
        Template content = Template.builder()
            .template(config.tradeNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(localPlayer))
            .replacement("%COUNTERPARTY%", Replacements.ofLink(counterparty, config.playerLookupService().getPlayerUrl(counterparty)))
            .replacement("%IN_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(receiveValue)))
            .replacement("%OUT_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(giveValue)))
            .build();

        createMessage(config.tradeSendImage(), NotificationBody.builder()
            .text(content)
            .extra(new TradeNotificationData(counterparty, received, disbursed, receiveValue, giveValue))
            .playerName(localPlayer)
            .screenshotOverride(image.get())
            .type(NotificationType.TRADE)
            .build()
        );

        this.reset();
    }

    public void onWidgetLoad(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.TRADECONFIRM) {
            Utils.captureScreenshot(client, clientThread, drawManager, imageCapture, executor, config, image::set);
        }
    }

    public void onWidgetClose(WidgetClosed event) {
        // relevant when local player declines a trade since no chat message occurs
        if (event.getGroupId() == InterfaceID.TRADECONFIRM) {
            clientThread.invokeAtTickEnd(this::reset);
        }
    }

    private long getTotalValue(Item[] items) {
        long v = 0;
        for (Item item : items) {
            v += ItemUtils.getPrice(itemManager, item.getId()) * item.getQuantity();
        }
        return v;
    }

    private List<SerializedItemStack> getItems(Item[] items) {
        if (items.length == 0) {
            return Collections.emptyList();
        }
        Map<Integer, Integer> quantityById = new HashMap<>(items.length * 4 / 3);
        for (Item item : items) {
            quantityById.merge(item.getId(), item.getQuantity(), Integer::sum);
        }
        List<SerializedItemStack> stacks = new ArrayList<>(quantityById.size());
        quantityById.forEach((id, quantity) -> stacks.add(ItemUtils.stackFromItem(itemManager, id, quantity)));
        return stacks;
    }
}
