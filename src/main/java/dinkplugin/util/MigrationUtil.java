package dinkplugin.util;

import com.google.common.collect.ImmutableMap;
import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.FilterMode;
import dinkplugin.domain.PlayerLookupService;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
        Map<String, String> mappings = Map.of(
            "webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "sendScreenshot", "lootSendImage",
            "lootvalue", "minLootValue"
        );
        return new Metadata("discordlootlogger", mappings, "DiscordLootLoggerPlugin", DinkPluginConfig::notifyLoot, "lootEnabled", null, Set.of("lootlogger", "adam"));
    }

    private Metadata getBoredskaMappings(DinkPluginConfig config) {
        // https://github.com/Boredska/gim-bank-discord/blob/master/src/main/java/gim/bank/discord/GimBankDiscordConfig.java
        Map<String, String> mappings = Map.of("webhook",
            config.groupStorageWebhook().isBlank() && config.notifyGroupStorage() ? "discordWebhook" : "groupStorageWebhook"
        );
        return new Metadata("gimbankdiscord", mappings, "GimBankDiscordPlugin", DinkPluginConfig::notifyGroupStorage, "groupStorageEnabled", null, Set.of("gim", "bank", "gimbank", "boredska"));
    }

    private Metadata getBossHusoMappings(DinkPluginConfig config) {
        // https://github.com/BossHuso/discord-rare-drop-notificater/blob/master/src/main/java/com/masterkenth/DiscordRareDropNotificaterConfig.java
        Map<String, String> mappings = Map.of(
            "webhookurl", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "minrarity", "lootRarityThreshold",
            "minvalue", "minLootValue",
            "andinsteadofor", "lootRarityValueIntersection",
            "sendscreenshot", "lootSendImage",
            "ignoredkeywords", "lootItemDenylist",
            "whiteListedItems", "lootItemAllowlist",
            "sendEmbeddedMessage", "discordRichEmbeds",
            "whiteListedRSNs", config.nameFilterMode() == FilterMode.ALLOW ? "ignoredNames" : ""
        );
        Function<Object, Object> itemListTransformer = v -> ConfigUtil
            .readDelimited(v.toString())
            .map(s -> '*' + s + '*')
            .collect(Collectors.joining("\n"));
        var transformers = Map.of(
            "whiteListedItems", itemListTransformer,
            "ignoredkeywords", itemListTransformer
        );
        return new Metadata("discordraredropnotificater", mappings, "DiscordRareDropNotificaterPlugin", DinkPluginConfig::notifyLoot, "lootEnabled", transformers, Set.of("discordraredropnotifier", "rare", "huso", "bosshuso"));
    }

    private Metadata getJakeMappings(DinkPluginConfig config) {
        // https://github.com/MidgetJake/UniversalDiscordNotifier/blob/master/src/main/java/universalDiscord/UniversalDiscordConfig.java
        Map<String, String> mappings = Map.ofEntries(
            Map.entry("discordWebhook", "discordWebhook"),
            Map.entry("playerUrl", "playerLookupService"),
            Map.entry("collectionLogEnabled", "collectionLogEnabled"),
            Map.entry("collectionSendImage", "collectionSendImage"),
            Map.entry("collectionNotifMessage", "collectionNotifMessage"),
            Map.entry("petEnabled", "petEnabled"),
            Map.entry("petSendImage", "petSendImage"),
            Map.entry("petNotifMessage", "petNotifMessage"),
            Map.entry("levelEnabled", "levelEnabled"),
            Map.entry("levelSendImage", "levelSendImage"),
            Map.entry("levelInterval", "levelInterval"),
            Map.entry("levelNotifMessage", "levelNotifMessage"),
            Map.entry("lootEnabled", "lootEnabled"),
            Map.entry("lootSendImage", "lootSendImage"),
            Map.entry("lootIcons", "lootIcons"),
            Map.entry("minLootValue", "minLootValue"),
            Map.entry("lootNotifMessage", "lootNotifMessage"),
            Map.entry("deathEnabled", "deathEnabled"),
            Map.entry("deathSendImage", "deathSendImage"),
            Map.entry("deathNotifMessage", "deathNotifMessage"),
            Map.entry("slayerEnabled", "slayerEnabled"),
            Map.entry("slayerSendImage", "slayerSendImage"),
            Map.entry("slayerNotifMessage", "slayerNotifMessage"),
            Map.entry("questEnabled", "questEnabled"),
            Map.entry("questSendImage", "questSendImage"),
            Map.entry("questNotifMessage", "questNotifMessage"),
            Map.entry("clueEnabled", "clueEnabled"),
            Map.entry("clueSendImage", "clueSendImage"),
            Map.entry("clueShowItems", "clueShowItems"),
            Map.entry("clueMinValue", "clueMinValue"),
            Map.entry("clueNotifMessage", "clueNotifMessage")
        );
        Map<String, Function<Object, Object>> transformers = Map.of(
            "playerUrl", v -> {
                switch (v.toString()) {
                    case "TEMPLEOSRS":
                        return PlayerLookupService.TEMPLE_OSRS.name();
                    case "WISEOLDMAN":
                        return PlayerLookupService.WISE_OLD_MAN.name();
                    default:
                        return v;
                }
            }
        );
        return new Metadata("universalDiscord", mappings, "UniversalDiscordPlugin", null, null, transformers, Set.of("universaldiscordnotifications", "universal", "jake", "midgetjake"));
    }

    private Metadata getJamesMappings(DinkPluginConfig config) {
        // https://github.com/jamesdrudolph/Discord-Death-Notifications/blob/master/src/main/java/moe/cuteanimegirls/discorddeathnotifications/DeathNotificationsConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.deathWebhook().isBlank() ? "discordWebhook" : "deathWebhook",
            "deathMessage", "deathNotifMessage"
        );
        Map<String, Function<Object, Object>> transformers = Map.of(
            "deathMessage", msg -> "%USERNAME% " + msg
        );
        return new Metadata("discorddeathnotifications", mappings, "DeathNotificationsPlugin", DinkPluginConfig::notifyDeath, "deathEnabled", transformers, Set.of("death", "james", "elguy"));
    }

    private Metadata getPaulMappings(DinkPluginConfig config) {
        // https://github.com/PJGJ210/Discord-Collection-Logger/blob/master/src/main/java/discordcollectionlogger/DiscordCollectionLoggerConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.collectionWebhook().isBlank() ? "discordWebhook" : "collectionWebhook",
            "sendScreenshot", "collectionSendImage",
            "includepets", config.notifyPet() ? "" : "petEnabled"
        );
        return new Metadata("discordcollectionlogger", mappings, "DiscordCollectionLoggerPlugin", DinkPluginConfig::notifyCollectionLog, "collectionLogEnabled", null, Set.of("collection", "paul"));
    }

    private Metadata getRinzMappings(DinkPluginConfig config) {
        // https://github.com/RinZJ/better-discord-loot-logger/blob/master/src/main/java/com/betterdiscordlootlogger/BetterDiscordLootLoggerConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "sendScreenshot", "lootSendImage",
            "pets", config.notifyPet() ? "" : "petEnabled",
            "valuableDropThreshold", "minLootValue",
            "collectionLogItem", config.notifyCollectionLog() ? "" : "collectionLogEnabled"
        );
        return new Metadata("betterdiscordlootlogger", mappings, "BetterDiscordLootLoggerPlugin", DinkPluginConfig::notifyLoot, "lootEnabled", null, Set.of("betterloot", "rinz"));
    }

    private Metadata getShamerMappings(DinkPluginConfig config) {
        // https://github.com/jack0lantern/raidshamer/blob/main/src/main/java/ejedev/raidshamer/RaidShamerConfig.java
        Map<String, String> mappings = Map.of(
            "webhookLink", config.deathWebhook().isBlank() ? "discordWebhook" : "deathWebhook",
            "captureOwnDeaths", config.notifyDeath() ? "" : "deathEnabled"
        );
        return new Metadata("raidshamer", mappings, "RaidShamerPlugin", DinkPluginConfig::notifyDeath, "deathEnabled", null, Set.of("deathshamer", "botanophobia"));
    }

    private Metadata getTakamokMappings(DinkPluginConfig config) {
        // https://github.com/ATremonte/Discord-Level-Notifications/blob/master/src/main/java/com/discordlevelnotifications/LevelNotificationsConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.levelWebhook().isBlank() ? "discordWebhook" : "levelWebhook",
            "sendScreenshot", "levelSendImage",
            "minimumLevel", "levelMinValue",
            "levelInterval", "levelInterval"
        );
        return new Metadata("discordlevelnotifications", mappings, "LevelNotificationsPlugin", DinkPluginConfig::notifyLevel, "levelEnabled", null, Set.of("level", "takamok"));
    }

    @Value
    @Accessors(fluent = true)
    public static class Metadata {
        String configGroup;
        Map<String, String> mappings;
        String pluginClassName;
        Predicate<DinkPluginConfig> notifierEnabled;
        String notifierEnabledKey;
        Map<String, Function<Object, Object>> configValueTransformers;
        Collection<String> aliases;

        public boolean matchesName(String key) {
            return pluginClassName.equalsIgnoreCase(key) || configGroup.equals(key) || aliases.contains(key.toLowerCase());
        }

        public Object transform(String configKey, Object configValue) {
            if (configValueTransformers == null) return configValue;
            return configValueTransformers.getOrDefault(configKey, v -> v).apply(configValue);
        }
    }

    static {
        PLUGIN_METADATA = ImmutableMap.<String, Function<DinkPluginConfig, Metadata>>builder()
//            .put("BetterDiscordLootLogger", MigrationUtil::getRinzMappings)
            .put("DiscordCollectionLogger", MigrationUtil::getPaulMappings)
//            .put("DiscordDeathNotifications", MigrationUtil::getJamesMappings)
//            .put("DiscordLevelNotifications", MigrationUtil::getTakamokMappings)
            .put("DiscordLootLogger", MigrationUtil::getAdamMappings)
            .put("DiscordRareDropNotifier", MigrationUtil::getBossHusoMappings)
//            .put("GIMBankDiscord", MigrationUtil::getBoredskaMappings)
//            .put("RaidShamer", MigrationUtil::getShamerMappings)
//            .put("UniversalDiscordNotifications", MigrationUtil::getJakeMappings)
            .build();
    }
}
