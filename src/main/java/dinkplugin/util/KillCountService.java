package dinkplugin.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.KillCountNotifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class KillCountService {

    public static final String GAUNTLET_NAME = "Gauntlet", GAUNTLET_BOSS = "Crystalline Hunllef";
    public static final String CG_NAME = "Corrupted Gauntlet", CG_BOSS = "Corrupted Hunllef";

    private static final String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private RarityService rarityService;

    private final Cache<String, Integer> killCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(64L)
        .build();

    @Getter
    @Nullable
    private Drop lastDrop = null;

    public void reset() {
        this.lastDrop = null;
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
        if (GAUNTLET_BOSS.equals(name) || CG_BOSS.equals(name)) {
            // already handled by onGameMessage
            return;
        }
        if (name != null) {
            this.incrementKills(LootRecordType.NPC, name, event.getItems());
        }
    }

    public void onPlayerKill(PlayerLootReceived event) {
        String name = event.getPlayer().getName();
        if (name != null) {
            this.incrementKills(LootRecordType.PLAYER, name, event.getItems());
        }
    }

    public void onLoot(LootReceived event) {
        boolean increment;
        switch (event.getType()) {
            case NPC:
                // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
                increment = "The Whisperer".equalsIgnoreCase(event.getName());
                break;
            case PLAYER:
                increment = false; // handled by PlayerLootReceived
                break;
            default:
                increment = true;
                break;
        }

        if (increment) {
            String source = isCorruptedGauntlet(event) ? CG_NAME : event.getName();
            this.incrementKills(event.getType(), source, event.getItems());
        }
    }

    public void onGameMessage(String message) {
        // update cached clue casket count
        Map.Entry<String, Integer> clue = ClueNotifier.parse(message);
        if (clue != null) {
            String tier = Utils.ucFirst(clue.getKey());
            int count = clue.getValue() - 1; // decremented since onLoot will increment
            killCounts.put("Clue Scroll (" + tier + ")", count);
            return;
        }

        // update cached KC via boss chat message with robustness for chat event coming before OR after the loot event
        KillCountNotifier.parseBoss(message).ifPresent(pair -> {
            String boss = pair.getKey();
            Integer kc = pair.getValue();

            // Update cache. We store kc - 1 since onNpcLootReceived will increment; kc - 1 + 1 == kc
            String cacheKey = getCacheKey(LootRecordType.UNKNOWN, boss);
            killCounts.asMap().merge(cacheKey, kc - 1, Math::max);

            if (boss.equals(GAUNTLET_BOSS) || boss.equals(CG_BOSS)) {
                // populate lastDrop for isCorruptedGauntlet to function
                this.lastDrop = new Drop(boss, LootRecordType.EVENT, Collections.emptyList());

                if (!ConfigUtil.isPluginDisabled(configManager, RL_LOOT_PLUGIN_NAME)) {
                    // onLoot will already increment kc, no need to schedule task below.
                    // this early return also simplifies our test code
                    return;
                }
            }

            // However: we don't know if boss message appeared before/after the loot event.
            // If after, we should store kc. If before, we should store kc - 1.
            // Given this uncertainty, we wait so that the loot event has passed, and then we can store latest kc.
            executor.schedule(() -> {
                killCounts.asMap().merge(cacheKey, kc, Math::max);
            }, 15, TimeUnit.SECONDS);
        });
    }

    /**
     * @param event a loot received event that was just fired
     * @return whether the event represents corrupted gauntlet
     * @apiNote Useful to distinguish normal vs. corrupted gauntlet since the base loot tracker plugin does not,
     * which was <a href="https://github.com/pajlads/DinkPlugin/issues/469">reported</a> to our issue tracker.
     */
    public boolean isCorruptedGauntlet(@NotNull LootReceived event) {
        return event.getType() == LootRecordType.EVENT && lastDrop != null && "The Gauntlet".equals(event.getName())
            && (CG_NAME.equals(lastDrop.getSource()) || CG_BOSS.equals(lastDrop.getSource()));
    }

    @Nullable
    public Integer getKillCount(LootRecordType type, String sourceName) {
        if (sourceName == null) return null;
        Integer stored = getStoredKillCount(type, sourceName);
        if (stored != null) {
            return killCounts.asMap().merge(getCacheKey(type, sourceName), stored, Math::max);
        }
        return killCounts.getIfPresent(getCacheKey(type, sourceName));
    }

    private void incrementKills(@NotNull LootRecordType type, @NotNull String sourceName, @NotNull Collection<ItemStack> items) {
        String cacheKey = getCacheKey(type, sourceName);
        killCounts.asMap().compute(cacheKey, (key, cachedKc) -> {
            if (cachedKc != null) {
                // increment kill count
                return cachedKc + 1;
            } else {
                // pull kc from loot tracker or chat commands plugin
                Integer kc = getStoredKillCount(type, sourceName);
                // increment if found
                return kc != null ? kc + 1 : null;
            }
        });
        this.lastDrop = new Drop(sourceName, type, items);
    }

    /**
     * @param type       {@link LootReceived#getType()}
     * @param sourceName {@link NPC#getName()} or {@link LootReceived#getName()}
     * @return the kill count stored by base runelite plugins
     */
    @Nullable
    private Integer getStoredKillCount(@NotNull LootRecordType type, @NotNull String sourceName) {
        // get kc from base runelite chat commands plugin (if enabled)
        if (!ConfigUtil.isPluginDisabled(configManager, RL_CHAT_CMD_PLUGIN_NAME)) {
            Integer kc = configManager.getRSProfileConfiguration("killcount", cleanBossName(sourceName), int.class);
            if (kc != null) {
                return kc - 1; // decremented since chat event typically occurs before loot event
            }
        }

        if (ConfigUtil.isPluginDisabled(configManager, RL_LOOT_PLUGIN_NAME)) {
            // assume stored kc is useless if loot tracker plugin is disabled
            return null;
        }
        String json = configManager.getConfiguration(LootTrackerConfig.GROUP,
            configManager.getRSProfileKey(),
            "drops_" + type + "_" + sourceName
        );
        if (json == null) {
            // no kc stored implies first kill
            return 0;
        }
        try {
            int kc = gson.fromJson(json, SerializedLoot.class).getKills();

            // loot tracker doesn't count kill if no loot - https://github.com/runelite/runelite/issues/5077
            OptionalDouble nothingProbability = rarityService.getRarity(sourceName, -1, 0);
            if (nothingProbability.isPresent() && nothingProbability.getAsDouble() < 1.0) {
                // estimate the actual kc (including kills with no loot)
                kc = (int) Math.round(kc / (1 - nothingProbability.getAsDouble()));
            }

            return kc;
        } catch (JsonSyntaxException e) {
            // should not occur unless loot tracker changes stored loot POJO structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

    /**
     * @param boss {@link LootReceived#getName()}
     * @return lowercase boss name that {@link ChatCommandsPlugin} uses during serialization
     */
    private static String cleanBossName(String boss) {
        if ("The Gauntlet".equalsIgnoreCase(boss)) return "gauntlet";
        if ("The Leviathan".equalsIgnoreCase(boss)) return "leviathan";
        if ("The Whisperer".equalsIgnoreCase(boss)) return "whisperer";
        if (boss.startsWith("Barrows")) return "barrows chests";
        if (boss.endsWith("Hallowed Sepulchre)")) return "hallowed sepulchre";
        if (boss.endsWith("Tempoross)")) return "tempoross";
        if (boss.endsWith("Wintertodt)")) return "wintertodt";
        return StringUtils.remove(boss.toLowerCase(), ':');
    }

    private static String getCacheKey(@NotNull LootRecordType type, @NotNull String sourceName) {
        switch (type) {
            case PICKPOCKET:
                return "pickpocket_" + sourceName;
            case PLAYER:
                return "player_" + sourceName;
            default:
                if ("The Gauntlet".equals(sourceName)) return GAUNTLET_BOSS;
                if (CG_NAME.equals(sourceName)) return CG_BOSS;
                return sourceName;
        }
    }

}
