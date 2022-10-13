package dinkplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("dinkplugin")
public interface DinkPluginConfig extends Config {
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

    @ConfigSection(
        name = "Death",
        description = "Settings for notifying when you die",
        position = 16
    )
    String deathSection = "Death";

    @ConfigSection(
        name = "Slayer",
        description = "Settings for notifying when you complete a slayer task",
        position = 19
    )
    String slayerSection = "Slayer";

    @ConfigSection(
        name = "Quests",
        description = "Settings for notifying when you complete a quest",
        position = 22
    )
    String questSection = "Quests";

    @ConfigSection(
        name = "Clue Scrolls",
        description = "Settings for notifying when you complete a clue scroll",
        position = 25
    )
    String clueSection = "Clue Scrolls";

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
        keyName = "collectionSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 2,
        section = collectionSection
    )
    default boolean collectionSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "collectionNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %ITEM% for the item",
        position = 3,
        section = collectionSection
    )
    default String collectionNotifyMessage() {
        return "%USERNAME% has added %ITEM% to their collection";
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
        keyName = "petSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 5,
        section = petSection
    )
    default boolean petSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "petNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username",
        position = 6,
        section = petSection
    )
    default String petNotifyMessage() {
        return "%USERNAME% has a funny feeling they are being followed";
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
        keyName = "levelSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 8,
        section = levelSection
    )
    default boolean levelSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "levelInterval",
        name = "Notify Interval",
        description = "Interval between when a notification should be sent",
        position = 9,
        section = levelSection
    )
    default int levelInterval() {
        return 1;
    }

    @ConfigItem(
        keyName = "levelNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %SKILL% to insert the levelled skill(s)",
        position = 10,
        section = levelSection
    )
    default String levelNotifyMessage() {
        return "%USERNAME% has levelled %SKILL%";
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
        return "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%";
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

    @ConfigItem(
        keyName = "deathEnabled",
        name = "Enable Death",
        description = "Enable notifications for when you die",
        position = 16,
        section = deathSection
    )
    default boolean notifyDeath() {
        return false;
    }

    @ConfigItem(
        keyName = "deathSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 17,
        section = deathSection
    )
    default boolean deathSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "deathNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username",
        position = 18,
        section = deathSection
    )
    default String deathNotifyMessage() {
        return "%USERNAME% has died...";
    }

    @ConfigItem(
        keyName = "slayerEnabled",
        name = "Enable Slayer",
        description = "Enable notifications for when you complete a slayer task",
        position = 19,
        section = slayerSection
    )
    default boolean notifySlayer() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 20,
        section = slayerSection
    )
    default boolean slayerSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %TASK% to insert your task, %POINTS% to show how many points you obtained and %TASKCOUNT% to show how many tasks you have completed.",
        position = 21,
        section = slayerSection
    )
    default String slayerNotifyMessage() {
        return "%USERNAME% has completed a slayer task: %TASK%, getting %POINTS% points and making that %TASKCOUNT% tasks completed";
    }

    @ConfigItem(
        keyName = "questEnabled",
        name = "Enable Quest",
        description = "Enable notifications for when you complete a quest",
        position = 22,
        section = questSection
    )
    default boolean notifyQuest() {
        return false;
    }

    @ConfigItem(
        keyName = "questSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 23,
        section = questSection
    )
    default boolean questSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "questNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %QUEST% to insert the quest that you completed",
        position = 24,
        section = questSection
    )
    default String questNotifyMessage() {
        return "%USERNAME% has completed a quest: %QUEST%";
    }

    @ConfigItem(
        keyName = "clueEnabled",
        name = "Enable Clue Scrolls",
        description = "Enable notifications for when you complete a clue scroll",
        position = 25,
        section = clueSection
    )
    default boolean notifyClue() {
        return false;
    }

    @ConfigItem(
        keyName = "clueSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 26,
        section = clueSection
    )
    default boolean clueSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "clueShowItems",
        name = "Show Item Icons",
        description = "Show item icons gained from the clue",
        position = 27,
        section = clueSection
    )
    default boolean clueShowItems() {
        return false;
    }

    @ConfigItem(
        keyName = "clueMinValue",
        name = "Min Value",
        description = "The minimum value of the items to be shown",
        position = 28,
        section = clueSection
    )
    default int clueMinValue() {
        return 0;
    }

    @ConfigItem(
        keyName = "clueNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %CLUE% to insert the clue type, %LOOT% to show the loot obtained and %COUNT% to insert how many of those clue types you have completed",
        position = 29,
        section = clueSection
    )
    default String clueNotifyMessage() {
        return "%USERNAME% has completed a %CLUE% clue, they have completed %COUNT%.\nThey obtained:\n\n%LOOT%";
    }

}
