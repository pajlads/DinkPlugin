package dinkplugin.notifiers;

import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GrandExchangeNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GrandExchangeNotifier extends BaseNotifier {
    private static final int LOGIN_DELAY = 2;
    private final AtomicInteger initTicks = new AtomicInteger();
    private final Map<Integer, Instant> progressNotificationTimeBySlot = new HashMap<>();

    @Inject
    private ItemManager itemManager;

    @Override
    public boolean isEnabled() {
        return config.notifyGrandExchange() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.grandExchangeWebhook();
    }

    public void onAccountChange() {
        progressNotificationTimeBySlot.clear();
    }

    public void onGameStateChange(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            initTicks.set(LOGIN_DELAY);
    }

    public void onTick() {
        initTicks.updateAndGet(i -> Math.max(i - 1, 0));
    }

    public void onOfferChange(int slot, GrandExchangeOffer offer) {
        if (shouldNotify(slot, offer)) {
            handleNotify(slot, offer);
        }
    }

    private void handleNotify(int slot, GrandExchangeOffer offer) {
        log.debug("Notifying for slot={}, item={}, quantity={}, price={}, state={}",
            slot, offer.getItemId(), offer.getQuantitySold(), offer.getPrice(), offer.getState());

        ItemComposition comp = itemManager.getItemComposition(offer.getItemId());
        SerializedItemStack item = new SerializedItemStack(offer.getItemId(), offer.getQuantitySold(), getUnitPrice(offer), comp.getMembersName());
        long marketPrice = ItemUtils.getPrice(itemManager, offer.getItemId());

        List<Embed> embeds;
        if (config.grandExchangeSendImage()) {
            embeds = Collections.emptyList();
        } else {
            embeds = ItemUtils.buildEmbeds(new int[]{offer.getItemId()});
        }

        String playerName = Utils.getPlayerName(client);
        Template message = Template.builder()
            .template(config.grandExchangeNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%TYPE%", Replacements.ofText(getHumanType(offer.getState())))
            .replacement("%ITEM%", ItemUtils.templateStack(item))
            .replacement("%STATUS%", Replacements.ofText(getHumanStatus(offer.getState())))
            .build();
        createMessage(config.grandExchangeSendImage(), NotificationBody.builder()
            .type(NotificationType.GRAND_EXCHANGE)
            .text(message)
            .embeds(embeds)
            .extra(new GrandExchangeNotificationData(slot + 1, offer.getState(), item, marketPrice, offer.getTotalQuantity()))
            .playerName(playerName)
            .build());
    }

    private boolean shouldNotify(int slot, GrandExchangeOffer offer) {
        if (initTicks.get() > 0)
            return false;

        boolean valuable = getTransactedValue(offer) >= config.grandExchangeMinValue();

        switch (offer.getState()) {
            case EMPTY:
                if (client.getGameState() == GameState.LOGGED_IN)
                    progressNotificationTimeBySlot.remove(slot);
                return false;

            case BOUGHT:
            case SOLD:
                progressNotificationTimeBySlot.remove(slot);
                return valuable;

            case CANCELLED_BUY:
            case CANCELLED_SELL:
                progressNotificationTimeBySlot.remove(slot);
                return valuable && config.grandExchangeIncludeCancelled();

            case BUYING:
            case SELLING:
                if (!valuable)
                    return false;

                if (offer.getQuantitySold() >= offer.getTotalQuantity())
                    return false; // ignore since BOUGHT/SOLD is about to occur

                int spacing = config.grandExchangeProgressSpacingMinutes();
                if (spacing < 0)
                    return false; // negative => no in-progress notifications allowed

                // convert minutes to seconds, but treat 0 minutes as 2 seconds to workaround duplicate RL events
                long spacingSeconds = spacing > 0 ? spacing * 60L : 2L;

                Instant now = Instant.now();
                Instant prior = progressNotificationTimeBySlot.get(slot);
                if (prior == null || Duration.between(prior, now).getSeconds() >= spacingSeconds) {
                    return progressNotificationTimeBySlot.put(slot, now) == prior;
                }
                return false;

            default:
                return false;
        }
    }

    public static String getHumanStatus(GrandExchangeOfferState state) {
        switch (state) {
            case CANCELLED_BUY:
            case CANCELLED_SELL:
                return "Cancelled";

            case BUYING:
            case SELLING:
                return "In Progress";

            case BOUGHT:
            case SOLD:
                return "Completed";

            default:
                return null;
        }
    }

    private static String getHumanType(GrandExchangeOfferState state) {
        switch (state) {
            case BUYING:
            case CANCELLED_BUY:
            case BOUGHT:
                return "bought";

            case SELLING:
            case CANCELLED_SELL:
            case SOLD:
                return "sold";

            default:
                return null;
        }
    }

    private static long getTransactedValue(GrandExchangeOffer offer) {
        long spent = offer.getSpent();
        return spent > 0 ? spent : (long) offer.getQuantitySold() * offer.getPrice();
    }

    private static int getUnitPrice(GrandExchangeOffer offer) {
        int quantity = offer.getQuantitySold();
        int spent = offer.getSpent();
        return quantity > 0 && spent > 0 ? spent / quantity : offer.getPrice();
    }
}
