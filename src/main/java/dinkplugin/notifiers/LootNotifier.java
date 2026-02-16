package dinkplugin.notifiers;

import dinkplugin.domain.LootCriteria;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Evaluable;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.notifiers.data.AnnotatedItemStack;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.RareItemStack;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import dinkplugin.util.MathUtils;
import dinkplugin.util.ThievingService;
import dinkplugin.util.RarityService;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
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

    @Inject
    private ThievingService thievingService;

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();
    private final Collection<Pattern> itemNameDenylist = new CopyOnWriteArrayList<>();
    private final Collection<String> sourceDenylist = new CopyOnWriteArraySet<>();

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
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );

        itemNameDenylist.clear();
        itemNameDenylist.addAll(
            ConfigUtil.readDelimited(config.lootItemDenylist())
                .map(Utils::regexify)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );

        sourceDenylist.clear();
        sourceDenylist.addAll(
            ConfigUtil.readDelimited(config.lootSourceDenylist())
                .map(String::toLowerCase)
                .collect(Collectors.toList())
        );
    }

    public void onConfigChanged(String key, String value) {
        if ("lootSourceDenylist".equals(key)) {
            sourceDenylist.clear();
            sourceDenylist.addAll(
                ConfigUtil.readDelimited(value)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList())
            );
            return;
        }

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
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
    }

    public void onServerNpcLoot(ServerNpcLoot event) {
        if (!isEnabled()) return;

        var comp = event.getComposition();
        this.handleNotify(event.getItems(), comp.getName(), LootRecordType.NPC, comp.getId());
    }

    public void onNpcLootReceived(NpcLootReceived event) {
        if (!isEnabled()) return;

        NPC npc = event.getNpc();
        int id = npc.getId();
        if (KillCountService.SPECIAL_LOOT_NPC_IDS.contains(id)) {
            // LootReceived is fired for certain NPCs rather than NpcLootReceived, but return here just in case upstream changes their implementation.
            return;
        }

        this.handleNotify(event.getItems(), npc.getName(), LootRecordType.NPC, id);
    }

    public void onPlayerLootReceived(PlayerLootReceived event) {
        if (WorldUtils.isSafeArea(client))
            return;

        if (config.includePlayerLoot() && isEnabled())
            this.handleNotify(event.getItems(), event.getPlayer().getName(), LootRecordType.PLAYER, null);
    }

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            if ("Barbarian Assault high gamble".equals(lootReceived.getName()) && !config.lootIncludeGambles()) {
                // skip ba gambles, depending on config (since we have GambleNotifier)
                return;
            }

            if (!config.lootIncludeClueScrolls() && StringUtils.startsWithIgnoreCase(lootReceived.getName(), "Clue Scroll")) {
                // skip clue scroll loot, depending on config
                return;
            }

            String source = killCountService.getStandardizedSource(lootReceived);
            this.handleNotify(lootReceived.getItems(), source, lootReceived.getType(), null);
        } else if (lootReceived.getType() == LootRecordType.NPC && KillCountService.SPECIAL_LOOT_NPC_NAMES.contains(lootReceived.getName())) {
            // Special case: upstream fires LootReceived for certain NPCs, but not NpcLootReceived
            String source = killCountService.getStandardizedSource(lootReceived);
            var type = KillCountService.THE_GAUNTLET.equals(source) || KillCountService.CG_NAME.equals(source) ? LootRecordType.EVENT : lootReceived.getType();
            this.handleNotify(lootReceived.getItems(), source, type, null);
        }
    }

    public void onGameMessage(String message) {
        if ("You have found a Pharaoh's sceptre! It fell on the floor.".equals(message)) {
            this.handleNotify(List.of(new ItemStack(ItemID.PHARAOHS_SCEPTRE, 1)), "Pyramid Plunder", LootRecordType.EVENT, null);
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper, LootRecordType type, Integer npcId) {
        if (type != LootRecordType.PLAYER && sourceDenylist.contains(dropper.toLowerCase())) {
            log.debug("Skipping loot notif for denied loot source: {} ({})", dropper, type);
            return;
        }

        final Integer kc = killCountService.getKillCount(type, dropper);
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        JoiningReplacement.JoiningReplacementBuilder lootMessage = JoiningReplacement.builder().delimiter("\n");
        long totalStackValue = 0;
        boolean sendMessage = false;
        boolean onAllowList = false;
        SerializedItemStack max = null;
        RareItemStack rarest = null;

        final double rarityThreshold = config.lootRarityThreshold() > 0 ? 1.0 / config.lootRarityThreshold() : Double.NaN;
        final boolean intersection = config.lootRarityValueIntersection() && Double.isFinite(rarityThreshold);
        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();

            OptionalDouble rarity;
            if (type == LootRecordType.NPC) {
                rarity = rarityService.getRarity(dropper, item.getId(), item.getQuantity());
            } else if (type == LootRecordType.PICKPOCKET) {
                rarity = thievingService.getRarity(dropper, item.getId(), item.getQuantity());
            } else {
                rarity = OptionalDouble.empty();
            }

            boolean shouldSend;
            var criteria = EnumSet.noneOf(LootCriteria.class);
            if (totalPrice >= minValue) {
                criteria.add(LootCriteria.VALUE);
            }
            if (MathUtils.lessThanOrEqual(rarity.orElse(1), rarityThreshold)) {
                criteria.add(LootCriteria.RARITY);
            }
            if (intersection) {
                shouldSend = criteria.contains(LootCriteria.VALUE) && (rarity.isEmpty() || criteria.contains(LootCriteria.RARITY));
            } else {
                shouldSend = criteria.contains(LootCriteria.VALUE) || criteria.contains(LootCriteria.RARITY);
            }

            boolean denied = matches(itemNameDenylist, stack.getName());
            if (denied) {
                shouldSend = false;
                criteria.add(LootCriteria.DENYLIST);
            } else {
                if (matches(itemNameAllowlist, stack.getName())) {
                    shouldSend = true;
                    onAllowList = true;
                    criteria.add(LootCriteria.ALLOWLIST);
                }
                if (max == null || totalPrice > max.getTotalPrice()) {
                    max = stack;
                }
            }

            if (shouldSend) {
                sendMessage = true;
                lootMessage.component(ItemUtils.templateStack(stack, true));
                if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
            }

            var annotated = AnnotatedItemStack.of(stack, criteria);
            if (rarity.isPresent()) {
                RareItemStack rareStack = RareItemStack.of(annotated, rarity.getAsDouble());
                serializedItems.add(rareStack);
                if (!denied && (rarest == null || rareStack.getRarity() < rarest.getRarity())) {
                    rarest = rareStack;
                }
            } else {
                serializedItems.add(annotated);
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
            if (npcId == null && (type == LootRecordType.NPC || type == LootRecordType.PICKPOCKET)) {
                npcId = client.getTopLevelWorldView().npcs().stream()
                    .filter(npc -> dropper.equals(npc.getName()))
                    .findAny()
                    .map(NPC::getId)
                    .orElse(null);
            }

            String overrideUrl = getWebhookUrl();
            if (config.lootRedirectPlayerKill() && !config.pkWebhook().isBlank()) {
                if (type == LootRecordType.PLAYER || (type == LootRecordType.EVENT && "Loot Chest".equals(dropper))) {
                    overrideUrl = config.pkWebhook();
                }
            }
            Double rarity = rarest != null ? rarest.getRarity() : null;
            boolean screenshot = config.lootSendImage() && (totalStackValue >= config.lootImageMinValue() || onAllowList);
            Collection<String> party = type == LootRecordType.EVENT ? Utils.getBossParty(client, dropper) : null;
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
                .replacement("%COUNT%", Replacements.ofText(kc != null ? kc.toString() : "unknown"))
                .build();
            createMessage(overrideUrl, screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper, type, kc, rarity, party, npcId))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(max.getId()))
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
