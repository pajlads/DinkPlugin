package dinkplugin.notifiers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.SerializedLoot;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LootNotifier extends BaseNotifier {

    private static final String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private Gson gson;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ConfigManager configManager;

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();
    private final Collection<Pattern> itemNameDenylist = new CopyOnWriteArrayList<>();

    private final Cache<String, Integer> killCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    @Override
    public boolean isEnabled() {
        return config.notifyLoot() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.lootWebhook();
    }

    public void reset() {
        killCounts.invalidateAll();
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
        if (!isEnabled()) {
            // increment kill count
            killCounts.asMap().computeIfPresent(event.getNpc().getName(), (k, v) -> v + 1);
            return;
        }

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

            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        } else if (lootReceived.getType() == LootRecordType.NPC && "The Whisperer".equalsIgnoreCase(lootReceived.getName())) {
            // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
            this.handleNotify(lootReceived.getItems(), lootReceived.getName(), lootReceived.getType());
        }
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (!isEnabled()) return;

        // special case: runelite client & loot tracker do not handle unsired loot at the time of writing
        if (event.getGroupId() == WidgetID.DIALOG_SPRITE_GROUP_ID) {
            clientThread.invokeAtTickEnd(() -> {
                Widget textWidget = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);
                if (textWidget != null && StringUtils.containsIgnoreCase(textWidget.getText(), "The Font consumes the Unsired")) {
                    Widget spriteWidget = firstWithItem(WidgetInfo.DIALOG_SPRITE, WidgetInfo.DIALOG_SPRITE_SPRITE, WidgetInfo.DIALOG_SPRITE_TEXT);
                    if (hasItem(spriteWidget)) {
                        ItemStack item = new ItemStack(
                            spriteWidget.getItemId(),
                            1,
                            client.getLocalPlayer().getLocalLocation()
                        );
                        this.handleNotify(Collections.singletonList(item), "The Font of Consumption", LootRecordType.EVENT);
                    } else {
                        Widget widget = client.getWidget(WidgetInfo.DIALOG_SPRITE);
                        log.warn(
                            "Failed to locate widget with item for Unsired loot. Children: {} - Nested: {} - Sprite: {} - Model: {}",
                            widget != null && widget.getDynamicChildren() != null ? widget.getDynamicChildren().length : -1,
                            widget != null && widget.getNestedChildren() != null ? widget.getNestedChildren().length : -1,
                            widget != null ? widget.getSpriteId() : -1,
                            widget != null ? widget.getModelId() : -1
                        );
                    }
                }
            });
        }
    }

    private void handleNotify(Collection<ItemStack> items, String dropper, LootRecordType type) {
        final Integer kc = incrementAndGetKillCount(dropper, type);
        final int minValue = config.minLootValue();
        final boolean icons = config.lootIcons();

        Collection<ItemStack> reduced = ItemUtils.reduceItemStack(items);
        List<SerializedItemStack> serializedItems = new ArrayList<>(reduced.size());
        List<Embed> embeds = new ArrayList<>(icons ? reduced.size() : 0);

        JoiningReplacement.JoiningReplacementBuilder lootMessage = JoiningReplacement.builder().delimiter("\n");
        long totalStackValue = 0;
        boolean sendMessage = false;
        SerializedItemStack max = null;

        for (ItemStack item : reduced) {
            SerializedItemStack stack = ItemUtils.stackFromItem(itemManager, item.getId(), item.getQuantity());
            long totalPrice = stack.getTotalPrice();
            boolean worthy = totalPrice >= minValue || matches(itemNameAllowlist, stack.getName());
            if (worthy && !matches(itemNameDenylist, stack.getName())) {
                sendMessage = true;
                lootMessage.component(ItemUtils.templateStack(stack, true));
                if (icons) embeds.add(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId())));
                if (max == null || totalPrice > max.getTotalPrice()) {
                    max = stack;
                }
            }
            serializedItems.add(stack);
            totalStackValue += totalPrice;
        }

        if (sendMessage) {
            boolean screenshot = config.lootSendImage() && totalStackValue >= config.lootImageMinValue();
            Template notifyMessage = Template.builder()
                .template(config.lootNotifyMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
                .replacement("%LOOT%", lootMessage.build())
                .replacement("%TOTAL_VALUE%", Replacements.ofText(QuantityFormatter.quantityToStackSize(totalStackValue)))
                .replacement("%SOURCE%", Replacements.ofText(dropper))
                .build();
            createMessage(screenshot,
                NotificationBody.builder()
                    .text(notifyMessage)
                    .embeds(embeds)
                    .extra(new LootNotificationData(serializedItems, dropper, type, kc))
                    .type(NotificationType.LOOT)
                    .thumbnailUrl(ItemUtils.getItemImageUrl(max.getId()))
                    .build()
            );
        }
    }

    private Integer incrementAndGetKillCount(String npcName, LootRecordType type) {
        if (type != LootRecordType.NPC && type != LootRecordType.EVENT) return null;
        Integer stored = getStoredKillCount(npcName, type);
        if (stored == null) {
            killCounts.asMap().computeIfPresent(npcName, (k, v) -> v + 1);
            return null;
        }
        return killCounts.asMap().compute(npcName, (npc, cached) -> {
            if (cached == null || cached < stored) {
                return stored + 1;
            }
            return cached + 1;
        });
    }

    /**
     * @param npcName {@link NPC#getName()}
     * @param type {@link LootReceived#getType()}
     * @return the kill count stored by the base runelite chat commands or loot tracker plugin
     */
    private Integer getStoredKillCount(String npcName, LootRecordType type) {
        // Get kill count from base Chat Commands plugin, if enabled
        if (!ConfigUtil.isPluginDisabled(configManager, RL_CHAT_CMD_PLUGIN_NAME)) {
            String boss = "Barrows".equals(npcName) ? "barrows chests"
                : StringUtils.remove(npcName.toLowerCase(), ':');
            Integer kc = configManager.getRSProfileConfiguration("killcount", boss, int.class);
            if (kc != null) {
                // chat event typically occurs before loot event
                return kc - 1;
            }
        }

        if (type != LootRecordType.NPC || ConfigUtil.isPluginDisabled(configManager, RL_LOOT_PLUGIN_NAME)) {
            // assume stored kc is useless if loot tracker plugin is disabled
            return null;
        }
        String json = configManager.getConfiguration(LootTrackerConfig.GROUP,
            configManager.getRSProfileKey(),
            "drops_NPC_" + npcName
        );
        if (json == null) {
            // no kc stored implies first kill
            return 0;
        }
        try {
            return gson.fromJson(json, SerializedLoot.class).getKills();
        } catch (JsonSyntaxException e) {
            // should not occur unless loot tracker changes stored loot pojo structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

    private Widget firstWithItem(WidgetInfo... widgets) {
        for (WidgetInfo info : widgets) {
            Widget widget = client.getWidget(info);
            if (hasItem(widget)) {
                log.debug("Obtained item from widget via {}", info);
                return widget;
            }
        }
        return null;
    }

    private static boolean hasItem(Widget widget) {
        return widget != null && widget.getItemId() >= 0;
    }

    private static boolean matches(Collection<Pattern> regexps, String input) {
        for (Pattern regex : regexps) {
            if (regex.matcher(input).find())
                return true;
        }
        return false;
    }

}
