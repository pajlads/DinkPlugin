package dinkplugin.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.KillCountNotifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
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
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Slf4j
@Singleton
public class KillCountService {

    public static final String GAUNTLET_NAME = "Gauntlet", GAUNTLET_BOSS = "Crystalline Hunllef";
    public static final String CG_NAME = "Corrupted Gauntlet", CG_BOSS = "Corrupted Hunllef";
    public static final String HERBIBOAR = "Herbiboar";
    public static final String TOA = "Tombs of Amascut";
    public static final String TOB = "Theatre of Blood";
    public static final String COX = "Chambers of Xeric";

    private static final String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
    private static final String RL_LOOT_PLUGIN_NAME = LootTrackerPlugin.class.getSimpleName().toLowerCase();
    private static final String RIFT_PREFIX = "Amount of rifts you have closed: ";
    private static final String HERBIBOAR_PREFIX = "Your herbiboar harvest count is: ";

    public static final Set<Integer> SPECIAL_LOOT_NPC_IDS = Set.of(
        NpcID.WHISPERER, NpcID.WHISPERER_MELEE, NpcID.WHISPERER_QUEST, NpcID.WHISPERER_MELEE_QUEST,
        NpcID.ARAXXOR, NpcID.ARAXXOR_DEAD, NpcID.RT_FIRE_QUEEN_INACTIVE, NpcID.RT_ICE_KING_INACTIVE
    );
    public static final Set<String> SPECIAL_LOOT_NPC_NAMES = Set.of("The Whisperer", "Araxxor",
        "Branda the Fire Queen", "Eldric the Ice King", "Crystalline Hunllef", "Corrupted Hunllef");

    @Inject
    private ConfigManager configManager;

    @Inject
    private DinkPluginConfig config;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

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

    public void onServerNpcLoot(ServerNpcLoot event) {
        this.incrementKills(LootRecordType.NPC, event.getComposition().getName(), event.getItems());
    }

    public void onNpcKill(NpcLootReceived event) {
        NPC npc = event.getNpc();
        int id = npc.getId();
        if (SPECIAL_LOOT_NPC_IDS.contains(id)) {
            // LootReceived is fired for certain NPCs rather than NpcLootReceived, but return here just in case upstream changes their implementation.
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
                // Special case: upstream fires LootReceived for certain NPCs, but not NpcLootReceived
                increment = SPECIAL_LOOT_NPC_NAMES.contains(event.getName());
                break;
            case PLAYER:
                increment = false; // handled by PlayerLootReceived
                break;
            default:
                increment = true;
                break;
        }

        if (increment) {
            this.incrementKills(event.getType(), getStandardizedSource(event), event.getItems());
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

        // guardians of the rift count (for pet tracking)
        if (message.startsWith(RIFT_PREFIX)) {
            int riftCount = Integer.parseInt(message.substring(RIFT_PREFIX.length(), message.length() - 1).replace(",", ""));
            killCounts.put("Guardians of the Rift", riftCount);
            return;
        }

        // herbiboar count (for pet tracking)
        if (message.startsWith(HERBIBOAR_PREFIX)) {
            int harvestCount = Integer.parseInt(message.substring(HERBIBOAR_PREFIX.length(), message.length() - 1).replace(",", ""));
            killCounts.put(HERBIBOAR, harvestCount);
            return;
        }

        // update cached KC via boss chat message with robustness for chat event coming before OR after the loot event
        KillCountNotifier.parseBoss(message).ifPresent(pair -> {
            String boss = pair.getKey();
            Integer kc = pair.getValue();

            // Update cache. We store kc - 1 since onNpcLootReceived will increment; kc - 1 + 1 == kc
            String cacheKey = getCacheKey(LootRecordType.UNKNOWN, boss);
            killCounts.asMap().merge(cacheKey, kc - 1, Math::max);

            if (boss.equals("Araxxor") || boss.equals(GAUNTLET_BOSS) || boss.equals(CG_BOSS) || boss.startsWith(TOA) || boss.startsWith(TOB) || boss.startsWith(COX)) {
                // populate lastDrop to workaround loot tracking quirks
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

    public void onWidget(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.KILL_LOG && config.useSlayerWidgetKc()) {
            clientThread.invokeAtTickEnd(this::handleSlayerLog);
        }
    }

    public String getStandardizedSource(LootReceived event) {
        if (isCorruptedGauntlet(event)) {
            return KillCountService.CG_NAME;
        } else if (lastDrop != null && shouldUseChatName(event)) {
            return lastDrop.getSource(); // distinguish entry/expert/challenge modes
        }
        return event.getName();
    }

    private boolean shouldUseChatName(LootReceived event) {
        assert lastDrop != null;
        String lastSource = lastDrop.getSource();
        Predicate<String> coincides = source -> source.equals(event.getName()) && lastSource.startsWith(source);
        return coincides.test(TOA) || coincides.test(TOB) || coincides.test(COX);
    }

    /**
     * @param event a loot received event that was just fired
     * @return whether the event represents corrupted gauntlet
     * @apiNote Useful to distinguish normal vs. corrupted gauntlet since the base loot tracker plugin does not,
     * which was <a href="https://github.com/pajlads/DinkPlugin/issues/469">reported</a> to our issue tracker.
     */
    private boolean isCorruptedGauntlet(LootReceived event) {
        return event.getType() == LootRecordType.EVENT && lastDrop != null && "The Gauntlet".equals(event.getName())
            && (CG_NAME.equals(lastDrop.getSource()) || CG_BOSS.equals(lastDrop.getSource()));
    }

    @Nullable
    public Duration getPb(String boss) {
        if (ConfigUtil.isPluginDisabled(configManager, RL_CHAT_CMD_PLUGIN_NAME)) return null;
        Double pb = configManager.getRSProfileConfiguration("personalbest", cleanBossName(boss), double.class);
        if (pb == null) return null;
        int seconds = pb.intValue();
        double millis = (pb - seconds) * 1000;
        return Duration.ofSeconds(seconds).plusMillis((long) millis);
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
        Integer newKc = killCounts.asMap().compute(cacheKey, (key, cachedKc) -> {
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

        if (newKc != null && type == LootRecordType.NPC && getSlayerKc(sourceName) != null) {
            setSlayerKc(sourceName, newKc);
        }
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

        Integer slayerKc = type == LootRecordType.NPC ? getSlayerKc(sourceName) : null;
        SerializedLoot lootRecord = getLootTrackerRecord(type, sourceName);
        if (lootRecord != null) {
            if (slayerKc != null) {
                return Math.max(lootRecord.getKills(), slayerKc);
            }
            return lootRecord.getKills();
        }
        return slayerKc;
    }

    @Nullable
    public SerializedLoot getLootTrackerRecord(@NotNull LootRecordType type, @NotNull String sourceName) {
        if (type == LootRecordType.EVENT && "Pyramid Plunder".equals(sourceName)) {
            // ignore events that are not recorded by the base loot tracker
            return null;
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
            return new SerializedLoot();
        }
        try {
            SerializedLoot lootRecord = gson.fromJson(json, SerializedLoot.class);

            // loot tracker doesn't count kill if no loot - https://github.com/runelite/runelite/issues/5077
            OptionalDouble nothingProbability = rarityService.getRarity(sourceName, -1, 0);
            if (nothingProbability.isPresent() && nothingProbability.getAsDouble() < 1.0) {
                // estimate the actual kc (including kills with no loot)
                int kc = (int) Math.round(lootRecord.getKills() / (1 - nothingProbability.getAsDouble()));
                return lootRecord.withKills(kc);
            } else {
                return lootRecord;
            }
        } catch (JsonSyntaxException e) {
            // should not occur unless loot tracker changes stored loot POJO structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

    private void handleSlayerLog() {
        var title = client.getWidget(InterfaceID.KillLog.INTERFACE_TITLE);
        if (title == null || !"Slayer Kill Log".equals(title.getText())) {
            return;
        }

        var monsters = client.getWidget(InterfaceID.KillLog.NAME);
        var counts = client.getWidget(InterfaceID.KillLog.KILL);
        if (monsters == null || counts == null) {
            return;
        }

        var mobs = monsters.getChildren();
        var kills = counts.getChildren();
        if (mobs == null || kills == null) {
            return;
        }

        final int n = Math.min(mobs.length, kills.length);
        for (int i = 0; i < n; i++) {
            var mob = mobs[i].getText().replace(":", "");
            var count = kills[i].getText().replace(",", "");

            int kc;
            try {
                kc = Integer.parseInt(count);
            } catch (NumberFormatException e) {
                if (count.startsWith("Lots")) {
                    kc = 65_535;

                    // avoid overwriting a higher kc
                    Integer oldKc = getSlayerKc(mob);
                    if (oldKc != null && oldKc >= kc) {
                        continue;
                    }
                } else {
                    log.debug("Failed to parse slayer log entry for mob '{}' with kc '{}'", mob, count);
                    continue;
                }
            }

            setSlayerKc(mob, kc);
        }
    }

    private void setSlayerKc(String mob, int kc) {
        if (kc <= 0) return;
        configManager.setRSProfileConfiguration(SettingsManager.CONFIG_GROUP, "kc_" + mob.toLowerCase(), kc);
    }

    private Integer getSlayerKc(String mob) {
        if (!config.useSlayerWidgetKc()) return null;
        return configManager.getRSProfileConfiguration(SettingsManager.CONFIG_GROUP, "kc_" + mob.toLowerCase(), int.class);
    }

    /**
     * @param boss {@link LootReceived#getName()}
     * @return lowercase boss name that {@link ChatCommandsPlugin} uses during serialization
     */
    private static String cleanBossName(String boss) {
        if ("The Gauntlet".equalsIgnoreCase(boss)) return "gauntlet";
        if ("The Leviathan".equalsIgnoreCase(boss)) return "leviathan";
        if ("The Whisperer".equalsIgnoreCase(boss)) return "whisperer";
        if ("The Hueycoatl".equalsIgnoreCase(boss)) return "hueycoatl";
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
                // exceptions where boss name in chat message differs from npc name
                if ("Whisperer".equals(sourceName)) return "The Whisperer";
                if ("The Gauntlet".equals(sourceName)) return GAUNTLET_BOSS;
                if (CG_NAME.equals(sourceName)) return CG_BOSS;

                return sourceName;
        }
    }

}
