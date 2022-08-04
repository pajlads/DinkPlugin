package universalDiscord;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("universalDiscord")
public interface UniversalDiscordConfig extends Config {
    @ConfigSection(
            name = "Collection Log",
            description = "Settings for notifying about collection log",
            position = 1
    )
    String collectionSection = "Collection Log";

    @ConfigSection(
            name = "Pet",
            description = "Settings for notifying when obtaining a pet",
            position = 4
    )
    String petSection = "Pet";

    @ConfigSection(
            name = "Levels",
            description = "Settings for notifying when levelling a skill",
            position = 7
    )
    String levelSection = "Levels";

    @ConfigSection(
            name = "Loot",
            description = "Settings for notifying when loot is dropped",
            position = 11
    )
    String lootSection = "Loot";

    @ConfigItem(
            keyName = "discordWebhook",
            name = "Discord Webhook",
            description = "The Webhook URL for the discord channel",
            position = 0
    )
    default String discordWebhook() {
        return "";
    }

    @ConfigItem(
            keyName = "collectionLogEnabled",
            name = "Enable collection log",
            description = "Enable notifications for collection log",
            position = 1,
            section = collectionSection
    )
    default boolean notifyCollectionLog() {
        return false;
    }

    @ConfigItem(
            keyName = "collectionNotifMessage",
            name = "Notification Message",
            description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %ITEM% for the item",
            position = 2,
            section = collectionSection
    )
    default String collectionNotifyMessage() {
        return "%USERNAME% has added %ITEM% to their collection";
    }

    @ConfigItem(
            keyName = "collectionSendImage",
            name = "Send Image",
            description = "Send image with the notification",
            position = 3,
            section = collectionSection
    )
    default boolean collectionSendImage() {
        return true;
    }

    @ConfigItem(
            keyName = "petEnabled",
            name = "Enable pets",
            description = "Enable notifications for obtaining pets",
            position = 4,
            section = petSection
    )
    default boolean notifyPet() {
        return false;
    }

    @ConfigItem(
            keyName = "petNotifMessage",
            name = "Notification Message",
            description = "The message to be sent through the webhook. Use %USERNAME% to insert your username",
            position = 5,
            section = petSection
    )
    default String petNotifyMessage() {
        return "%USERNAME% has a funny feeling they are being followed";
    }

    @ConfigItem(
            keyName = "petSendImage",
            name = "Send Image",
            description = "Send image with the notification",
            position = 6,
            section = petSection
    )
    default boolean petSendImage() {
        return true;
    }


    @ConfigItem(
            keyName = "levelEnabled",
            name = "Enable level",
            description = "Enable notifications for gaining levels",
            position = 7,
            section = levelSection
    )
    default boolean notifyLevel() {
        return false;
    }

    @ConfigItem(
            keyName = "levelInterval",
            name = "Notify Interval",
            description = "Interval between when a notification should be sent",
            position = 8,
            section = levelSection
    )
    default int levelInterval() {
        return 1;
    }

    @ConfigItem(
            keyName = "levelNotifMessage",
            name = "Notification Message",
            description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %SKILL% to insert the levelled skill(s)",
            position = 9,
            section = levelSection
    )
    default String levelNotifyMessage() {
        return "%USERNAME% has levelled %SKILL%";
    }

    @ConfigItem(
            keyName = "levelSendImage",
            name = "Send Image",
            description = "Send image with the notification",
            position = 10,
            section = levelSection
    )
    default boolean levelSendImage() {
        return true;
    }

    @ConfigItem(
            keyName = "lootEnabled",
            name = "Enable loot",
            description = "Enable notifications for gaining loot",
            position = 11,
            section = lootSection
    )
    default boolean notifyLoot() {
        return false;
    }

    @ConfigItem(
            keyName = "lootIcons",
            name = "Show loot icons",
            description = "Show icons for the loot obtained",
            position = 12,
            section = lootSection
    )
    default boolean lootIcons() {
        return false;
    }

    @ConfigItem(
            keyName = "minLootValue",
            name = "Min Loot value",
            description = "Minimum value of the loot to notify",
            position = 13,
            section = lootSection
    )
    default int minLootValue() {
        return 0;
    }

    @ConfigItem(
            keyName = "lootNotifMessage",
            name = "Notification Message",
            description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %LOOT% to insert the loot and %SOURCE% to show the source of the loot",
            position = 14,
            section = lootSection
    )
    default String lootNotifyMessage() {
        return "%USERNAME% has looted: \n\n%LOOT%\n From: %SOURCE%";
    }

    @ConfigItem(
            keyName = "lootSendImage",
            name = "Send Image",
            description = "Send image with the notification",
            position = 15,
            section = lootSection
    )
    default boolean lootSendImage() {
        return true;
    }
}
