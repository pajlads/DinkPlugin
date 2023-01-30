package dinkplugin.notifiers;

import com.google.common.collect.ImmutableList;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.GambleNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class GambleNotifier extends BaseNotifier {
    // itemName (x quantity)![ tertiaryReward!] gc: n
    private static final Pattern GAMBLE_REGEX = Pattern.compile(
        "^(.+?)!\\s*(.+!)?\\s+High level gamble count: (\\d+).*"
    );
    private static final Pattern ITEM_QUANTITY_REGEX = Pattern.compile("^(.+?)\\(x (\\d+)\\)$");
    private static final List<String> RARE_LOOT = ImmutableList.of("dragon chainbody", "dragon med helm");

    @Value
    @VisibleForTesting
    static class ParsedData {
        @NonNull
        String itemName;
        int itemQuantity;
        @Nullable
        String tertiaryItem;
        int gambleCount;

        Stream<Pair<String, Integer>> itemsWithQuantities() {
            return Stream.of(Pair.of(itemName, itemQuantity), Pair.of(tertiaryItem, 1))
                .filter(p -> p.getLeft() != null);
        }
    }

    @Inject
    private ItemSearcher itemSearcher;

    @Inject
    private ItemManager itemManager;

    @Override
    public boolean isEnabled() {
        return config.notifyGamble() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.gambleWebhook();
    }

    public void onMesBoxNotification(String message) {
        if (!isEnabled()) return;
        ParsedData data = parse(message);
        if (data != null) {
            handleNotify(data);
        }
    }

    @Nullable
    @VisibleForTesting
    static ParsedData parse(String message) {
        Matcher matcher = GAMBLE_REGEX.matcher(message);
        if (!matcher.matches()) return null;
        String rawItem = matcher.group(1).trim();
        String itemName = rawItem;
        int itemQuantity = 1;
        Matcher quantityMatcher = ITEM_QUANTITY_REGEX.matcher(rawItem);
        if (quantityMatcher.matches()) {
            itemName = quantityMatcher.group(1).trim();
            itemQuantity = Integer.parseInt(quantityMatcher.group(2));
        }
        String tertiary = matcher.group(2);
        if (tertiary != null) {
            tertiary = tertiary.replaceAll("!", "").trim();
        }
        int gambleCount = Integer.parseInt(matcher.group(3));
        return new ParsedData(itemName, itemQuantity, tertiary, gambleCount);
    }

    private void handleNotify(ParsedData data) {
        String messageFormat;
        if (config.gambleRareLoot() && RARE_LOOT.contains(data.itemName.toLowerCase())) {
            messageFormat = config.gambleRareNotifyMessage();
        } else if (data.gambleCount % config.gambleInterval() == 0) {
            messageFormat = config.gambleNotifyMessage();
        } else {
            return;
        }
        String player = Utils.getPlayerName(client);
        String message = StringUtils.replaceEach(
            messageFormat,
            new String[] { "%USERNAME%", "%COUNT%", "%LOOT%" },
            new String[] { player, String.valueOf(data.gambleCount), lootSummary(data) }
        );
        createMessage(config.gambleSendImage(), NotificationBody.builder()
            .text(message)
            .extra(new GambleNotificationData(data.gambleCount, serializeItems(data)))
            .type(NotificationType.BARBARIAN_ASSAULT_GAMBLE)
            .build());
    }

    // ex. "watermelon seed x 50, clue scroll (elite)"
    private String lootSummary(ParsedData data) {
        return data.itemsWithQuantities()
            .map(p -> {
                Integer count = p.getRight();
                return p.getLeft().toLowerCase() + (count > 1 ? " x " + count : "");
            })
            .collect(Collectors.joining(", "));
    }

    private List<SerializedItemStack> serializeItems(ParsedData data) {
        return data.itemsWithQuantities()
            .map(p -> Pair.of(itemSearcher.findItemId(p.getLeft()), p.getRight()))
            .filter(p -> p.getLeft() != null)
            .map(p -> ItemUtils.stackFromItem(itemManager, p.getLeft(), p.getRight()))
            .collect(Collectors.toList());
    }
}
