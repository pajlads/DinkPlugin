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
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class LootNotifier extends BaseNotifier {

    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private Gson gson;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ConfigManager configManager;

    private final Set<String> itemLowerNameAllowlist = new HashSet<>();
    private final Set<String> itemLowerNameDenylist = new HashSet<>();

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
        synchronized (itemLowerNameAllowlist) {
            itemLowerNameAllowlist.clear();
            ConfigUtil.readDelimited(config.lootItemAllowlist())
                .map(String::toLowerCase)
                .forEach(itemLowerNameAllowlist::add);
        }

        synchronized (itemLowerNameDenylist) {
            itemLowerNameDenylist.clear();
            ConfigUtil.readDelimited(config.lootItemDenylist())
                .map(String::toLowerCase)
                .forEach(itemLowerNameDenylist::add);
        }
    }

    public void onConfigChanged(String key, String value) {
        Collection<String> itemNames;
        if ("lootItemAllowlist".equals(key)) {
            itemNames = itemLowerNameAllowlist;
        } else if ("lootItemDenylist".equals(key)) {
            itemNames = itemLowerNameDenylist;
        } else {
            return;
        }

        synchronized (itemNames) {
            itemNames.clear();
            ConfigUtil.readDelimited(value)
                .map(String::toLowerCase)
                .forEach(itemNames::add);
        }

        log.debug("{} is now {}", key, itemNames);
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
        final Integer kc = type == LootRecordType.NPC ? incrementAndGetKillCount(dropper) : null;
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
            String lowerName = stack.getName().toLowerCase();
            boolean worthy = totalPrice >= minValue || itemLowerNameAllowlist.contains(lowerName);
            if (worthy && !itemLowerNameDenylist.contains(lowerName)) {
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

    private Integer incrementAndGetKillCount(String npcName) {
        Integer stored = getStoredKillCount(npcName);
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
     * @return the kill count stored by the base runelite loot tracker plugin
     */
    private Integer getStoredKillCount(String npcName) {
        if ("false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, RL_LOOT_PLUGIN_NAME))) {
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

}
