package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.FilterMode;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class MigrationUtil {

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
