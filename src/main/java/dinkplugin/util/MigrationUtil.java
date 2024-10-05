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

}
