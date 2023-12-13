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
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class KillCountService {

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

        this.incrementKillCount(npc.getName());
    }

    public void onLoot(LootReceived event) {
        if (event.getType() == LootRecordType.NPC && "The Whisperer".equalsIgnoreCase(event.getName())) {
            // Special case: upstream fires LootReceived for the whisperer, but not NpcLootReceived
            this.incrementKillCount(event.getName());
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

    public Integer getKillCount(String npcName) {
        Integer stored = getStoredKillCount(npcName);
        if (stored != null) {
            return killCounts.asMap().merge(npcName, stored, Math::max);
        }
        return killCounts.getIfPresent(npcName);
    }

    private void incrementKillCount(String npcName) {
        killCounts.asMap().compute(npcName, (name, cachedKc) -> {
            if (cachedKc != null) {
                // increment kill count
                return cachedKc + 1;
            } else {
                // pull kc from loot tracker
                Integer kc = getStoredKillCount(name);
                // increment if found
                return kc != null ? kc + 1 : null;
            }
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
            // should not occur unless loot tracker changes stored loot POJO structure
            log.warn("Failed to read kills from loot tracker config", e);
            return null;
        }
    }

}
