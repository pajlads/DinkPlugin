package dinkplugin;

import dinkplugin.domain.AchievementDiaries;
import dinkplugin.domain.CombatAchievementTier;
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
        position = 21
    )
    String slayerSection = "Slayer";

    @ConfigSection(
        name = "Quests",
        description = "Settings for notifying when you complete a quest",
        position = 24
    )
    String questSection = "Quests";

    @ConfigSection(
        name = "Clue Scrolls",
        description = "Settings for notifying when you complete a clue scroll",
        position = 27
    )
    String clueSection = "Clue Scrolls";

    @ConfigSection(
        name = "Speedruns",
        description = "Settings for notifying when you finish a speedrun",
        position = 32
    )
    String speedrunSection = "Speedruns";

    @ConfigSection(
        name = "Kill Count",
        description = "Settings for notifying when you kill a boss",
        position = 37
    )
    String killCountSection = "Kill Count";

    @ConfigSection(
        name = "Combat Tasks",
        description = "Settings for notifying when you complete a combat achievement",
        position = 43
    )
    String combatTaskSection = "Combat Tasks";

    @ConfigSection(
        name = "Achievement Diary",
        description = "Settings for notifying when you complete an Achievement Diary",
        position = 47
    )
    String diarySection = "Achievement Diary";

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
        keyName = "lootSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 12,
        section = lootSection
    )
    default boolean lootSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "lootIcons",
        name = "Show loot icons",
        description = "Show icons for the loot obtained",
        position = 13,
        section = lootSection
    )
    default boolean lootIcons() {
        return false;
    }

    @ConfigItem(
        keyName = "minLootValue",
        name = "Min Loot value",
        description = "Minimum value of the loot to notify",
        position = 14,
        section = lootSection
    )
    default int minLootValue() {
        return 0;
    }

    @ConfigItem(
        keyName = "lootNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %LOOT% to insert the loot and %SOURCE% to show the source of the loot",
        position = 15,
        section = lootSection
    )
    default String lootNotifyMessage() {
        return "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%";
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
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %VALUELOST% to insert the GE value of the stuff you lost",
        position = 18,
        section = deathSection
    )
    default String deathNotifyMessage() {
        return "%USERNAME% has died...";
    }

    @ConfigItem(
        keyName = "deathNotifPvpEnabled",
        name = "Distinguish PvP deaths",
        description = "Should the plugin use a different message for dying in PvP?",
        position = 19,
        section = deathSection
    )
    default boolean deathNotifPvpEnabled() {
        return true;
    }

    @ConfigItem(
        keyName = "deathNotifPvpMessage",
        name = "PvP notification message",
        description = "The message to be sent through the webhook. Use %PKER% to insert the killer, %USERNAME% to insert your username, %VALUELOST% to insert the GE value of the stuff you lost",
        position = 20,
        section = deathSection
    )
    default String deathNotifPvpMessage() {
        return "%USERNAME% has just been PKed by %PKER% for %VALUELOST% gp...";
    }

    @ConfigItem(
        keyName = "slayerEnabled",
        name = "Enable Slayer",
        description = "Enable notifications for when you complete a slayer task",
        position = 21,
        section = slayerSection
    )
    default boolean notifySlayer() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 22,
        section = slayerSection
    )
    default boolean slayerSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerPointThreshold",
        name = "Min Slayer Points",
        description = "The minimum slayer task points to warrant a notification",
        position = 23,
        section = slayerSection
    )
    default int slayerPointThreshold() {
        return 0;
    }

    @ConfigItem(
        keyName = "slayerNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %TASK% to insert your task, %POINTS% to show how many points you obtained and %TASKCOUNT% to show how many tasks you have completed.",
        position = 24,
        section = slayerSection
    )
    default String slayerNotifyMessage() {
        return "%USERNAME% has completed a slayer task: %TASK%, getting %POINTS% points and making that %TASKCOUNT% tasks completed";
    }

    @ConfigItem(
        keyName = "questEnabled",
        name = "Enable Quest",
        description = "Enable notifications for when you complete a quest",
        position = 25,
        section = questSection
    )
    default boolean notifyQuest() {
        return false;
    }

    @ConfigItem(
        keyName = "questSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 26,
        section = questSection
    )
    default boolean questSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "questNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %QUEST% to insert the quest that you completed",
        position = 27,
        section = questSection
    )
    default String questNotifyMessage() {
        return "%USERNAME% has completed a quest: %QUEST%";
    }

    @ConfigItem(
        keyName = "clueEnabled",
        name = "Enable Clue Scrolls",
        description = "Enable notifications for when you complete a clue scroll",
        position = 28,
        section = clueSection
    )
    default boolean notifyClue() {
        return false;
    }

    @ConfigItem(
        keyName = "clueSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 29,
        section = clueSection
    )
    default boolean clueSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "clueShowItems",
        name = "Show Item Icons",
        description = "Show item icons gained from the clue",
        position = 30,
        section = clueSection
    )
    default boolean clueShowItems() {
        return false;
    }

    @ConfigItem(
        keyName = "clueMinValue",
        name = "Min Value",
        description = "The minimum value of the items to be shown",
        position = 31,
        section = clueSection
    )
    default int clueMinValue() {
        return 0;
    }

    @ConfigItem(
        keyName = "clueNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %CLUE% to insert the clue type, %LOOT% to show the loot obtained and %COUNT% to insert how many of those clue types you have completed",
        position = 32,
        section = clueSection
    )
    default String clueNotifyMessage() {
        return "%USERNAME% has completed a %CLUE% clue, they have completed %COUNT%.\nThey obtained:\n\n%LOOT%";
    }

    @ConfigItem(
        keyName = "speedrunEnabled",
        name = "Enable speedruns",
        description = "Enable notifications for when you complete a speedrun",
        position = 33,
        section = speedrunSection
    )
    default boolean notifySpeedrun() {
        return false;
    }

    @ConfigItem(
        keyName = "speedrunSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 34,
        section = speedrunSection
    )
    default boolean speedrunSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "speedrunPBOnly",
        name = "Notify on Personal Best only",
        description = "Enable notifications only for your best runs",
        position = 35,
        section = speedrunSection
    )
    default boolean speedrunPBOnly() {
        return true;
    }

    @ConfigItem(
        keyName = "speedrunPBMessage",
        name = "PB message",
        description = "%USERNAME% to insert your username, %QUEST% to insert the quest name, %TIME% to insert your new time",
        position = 36,
        section = speedrunSection
    )
    default String speedrunPBMessage() {
        return "%USERNAME% has just beat their personal best in a speedrun of %QUEST% with a time of %TIME%";
    }

    @ConfigItem(
        keyName = "speedrunMessage",
        name = "Notification message",
        description = "%USERNAME% to insert your username, %QUEST% to insert the quest name, %TIME% to insert your new time, %BEST% to insert your PB",
        position = 37,
        section = speedrunSection
    )
    default String speedrunMessage() {
        return "%USERNAME% has just finished a speedrun of %QUEST% with a time of %TIME% (their PB is %BEST%)";
    }

    @ConfigItem(
        keyName = "killCountEnabled",
        name = "Enable Kill Count",
        description = "Enable notifications for boss kill count milestones",
        position = 38,
        section = killCountSection
    )
    default boolean notifyKillCount() {
        return false;
    }

    @ConfigItem(
        keyName = "killCountSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 39,
        section = killCountSection
    )
    default boolean killCountSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "killCountInitial",
        name = "Initial Boss Kill",
        description = "Notify on the first ever kill of any boss",
        position = 40,
        section = killCountSection
    )
    default boolean killCountNotifyInitial() {
        return true;
    }

    @ConfigItem(
        keyName = "killCountInterval",
        name = "Kill Count Interval",
        description = "Interval between when a notification should be sent",
        position = 41,
        section = killCountSection
    )
    default int killCountInterval() {
        return 50;
    }

    @ConfigItem(
        keyName = "killCountMessage",
        name = "Notification Message",
        description = "The message to be sent to the webhook. Use %USERNAME% to insert your username, %BOSS% to insert the NPC name, %COUNT% to insert the kill count",
        position = 42,
        section = killCountSection
    )
    default String killCountMessage() {
        return "%USERNAME% has defeated %BOSS% with a completion count of %COUNT%";
    }

    @ConfigItem(
        keyName = "combatTaskEnabled",
        name = "Enable Combat Tasks",
        description = "Enable notifications for combat achievements",
        position = 43,
        section = combatTaskSection
    )
    default boolean notifyCombatTask() {
        return false;
    }

    @ConfigItem(
        keyName = "combatTaskSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 44,
        section = combatTaskSection
    )
    default boolean combatTaskSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "combatTaskMinTier",
        name = "Min Tier",
        description = "Minimum combat achievement tier to warrant a notification",
        position = 45,
        section = combatTaskSection
    )
    default CombatAchievementTier minCombatAchievementTier() {
        return CombatAchievementTier.EASY;
    }

    @ConfigItem(
        keyName = "combatTaskMessage",
        name = "Notification Message",
        description = "The message to be sent to the webhook. Use %USERNAME% to insert your username, %TIER% to insert the task tier, %TASK% to insert the task name",
        position = 46,
        section = combatTaskSection
    )
    default String combatTaskMessage() {
        return "%USERNAME% has completed %TIER% combat task: %TASK%";
    }

    @ConfigItem(
        keyName = "diaryEnabled",
        name = "Enable Diary",
        description = "Enable notifications for achievement diary completions",
        position = 47,
        section = diarySection
    )
    default boolean notifyAchievementDiary() {
        return false;
    }

    @ConfigItem(
        keyName = "diaryMinDifficulty",
        name = "Min Difficulty",
        description = "Minimum achievement diary difficulty to warrant a notification",
        position = 48,
        section = diarySection
    )
    default AchievementDiaries.Difficulty minDiaryDifficulty() {
        return AchievementDiaries.Difficulty.EASY;
    }

}
