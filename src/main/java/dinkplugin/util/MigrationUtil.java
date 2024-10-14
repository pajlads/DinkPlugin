package dinkplugin.util;

import com.google.common.collect.ImmutableMap;
import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.FilterMode;
import dinkplugin.domain.PlayerLookupService;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import net.runelite.client.config.ConfigManager;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class MigrationUtil {

    public static final Map<String, Function<DinkPluginConfig, Metadata>> PLUGIN_METADATA;

    public Metadata findMetadata(String key, DinkPluginConfig config) {
        var fast = PLUGIN_METADATA.get(key);
        if (fast != null) return fast.apply(config);

        for (var func : PLUGIN_METADATA.values()) {
            var meta = func.apply(config);
            if (meta.matchesName(key))
                return meta;
        }
        return null;
    }

    private Metadata getAdamMappings(DinkPluginConfig config) {
        // https://github.com/Adam-/runelite-plugins/blob/discord-loot-logger/src/main/java/info/sigterm/plugins/discordlootlogger/DiscordLootLoggerConfig.java
        return Metadata.builder()
            .configGroup("discordlootlogger")
            .pluginClassName("DiscordLootLoggerPlugin")
            .alias("lootlogger")
            .alias("adam")
            .notifierEnabledKey("lootEnabled")
            .mapping("webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook")
            .mapping("sendScreenshot", "lootSendImage")
            .mapping("lootvalue", "minLootValue")
            .build();
    }

    private Metadata getBoredskaMappings(DinkPluginConfig config) {
        // https://github.com/Boredska/gim-bank-discord/blob/master/src/main/java/gim/bank/discord/GimBankDiscordConfig.java
        return Metadata.builder()
            .configGroup("gimbankdiscord")
            .pluginClassName("GimBankDiscordPlugin")
            .aliases(Set.of("gim", "bank", "gimbank", "boredska"))
            .notifierEnabledKey("groupStorageEnabled")
            .mapping("webhook", config.groupStorageWebhook().isBlank() && config.notifyGroupStorage() ? "discordWebhook" : "groupStorageWebhook")
            .build();
    }

    private Metadata getBossHusoMappings(DinkPluginConfig config) {
        // https://github.com/BossHuso/discord-rare-drop-notificater/blob/master/src/main/java/com/masterkenth/DiscordRareDropNotificaterConfig.java
        Function<Object, Object> itemListTransformer = v -> ConfigUtil
            .readDelimited(v.toString())
            .map(s -> '*' + s + '*')
            .collect(Collectors.joining("\n"));
        return Metadata.builder()
            .configGroup("discordraredropnotificater")
            .pluginClassName("DiscordRareDropNotificaterPlugin")
            .notifierEnabledKey("lootEnabled")
            .aliases(Set.of("discordraredropnotifier", "rare", "huso", "bosshuso"))
            .mapping("webhookurl", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook")
            .mapping("minrarity", "lootRarityThreshold")
            .mapping("minvalue", "minLootValue")
            .mapping("andinsteadofor", "lootRarityValueIntersection")
            .mapping("sendscreenshot", "lootSendImage")
            .mapping("ignoredkeywords", "lootItemDenylist")
            .mapping("whiteListedItems", "lootItemAllowlist")
            .mapping("sendEmbeddedMessage", "discordRichEmbeds")
            .mapping("whiteListedRSNs", "ignoredNames")
            .configValueTransformer("whiteListedItems", itemListTransformer)
            .configValueTransformer("ignoredkeywords", itemListTransformer)
            .configValueTransformer("whiteListedRSNs", names -> {
                if (!names.toString().isBlank() && config.nameFilterMode() == FilterMode.DENY) {
                    if (config.filteredNames().isBlank()) {
                        config.setNameFilterMode(FilterMode.ALLOW);
                    } else {
                        return "";
                    }
                }
                return names;
            })
            .build();
    }

    private Metadata getJakeMappings(DinkPluginConfig config) {
        // https://github.com/MidgetJake/UniversalDiscordNotifier/blob/master/src/main/java/universalDiscord/UniversalDiscordConfig.java
        return Metadata.builder()
            .configGroup("universalDiscord")
            .pluginClassName("UniversalDiscordPlugin")
            .aliases(Set.of("universaldiscordnotifications", "universal", "jake", "midgetjake"))
            .mapping("discordWebhook", "discordWebhook")
            .mapping("playerUrl", "playerLookupService")
            .mapping("collectionLogEnabled", "collectionLogEnabled")
            .mapping("collectionSendImage", "collectionSendImage")
            .mapping("collectionNotifMessage", "collectionNotifMessage")
            .mapping("petEnabled", "petEnabled")
            .mapping("petSendImage", "petSendImage")
            .mapping("petNotifMessage", "petNotifMessage")
            .mapping("levelEnabled", "levelEnabled")
            .mapping("levelSendImage", "levelSendImage")
            .mapping("levelInterval", "levelInterval")
            .mapping("levelNotifMessage", "levelNotifMessage")
            .mapping("lootEnabled", "lootEnabled")
            .mapping("lootSendImage", "lootSendImage")
            .mapping("lootIcons", "lootIcons")
            .mapping("minLootValue", "minLootValue")
            .mapping("lootNotifMessage", "lootNotifMessage")
            .mapping("deathEnabled", "deathEnabled")
            .mapping("deathSendImage", "deathSendImage")
            .mapping("deathNotifMessage", "deathNotifMessage")
            .mapping("slayerEnabled", "slayerEnabled")
            .mapping("slayerSendImage", "slayerSendImage")
            .mapping("slayerNotifMessage", "slayerNotifMessage")
            .mapping("questEnabled", "questEnabled")
            .mapping("questSendImage", "questSendImage")
            .mapping("questNotifMessage", "questNotifMessage")
            .mapping("clueEnabled", "clueEnabled")
            .mapping("clueSendImage", "clueSendImage")
            .mapping("clueShowItems", "clueShowItems")
            .mapping("clueMinValue", "clueMinValue")
            .mapping("clueNotifMessage", "clueNotifMessage")
            .configValueTransformer("playerUrl", v -> {
                switch (v.toString()) {
                    case "TEMPLEOSRS":
                        return PlayerLookupService.TEMPLE_OSRS.name();
                    case "WISEOLDMAN":
                        return PlayerLookupService.WISE_OLD_MAN.name();
                    default:
                        return v;
                }
            })
            .build();
    }

    private Metadata getJamesMappings(DinkPluginConfig config) {
        // https://github.com/jamesdrudolph/Discord-Death-Notifications/blob/master/src/main/java/moe/cuteanimegirls/discorddeathnotifications/DeathNotificationsConfig.java
        return Metadata.builder()
            .configGroup("discorddeathnotifications")
            .pluginClassName("DeathNotificationsPlugin")
            .alias("death")
            .alias("elguy")
            .notifierEnabledKey("deathEnabled")
            .mapping("webhook", config.deathWebhook().isBlank() ? "discordWebhook" : "deathWebhook")
            .mapping("deathMessage", "deathNotifMessage")
            .configValueTransformer("deathMessage", msg -> "%USERNAME% " + msg)
            .build();
    }

    private Metadata getPaulMappings(DinkPluginConfig config) {
        // https://github.com/PJGJ210/Discord-Collection-Logger/blob/master/src/main/java/discordcollectionlogger/DiscordCollectionLoggerConfig.java
        return Metadata.builder()
            .configGroup("discordcollectionlogger")
            .pluginClassName("DiscordCollectionLoggerPlugin")
            .alias("collection")
            .alias("paul")
            .notifierEnabledKey("collectionLogEnabled")
            .mapping("webhook", config.collectionWebhook().isBlank() ? "discordWebhook" : "collectionWebhook")
            .mapping("sendScreenshot", "collectionSendImage")
            .mapping("includepets", config.notifyPet() ? "" : "petEnabled")
            .build();
    }

    private Metadata getRinzMappings(DinkPluginConfig config) {
        // https://github.com/RinZJ/better-discord-loot-logger/blob/master/src/main/java/com/betterdiscordlootlogger/BetterDiscordLootLoggerConfig.java
        return Metadata.builder()
            .configGroup("betterdiscordlootlogger")
            .pluginClassName("BetterDiscordLootLoggerPlugin")
            .alias("betterloot")
            .alias("rinz")
            .notifierEnabledKey("lootEnabled")
            .mapping("webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook")
            .mapping("sendScreenshot", "lootSendImage")
            .mapping("pets", config.notifyPet() ? "" : "petEnabled")
            .mapping("valuableDropThreshold", "minLootValue")
            .mapping("collectionLogItem", config.notifyCollectionLog() ? "" : "collectionLogEnabled")
            .build();
    }

    private Metadata getShamerMappings(DinkPluginConfig config) {
        // https://github.com/jack0lantern/raidshamer/blob/main/src/main/java/ejedev/raidshamer/RaidShamerConfig.java
        return Metadata.builder()
            .configGroup("raidshamer")
            .pluginClassName("RaidShamerPlugin")
            .alias("deathshamer")
            .alias("botanophobia")
            .notifierEnabledKey("deathEnabled")
            .mapping("webhookLink", config.deathWebhook().isBlank() ? "discordWebhook" : "deathWebhook")
            .mapping("captureOwnDeaths", config.notifyDeath() ? "" : "deathEnabled")
            .build();
    }

    private Metadata getTakamokMappings(DinkPluginConfig config) {
        // https://github.com/ATremonte/Discord-Level-Notifications/blob/master/src/main/java/com/discordlevelnotifications/LevelNotificationsConfig.java
        return Metadata.builder()
            .configGroup("discordlevelnotifications")
            .pluginClassName("LevelNotificationsPlugin")
            .alias("level")
            .alias("takamok")
            .notifierEnabledKey("levelEnabled")
            .mapping("webhook", config.levelWebhook().isBlank() ? "discordWebhook" : "levelWebhook")
            .mapping("sendScreenshot", "levelSendImage")
            .mapping("minimumLevel", "levelMinValue")
            .mapping("levelInterval", "levelInterval")
            .build();
    }

    @Value
    @Builder
    @Accessors(fluent = true)
    public static class Metadata {
        @NonNull
        String configGroup;

        @Singular
        @NonNull
        Map<String, String> mappings;

        @NonNull
        String pluginClassName;

        @Nullable
        String notifierEnabledKey;

        @Singular
        @Nullable
        Map<String, Function<Object, Object>> configValueTransformers;

        @Singular
        @NonNull
        Set<String> aliases;

        public Map<String, Object> readConfig(ConfigManager configManager, Map<String, Type> configValueTypes) {
            Map<String, Object> valuesByKey = new HashMap<>(mappings.size() * 4 / 3);
            mappings.forEach((sourceKey, dinkKey) -> {
                Type valueType = configValueTypes.get(dinkKey);
                if (valueType == null) return;

                var sourceValue = configManager.getConfiguration(configGroup, sourceKey, valueType);
                if (sourceValue != null) {
                    var transformedValue = transform(sourceKey, sourceValue);
                    valuesByKey.put(dinkKey, transformedValue);
                }
            });
            return valuesByKey;
        }

        public boolean shouldEnableNotifier(ConfigManager configManager) {
            return notifierEnabledKey != null && !ConfigUtil.isPluginDisabled(configManager, pluginClassName.toLowerCase());
        }

        boolean matchesName(String key) {
            return pluginClassName.equalsIgnoreCase(key) || configGroup.equals(key) || aliases.contains(key.toLowerCase());
        }

        private Object transform(String configKey, Object configValue) {
            if (configValueTransformers == null) return configValue;
            return configValueTransformers.getOrDefault(configKey, v -> v).apply(configValue);
        }
    }

    static {
        PLUGIN_METADATA = ImmutableMap.<String, Function<DinkPluginConfig, Metadata>>builder()
            .put("BetterDiscordLootLogger", MigrationUtil::getRinzMappings)
            .put("DiscordCollectionLogger", MigrationUtil::getPaulMappings)
            .put("DiscordDeathNotifications", MigrationUtil::getJamesMappings)
            .put("DiscordLevelNotifications", MigrationUtil::getTakamokMappings)
            .put("DiscordLootLogger", MigrationUtil::getAdamMappings)
            .put("DiscordRareDropNotifier", MigrationUtil::getBossHusoMappings)
            .put("GIMBankDiscord", MigrationUtil::getBoredskaMappings)
            .put("RaidShamer", MigrationUtil::getShamerMappings)
            .put("UniversalDiscordNotifications", MigrationUtil::getJakeMappings)
            .build();
    }
}
