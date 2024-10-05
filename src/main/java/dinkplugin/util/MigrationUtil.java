package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.FilterMode;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class MigrationUtil {

    public Map.Entry<String, Map<String, String>> getAdamMappings(DinkPluginConfig config) {
        // https://github.com/Adam-/runelite-plugins/blob/discord-loot-logger/src/main/java/info/sigterm/plugins/discordlootlogger/DiscordLootLoggerConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "sendScreenshot", "lootSendImage",
            "lootvalue", "minLootValue"
        );
        return Map.entry("discordlootlogger", mappings);
    }

    public Map.Entry<String, Map<String, String>> getBossHusoMappings(DinkPluginConfig config) {
        // https://github.com/BossHuso/discord-rare-drop-notificater/blob/master/src/main/java/com/masterkenth/DiscordRareDropNotificaterConfig.java
        Map<String, String> mappings = Map.of(
            "webhookurl", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "minrarity", "lootRarityThreshold",
            "minvalue", "minLootValue",
            "andinsteadofor", "lootRarityValueIntersection",
            "sendscreenshot", "lootSendImage",
            "ignoredkeywords", "lootItemDenylist",
            "whiteListedItems", "whiteListedItems",
            "sendEmbeddedMessage", "discordRichEmbeds",
            "whiteListedRSNs", config.nameFilterMode() == FilterMode.ALLOW ? "ignoredNames" : ""
        );
        return Map.entry("discordraredropnotificater", mappings);
    }

    public Map.Entry<String, Map<String, String>> getJakeMappings() {
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
        return Map.entry("universalDiscord", mappings);
    }

    public Map.Entry<String, Map<String, String>> getPaulMappings(DinkPluginConfig config) {
        // https://github.com/PJGJ210/Discord-Collection-Logger/blob/master/src/main/java/discordcollectionlogger/DiscordCollectionLoggerConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.collectionWebhook().isBlank() ? "discordWebhook" : "collectionWebhook",
            "sendScreenshot", "collectionSendImage",
            "includepets", config.notifyPet() ? "" : "petEnabled"
        );
        return Map.entry("discordcollectionlogger", mappings);
    }

    public Map.Entry<String, Map<String, String>> getRinzMappings(DinkPluginConfig config) {
        // https://github.com/RinZJ/better-discord-loot-logger/blob/master/src/main/java/com/betterdiscordlootlogger/BetterDiscordLootLoggerConfig.java
        Map<String, String> mappings = Map.of(
            "webhook", config.lootWebhook().isBlank() ? "discordWebhook" : "lootWebhook",
            "sendScreenshot", "lootSendImage",
            "pets", config.notifyPet() ? "" : "petEnabled",
            "valuableDropThreshold", "minLootValue",
            "collectionLogItem", config.notifyCollectionLog() ? "" : "collectionLogEnabled"
        );
        return Map.entry("betterdiscordlootlogger", mappings);
    }

    public Map.Entry<String, Map<String, String>> getShamerMappings(DinkPluginConfig config) {
        // https://github.com/jack0lantern/raidshamer/blob/main/src/main/java/ejedev/raidshamer/RaidShamerConfig.java
        Map<String, String> mappings = Map.of(
            "webhookLink", config.deathWebhook().isBlank() ? "discordWebhook" : "deathWebhook",
            "captureOwnDeaths", config.notifyDeath() ? "" : "deathEnabled"
        );
        return Map.entry("raidshamer", mappings);
    }

}
