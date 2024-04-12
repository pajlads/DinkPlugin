package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GrandExchangeNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.SerializedOffer;
import dinkplugin.util.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.grandexchange.GrandExchangePlugin;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class GrandExchangeNotifier extends BaseNotifier {
    private static final Set<Integer> TAX_EXEMPT_ITEMS;
    private static final int LOGIN_DELAY = 2;
    private static final String RL_GE_PLUGIN_NAME = GrandExchangePlugin.class.getSimpleName().toLowerCase();
    private final AtomicInteger initTicks = new AtomicInteger();
    private final Map<Integer, Instant> progressNotificationTimeBySlot = new HashMap<>();

    @Inject
    private Gson gson;

    @Inject
    private ConfigManager configManager;

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
        log.debug("Notifying for slot={}, item={}, quantity={}, targetPrice={}, spent={}, state={}",
            slot, offer.getItemId(), offer.getQuantitySold(), offer.getPrice(), offer.getSpent(), offer.getState());

        ItemComposition comp = itemManager.getItemComposition(offer.getItemId());
        SerializedItemStack item = new SerializedItemStack(offer.getItemId(), offer.getQuantitySold(), getUnitPrice(offer), comp.getMembersName());
        long marketPrice = ItemUtils.getPrice(itemManager, offer.getItemId());
        OfferType type = getType(offer.getState());
        Long tax = type == OfferType.SELL ? calculateTax(item.getPriceEach(), item.getQuantity(), item.getId()) : null;

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
            .replacement("%TYPE%", Replacements.ofText(type.getDisplayName()))
            .replacement("%ITEM%", ItemUtils.templateStack(item, true))
            .replacement("%STATUS%", Replacements.ofText(getHumanStatus(offer.getState())))
            .build();
        createMessage(config.grandExchangeSendImage(), NotificationBody.builder()
            .type(NotificationType.GRAND_EXCHANGE)
            .text(message)
            .embeds(embeds)
            .extra(new GrandExchangeNotificationData(slot + 1, offer.getState(), item, marketPrice, offer.getPrice(), offer.getTotalQuantity(), tax))
            .playerName(playerName)
            .build());
    }

    private boolean shouldNotify(int slot, GrandExchangeOffer offer) {
        // During login, we only care about offers that have been completed, and that were *not* observed by the RuneLite GE plugin
        // This makes sure we don't fire any duplicate notifications for offers that were finished while we were online
        if (initTicks.get() > 0) {
            // check if offer is a completion
            if (offer.getState() != GrandExchangeOfferState.BOUGHT && offer.getState() != GrandExchangeOfferState.SOLD)
                return false;

            // require GE plugin to be enabled so that observed trades are written to config
            // however: not bullet-proof since GE plugin could've been disabled during the initial trade completion
            if (ConfigUtil.isPluginDisabled(configManager, RL_GE_PLUGIN_NAME))
                return false;

            // check whether the completion has already been observed
            if (getSavedOffer(slot).filter(saved -> saved.equals(offer)).isPresent())
                return false;
        }

        if (!isEnabled())
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
                if (!valuable || offer.getQuantitySold() <= 0)
                    return false;

                if (offer.getQuantitySold() >= offer.getTotalQuantity())
                    return false; // ignore since BOUGHT/SOLD is about to occur

                int spacing = config.grandExchangeProgressSpacingMinutes();
                if (spacing < 0)
                    return false; // negative => no in-progress notifications allowed

                if (getSavedOffer(slot).filter(saved -> saved.equals(offer)).isPresent())
                    return false; // ignore since quantity already observed (relevant when trade limit is binding)

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

    private Optional<SerializedOffer> getSavedOffer(int slot) {
        return Optional.ofNullable(configManager.getRSProfileConfiguration("geoffer", String.valueOf(slot)))
            .map(json -> {
                try {
                    return gson.fromJson(json, SerializedOffer.class);
                } catch (JsonSyntaxException e) {
                    log.warn("Failed to read saved GE offer", e);
                    return null;
                }
            });
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

    private static OfferType getType(GrandExchangeOfferState state) {
        switch (state) {
            case BUYING:
            case CANCELLED_BUY:
            case BOUGHT:
                return OfferType.BUY;

            case SELLING:
            case CANCELLED_SELL:
            case SOLD:
                return OfferType.SELL;

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

    private static long calculateTax(int unitPrice, int quantity, int itemId) {
        // https://secure.runescape.com/m=news/grand-exchange-tax--item-sink?oldschool=1
        if (unitPrice < 100 || TAX_EXEMPT_ITEMS.contains(itemId)) {
            return 0L;
        }
        int price = Math.min(unitPrice, 500_000_000);
        int unitTax = (int) Math.floor(price * 0.01);
        return (long) unitTax * quantity;
    }

    @Getter
    @RequiredArgsConstructor
    private enum OfferType {
        BUY("bought"),
        SELL("sold");

        private final String displayName;
    }

    static {
        // https://oldschool.runescape.wiki/w/Category:Items_exempt_from_Grand_Exchange_tax
        TAX_EXEMPT_ITEMS = ImmutableSet.of(
            ItemID.CHISEL, ItemID.SEED_DIBBER, ItemID.GARDENING_TROWEL,
            ItemID.GLASSBLOWING_PIPE, ItemID.HAMMER, ItemID.NEEDLE,
            ItemID.PESTLE_AND_MORTAR, ItemID.RAKE, ItemID.SAW,
            ItemID.SECATEURS, ItemID.SHEARS, ItemID.SPADE,
            ItemID.WATERING_CAN, ItemID.OLD_SCHOOL_BOND
        );
    }
}
