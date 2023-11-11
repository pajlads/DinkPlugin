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
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.annotations.Interface;
import net.runelite.api.annotations.VarCStr;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.DrawManager;
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

import static dinkplugin.message.DiscordMessageHandler.PRIVATE_CHAT_WIDGET;

@Slf4j
@Singleton
public class TradeNotifier extends BaseNotifier {
    @VisibleForTesting
    public static final String TRADE_ACCEPTED_MESSAGE = "Accepted trade.";
    @VisibleForTesting
    public static final @VarCStr int TRADE_COUNTERPARTY_VAR = 357;
    private static final @Interface int TRADE_CONFIRMATION_GROUP = 334;

    @Inject
    private ClientThread clientThread;

    @Inject
    private DrawManager drawManager;

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

        ItemContainer tradeInv = client.getItemContainer(InventoryID.TRADE);
        ItemContainer otherInv = client.getItemContainer(InventoryID.TRADEOTHER);
        if (tradeInv == null && otherInv == null) {
            log.debug("Could not find traded items!");
            this.reset();
            return;
        }
        Item[] trade = tradeInv != null ? tradeInv.getItems() : new Item[0];
        Item[] other = otherInv != null ? otherInv.getItems() : new Item[0];
        long receiveValue = getTotalValue(other);
        long giveValue = getTotalValue(trade);
        long grossValue = receiveValue + giveValue;
        long netValue = receiveValue - giveValue;
        if (grossValue < config.tradeMinValue()) {
            this.reset();
            return;
        }
        List<SerializedItemStack> received = getItems(other);
        List<SerializedItemStack> discarded = getItems(trade);

        String localPlayer = client.getLocalPlayer().getName();
        String counterparty = client.getVarcStrValue(TRADE_COUNTERPARTY_VAR);
        Template content = Template.builder()
            .template(config.tradeNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(localPlayer))
            .replacement("%COUNTERPARTY%", Replacements.ofLink(counterparty, config.playerLookupService().getPlayerUrl(counterparty)))
            .replacement("%GROSS_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(grossValue)))
            .replacement("%NET_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(netValue)))
            .build();

        createMessage(config.tradeSendImage(), NotificationBody.builder()
            .text(content)
            .extra(new TradeNotificationData(counterparty, received, discarded, grossValue, netValue))
            .playerName(localPlayer)
            .screenshotOverride(image.get())
            .type(NotificationType.TRADE)
            .build()
        );

        this.reset();
    }

    public void onWidgetLoad(WidgetLoaded event) {
        if (event.getGroupId() == TRADE_CONFIRMATION_GROUP) {
            boolean chatHidden = Utils.hideWidget(config.screenshotHideChat(), client, ComponentID.CHATBOX_FRAME);
            boolean whispersHidden = Utils.hideWidget(config.screenshotHideChat(), client, PRIVATE_CHAT_WIDGET);
            drawManager.requestNextFrameListener(frame -> {
                image.set(frame);
                Utils.unhideWidget(chatHidden, client, clientThread, ComponentID.CHATBOX_FRAME);
                Utils.unhideWidget(whispersHidden, client, clientThread, PRIVATE_CHAT_WIDGET);
            });
        }
    }

    public void onWidgetClose(WidgetClosed event) {
        // should be unnecessary, but frees memory just to be safe
        if (event.getGroupId() == TRADE_CONFIRMATION_GROUP) {
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
