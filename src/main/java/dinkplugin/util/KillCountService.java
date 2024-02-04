package dinkplugin.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.notifiers.KillCountNotifier;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.http.api.loottracker.LootRecordType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class KillCountService {

    private static final String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Inject
    private ScheduledExecutorService executor;

    private final Cache<String, Integer> killCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

    public void reset() {
        this.killCounts.invalidateAll();
    }

    public void onNpcKill(NpcLootReceived event) {
        NPC npc = event.getNpc();
        int id = npc.getId();
        if (id == NpcID.THE_WHISPERER || id == NpcID.THE_WHISPERER_12205 || id == NpcID.THE_WHISPERER_12206 || id == NpcID.THE_WHISPERER_12207) {
            // Upstream does not fire NpcLootReceived for the whisperer, since they do not hold a reference to the NPC.
            // So, we use LootReceived instead (and return here just in case they change their implementation).
            return;
        }

        String name = npc.getName();
        if (name != null) {
            this.incrementKillCount(LootRecordType.NPC, name);
        }
    }

    public void onLoot(LootReceived event) {
        boolean increment;
        if (event.getType() == LootRecordType.NPC && "The Whisperer".equalsIgnoreCase(event.getName())) {
            // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
            increment = true;
        } else {
            increment = event.getType() == LootRecordType.EVENT;
        }

        if (increment) {
            this.incrementKillCount(event.getType(), event.getName());
        }
    }

    public void onGameMessage(String message) {
        // update cached KC via boss chat message with robustness for chat event coming before OR after the loot event
        KillCountNotifier.parseBoss(message).ifPresent(pair -> {
            String boss = pair.getKey();
            Integer kc = pair.getValue();

            // Update cache. We store kc - 1 since onNpcLootReceived will increment; kc - 1 + 1 == kc
            killCounts.asMap().merge(boss, kc - 1, Math::max);

            // However: we don't know if boss message appeared before/after the loot event.
            // If after, we should store kc. If before, we should store kc - 1.
            // Given this uncertainty, we wait so that the loot event has passed, and then we can store latest kc.
            executor.schedule(() -> {
                killCounts.asMap().merge(boss, kc, Math::max);
            }, 15, TimeUnit.SECONDS);
        });
    }

    @Nullable
    public Integer getKillCount(LootRecordType type, String sourceName) {
        if (sourceName == null || (type != LootRecordType.NPC && type != LootRecordType.EVENT)) {
            return null;
        }
        Integer stored = getStoredKillCount(type, sourceName);
        if (stored != null) {
            return killCounts.asMap().merge(sourceName, stored, Math::max);
        }
        return killCounts.getIfPresent(sourceName);
    }

    private void incrementKillCount(@NotNull LootRecordType type, @NotNull String sourceName) {
        killCounts.asMap().compute(sourceName, (name, cachedKc) -> {
            if (cachedKc != null) {
                // increment kill count
                return cachedKc + 1;
            } else {
                // pull kc from loot tracker or chat commands plugin
                Integer kc = getStoredKillCount(type, name);
                // increment if found
                return kc != null ? kc + 1 : null;
            }
        });
    }

    /**
     * @param type       {@link LootReceived#getType()}
     * @param sourceName {@link NPC#getName()} or {@link LootReceived#getName()}
     * @return the kill count stored by base runelite plugins
     */
    @Nullable
    private Integer getStoredKillCount(@NotNull LootRecordType type, @NotNull String sourceName) {
        assert type == LootRecordType.NPC || type == LootRecordType.EVENT;

        // get kc from base runelite chat commands plugin (if enabled)
        if (!ConfigUtil.isPluginDisabled(configManager, RL_CHAT_CMD_PLUGIN_NAME)) {
            String boss = sourceName.startsWith("Barrows") ? "barrows chests" : StringUtils.remove(sourceName.toLowerCase(), ':');
            Integer kc = configManager.getRSProfileConfiguration("killcount", boss, int.class);
            if (kc != null) {
                return kc - 1; // decremented since chat event typically occurs before loot event
            }
        }

        if (type != LootRecordType.NPC || ConfigUtil.isPluginDisabled(configManager, RL_LOOT_PLUGIN_NAME)) {
            // assume stored kc is useless if loot tracker plugin is disabled
            return null;
        }
        String json = configManager.getConfiguration(LootTrackerConfig.GROUP,
            configManager.getRSProfileKey(),
            "drops_NPC_" + sourceName
        );
        if (json == null) {
            // no kc stored implies first kill
            return 0;
        }
        try {
            return gson.fromJson(json, SerializedLoot.class).getKills();
        } catch (JsonSyntaxException e) {
            // should not occur unless loot tracker changes stored loot POJO structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

}
