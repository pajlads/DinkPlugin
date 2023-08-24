package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Evaluable;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
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
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GambleNotifier extends BaseNotifier {
    // itemName (x quantity)![ tertiaryReward!] gc: n
    private static final Pattern GAMBLE_REGEX = Pattern.compile("^(.+?)!\\s*(.+!)?\\s+High level gamble count: (\\d+).*");
    private static final Pattern ITEM_QUANTITY_REGEX = Pattern.compile("^(.+?)\\(x (\\d+)\\)$");
    // penance pet is not actually present in dialog message, but it's here in case it's ever added
    private static final Collection<String> RARE_LOOT = ImmutableSet.of("dragon chainbody", "dragon med helm", "pet penance queen");

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
        List<SerializedItemStack> items = serializeItems(data);
        Template message = Template.builder()
            .template(messageFormat)
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(player))
            .replacement("%COUNT%", Replacements.ofText(String.valueOf(data.gambleCount)))
            .replacement("%LOOT%", lootSummary(items))
            .build();
        createMessage(config.gambleSendImage(), NotificationBody.builder()
            .text(message)
            .extra(new GambleNotificationData(data.gambleCount, items))
            .type(NotificationType.BARBARIAN_ASSAULT_GAMBLE)
            .build());
    }

    private static Evaluable lootSummary(List<SerializedItemStack> items) {
        JoiningReplacement.JoiningReplacementBuilder builder = JoiningReplacement.builder().delimiter("\n");
        items.forEach(item -> builder.component(ItemUtils.templateStack(item, true)));
        return builder.build();
    }

    private List<SerializedItemStack> serializeItems(ParsedData data) {
        return Stream.of(Pair.of(data.itemName, data.itemQuantity), Pair.of(data.tertiaryItem, 1))
            .filter(p -> p.getLeft() != null)
            .map(p -> Pair.of(itemSearcher.findItemId(p.getLeft()), p.getRight()))
            .filter(p -> p.getLeft() != null)
            .map(p -> ItemUtils.stackFromItem(itemManager, p.getLeft(), p.getRight()))
            .collect(Collectors.toList());
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
            tertiary = StringUtils.removeEnd(tertiary, "!").trim();
        }
        int gambleCount = Integer.parseInt(matcher.group(3));
        return new ParsedData(itemName, itemQuantity, tertiary, gambleCount);
    }

    @Value
    @VisibleForTesting
    static class ParsedData {
        @NonNull
        String itemName;
        int itemQuantity;
        // https://oldschool.runescape.wiki/w/Barbarian_Assault/Rewards#Tertiary_High_Gamble_Rewards
        @Nullable
        String tertiaryItem;
        int gambleCount;
    }
}
