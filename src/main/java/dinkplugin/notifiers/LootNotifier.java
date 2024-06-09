package dinkplugin.notifiers;

import com.google.common.math.DoubleMath;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

            String source = killCountService.isCorruptedGauntlet(lootReceived) ? KillCountService.CG_NAME : lootReceived.getName();
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
        Collection<Integer> deniedIds = new HashSet<>(reduced.size());

        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();
            boolean worthy = totalPrice >= minValue || matches(itemNameAllowlist, stack.getName());
            if (!matches(itemNameDenylist, stack.getName())) {
                if (worthy) {
                    sendMessage = true;
                    lootMessage.component(ItemUtils.templateStack(stack, true));
                    if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
                }
                if (max == null || totalPrice > max.getTotalPrice()) {
                    max = stack;
                }
            } else {
                deniedIds.add(item.getId());
            }
            serializedItems.add(stack);
            totalStackValue += totalPrice;
        }

        var rarityResult = getItemRarities(type, dropper, serializedItems, deniedIds);
        Collection<SerializedItemStack> augmentedItems = rarityResult.getKey();
        RareItemStack rarest = rarityResult.getValue().orElse(null);

        Evaluable lootMsg;
        if (!sendMessage) {
            if (sufficientlyRare(rarest)) {
                // allow notifications for rare drops, even if below configured min loot value
                sendMessage = true;
                lootMsg = Replacements.ofMultiple(" ",
                    Replacements.ofText("Various items including:"),
                    ItemUtils.templateStack(rarest, false)
                );
            } else if (totalStackValue >= minValue && max != null && "Loot Chest".equalsIgnoreCase(dropper)) {
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
                    .extra(new LootNotificationData(augmentedItems, dropper, type, kc, rarity))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(keyItem.getId()))
                    .build()
            );
        }
    }

    /**
     * Converts {@link SerializedItemStack} loot to {@link RareItemStack}
     *
     * @param type the type of loot source
     * @param source the name of the loot source
     * @param reduced the looted items (after {@link ItemUtils#reduceItemStack(Iterable)} was performed)
     * @param rarestDeniedIds the item IDs that should be ignored when finding the rarest drop
     * @return the dropped items augmented with rarity information (as available from the NPC drop table), and the single rarest item
     */
    @NotNull
    private Map.Entry<Collection<SerializedItemStack>, Optional<RareItemStack>> getItemRarities(LootRecordType type, String source, Collection<SerializedItemStack> reduced, Collection<Integer> rarestDeniedIds) {
        if (type != LootRecordType.NPC) {
            return Map.entry(reduced, Optional.empty());
        }

        RareItemStack rarest = null;
        List<SerializedItemStack> transformed = new ArrayList<>(reduced.size());
        for (SerializedItemStack item : reduced) {
            final int id = item.getId();
            OptionalDouble rarity = rarityService.getRarity(source, id, item.getQuantity());
            if (rarity.isPresent()) {
                RareItemStack rareItem = RareItemStack.of(item, rarity.getAsDouble());
                transformed.add(rareItem);

                // check if this stack is the rarest so far
                if ((rarest == null || rareItem.getRarity() < rarest.getRarity()) && !rarestDeniedIds.contains(id)) {
                    rarest = rareItem;
                }
            } else {
                transformed.add(item);
            }
        }
        return Map.entry(transformed, Optional.ofNullable(rarest));
    }

    private boolean sufficientlyRare(@Nullable RareItemStack rarest) {
        if (rarest == null) return false;
        int configRareDenominator = config.lootRarityThreshold();
        if (configRareDenominator <= 0) return false;
        double rarityThreshold = 1.0 / configRareDenominator;
        return DoubleMath.fuzzyCompare(rarest.getRarity(), rarityThreshold, MathUtils.EPSILON) <= 0;
    }

    private static boolean matches(Collection<Pattern> regexps, String input) {
        for (Pattern regex : regexps) {
            if (regex.matcher(input).find())
                return true;
        }
        return false;
    }

}
