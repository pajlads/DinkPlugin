package dinkplugin.notifiers;

import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Evaluable;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.RareItemStack;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import dinkplugin.util.MathUtils;
import dinkplugin.util.RarityService;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LootNotifier extends BaseNotifier {

    @Inject
    private ItemManager itemManager;

    @Inject
    private KillCountService killCountService;

    @Inject
    private RarityService rarityService;

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();
    private final Collection<Pattern> itemNameDenylist = new CopyOnWriteArrayList<>();

    @Override
    public boolean isEnabled() {
        return config.notifyLoot() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.lootWebhook();
    }

    public void init() {
        itemNameAllowlist.clear();
        itemNameAllowlist.addAll(
            ConfigUtil.readDelimited(config.lootItemAllowlist())
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );

        itemNameDenylist.clear();
        itemNameDenylist.addAll(
            ConfigUtil.readDelimited(config.lootItemDenylist())
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    public void onConfigChanged(String key, String value) {
        Collection<Pattern> itemNames;
        if ("lootItemAllowlist".equals(key)) {
            itemNames = itemNameAllowlist;
        } else if ("lootItemDenylist".equals(key)) {
            itemNames = itemNameDenylist;
        } else {
            return;
        }

        itemNames.clear();
        itemNames.addAll(
            ConfigUtil.readDelimited(value)
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (!isEnabled()) return;

        NPC npc = event.getNpc();
        int id = npc.getId();
        if (id == NpcID.THE_WHISPERER || id == NpcID.THE_WHISPERER_12205 || id == NpcID.THE_WHISPERER_12206 || id == NpcID.THE_WHISPERER_12207) {
            // Upstream does not fire NpcLootReceived for the whisperer, since they do not hold a reference to the NPC.
            // So, we use LootReceived instead (and return here just in case they change their implementation).
            return;
        }

        this.handleNotify(event.getItems(), npc.getName(), LootRecordType.NPC);
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (WorldUtils.isSafeArea(client))
            return;

        if (config.includePlayerLoot() && isEnabled())
            this.handleNotify(event.getItems(), event.getPlayer().getName(), LootRecordType.PLAYER);
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            if (!config.lootIncludeClueScrolls() && StringUtils.startsWithIgnoreCase(lootReceived.getName(), "Clue Scroll")) {
                // skip clue scroll loot, depending on config
                return;
            }

            String source = killCountService.getStandardizedSource(lootReceived);
            this.handleNotify(lootReceived.getItems(), source, lootReceived.getType());
        } else if (lootReceived.getType() == LootRecordType.NPC && "The Whisperer".equalsIgnoreCase(lootReceived.getName())) {
            // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper, LootRecordType type) {
        final Integer kc = killCountService.getKillCount(type, dropper);
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        JoiningReplacement.JoiningReplacementBuilder lootMessage = JoiningReplacement.builder().delimiter("\n");
        long totalStackValue = 0;
        boolean sendMessage = false;
        SerializedItemStack max = null;
        RareItemStack rarest = null;

        final double rarityThreshold = config.lootRarityThreshold() > 0 ? 1.0 / config.lootRarityThreshold() : Double.NaN;
        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();

            OptionalDouble rarity;
            if (type == LootRecordType.NPC) {
                rarity = rarityService.getRarity(dropper, item.getId(), item.getQuantity());
            } else {
                rarity = OptionalDouble.empty();
            }

            boolean shouldSend;
            if (config.lootRarityValueIntersection() && rarity.isPresent()) {
                shouldSend = totalPrice >= minValue && MathUtils.lessThanOrEqual(rarity.orElse(1), rarityThreshold);
            } else {
                shouldSend = totalPrice >= minValue || MathUtils.lessThanOrEqual(rarity.orElse(1), rarityThreshold);
            }

            shouldSend |= matches(itemNameAllowlist, stack.getName());

            boolean denied = matches(itemNameDenylist, stack.getName());
            if (denied) {
                shouldSend = false;
            }

            if (shouldSend) {
                sendMessage = true;
                lootMessage.component(ItemUtils.templateStack(stack, true));
                if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
            }

            if (max == null || totalPrice > max.getTotalPrice()) {
                max = stack;
            }

            if (rarity.isPresent()) {
                RareItemStack rareStack = RareItemStack.of(stack, rarity.getAsDouble());
                serializedItems.add(rareStack);
                if (!denied && (rarest == null || rareStack.getRarity() < rarest.getRarity())) {
                    rarest = rareStack;
                }
            } else {
                serializedItems.add(stack);
            }
            totalStackValue += totalPrice;
        }

        Evaluable lootMsg;
        if (!sendMessage) {
            if (totalStackValue >= minValue && max != null && "Loot Chest".equalsIgnoreCase(dropper)) {
                // Special case: PK loot keys should trigger notification if total value exceeds configured minimum even
                // if no single item itself would exceed the min value config - github.com/pajlads/DinkPlugin/issues/403
                sendMessage = true;
                lootMsg = Replacements.ofMultiple(" ",
                    Replacements.ofText("Various items including:"),
                    ItemUtils.templateStack(max, true)
                );
            } else {
                lootMsg = null;
            }
        } else {
            lootMsg = lootMessage.build();
        }

        if (sendMessage) {
            String overrideUrl = getWebhookUrl();
            if (config.lootRedirectPlayerKill() && !config.pkWebhook().isBlank()) {
                if (type == LootRecordType.PLAYER || (type == LootRecordType.EVENT && "Loot Chest".equals(dropper))) {
                    overrideUrl = config.pkWebhook();
                }
            }
            SerializedItemStack keyItem = rarest != null ? rarest : max;
            Double rarity = rarest != null ? rarest.getRarity() : null;
            boolean screenshot = config.lootSendImage() && totalStackValue >= config.lootImageMinValue();
            Evaluable source = type == LootRecordType.PLAYER
                ? Replacements.ofLink(dropper, config.playerLookupService().getPlayerUrl(dropper))
                : Replacements.ofWiki(dropper);
            Template notifyMessage = Template.builder()
                .template(config.lootNotifyMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
                .replacement("%LOOT%", lootMsg)
                .replacement("%TOTAL_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(totalStackValue)))
                .replacement("%SOURCE%", source)
                .build();
            createMessage(overrideUrl, screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper, type, kc, rarity))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(keyItem.getId()))
                    .build()
            );
        }
    }

    private static boolean matches(Collection<Pattern> regexps, String input) {
        for (Pattern regex : regexps) {
            if (regex.matcher(input).find())
                return true;
        }
        return false;
    }

}
