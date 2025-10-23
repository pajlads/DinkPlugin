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
import dinkplugin.util.*;
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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BingoNotifier extends BaseNotifier {

    @Inject
    private ItemManager itemManager;

    @Inject
    private KillCountService killCountService;

    @Inject
    private RarityService rarityService;

    @Inject
    private ThievingService thievingService;

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();

    @Override
    public boolean isEnabled() {
        return config.notifyBingo() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.bingoWebhook();
    }

    public void init() {
        itemNameAllowlist.clear();
        itemNameAllowlist.addAll(
            ConfigUtil.readDelimited(config.bingoItemAllowlist())
                .map(Utils::regexify)
                .collect(Collectors.toList())
        );
    }

    public void onConfigChanged(String key, String value) {
        Collection<Pattern> itemNames;
        if ("bingoItemAllowlist".equals(key)) {
            itemNames = itemNameAllowlist;
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

    public void onLootReceived(LootReceived lootReceived) {
        if (!isEnabled()) return;

        // ignore PK chest
        if (lootReceived.getType() == LootRecordType.EVENT && "Loot Chest".equals(killCountService.getStandardizedSource(lootReceived))) {
            return;
        }

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
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
        final Integer kc = killCountService.getKillCount(type, dropper);

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());

        JoiningReplacement.JoiningReplacementBuilder lootMessage = JoiningReplacement.builder().delimiter("\n");
        long totalStackValue = 0;
        boolean sendMessage = false;
        SerializedItemStack max = null;
        RareItemStack rarest = null;

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



            if (matches(itemNameAllowlist, stack.getName())) {
                shouldSend = true;
                criteria.add(LootCriteria.ALLOWLIST);
            } else {
                shouldSend = false;
            }
            if (max == null || totalPrice > max.getTotalPrice()) {
                max = stack;
            }

            if (shouldSend) {
                sendMessage = true;
                lootMessage.component(ItemUtils.templateStack(stack, true));
            }

            var annotated = AnnotatedItemStack.of(stack, criteria);
            if (rarity.isPresent()) {
                RareItemStack rareStack = RareItemStack.of(annotated, rarity.getAsDouble());
                serializedItems.add(rareStack);
                if ((rarest == null || rareStack.getRarity() < rarest.getRarity())) {
                    rarest = rareStack;
                }
            } else {
                serializedItems.add(annotated);
            }
            totalStackValue += totalPrice;
        }

        Evaluable lootMsg = lootMessage.build();

        if (sendMessage) {
            if (npcId == null && (type == LootRecordType.NPC || type == LootRecordType.PICKPOCKET)) {
                npcId = client.getTopLevelWorldView().npcs().stream()
                    .filter(npc -> dropper.equals(npc.getName()))
                    .findAny()
                    .map(NPC::getId)
                    .orElse(null);
            }

            String overrideUrl = getWebhookUrl();

            Double rarity = rarest != null ? rarest.getRarity() : null;
            boolean screenshot = config.bingoSendImage();
            Collection<String> party = type == LootRecordType.EVENT ? Utils.getBossParty(client, dropper) : null;
            Evaluable source = Replacements.ofWiki(dropper);
            Template notifyMessage = Template.builder()
                .template(config.bingoNotifyMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
                .replacement("%LOOT%", lootMsg)
                .replacement("%TOTAL_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(totalStackValue)))
                .replacement("%SOURCE%", source)
                .build();
            createMessage(overrideUrl, screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .embeds(new ArrayList<>(0))
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
