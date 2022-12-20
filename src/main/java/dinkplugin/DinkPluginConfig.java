package dinkplugin;

import dinkplugin.domain.AchievementDiary;
import dinkplugin.domain.CombatAchievementTier;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("dinkplugin")
public interface DinkPluginConfig extends Config {

    @ConfigSection(
        name = "Webhook Overrides",
        description = "Allows webhook data to be sent to a different URL, for the various notifiers",
        position = -20,
        closedByDefault = true
    )
    String webhookSection = "Webhook Overrides";

    @ConfigSection(
        name = "Collection Log",
        description = "Settings for notifying about collection log",
        position = 0
    )
    String collectionSection = "Collection Log";

    @ConfigSection(
        name = "Pet",
        description = "Settings for notifying when obtaining a pet",
        position = 10
    )
    String petSection = "Pet";

    @ConfigSection(
        name = "Levels",
        description = "Settings for notifying when levelling a skill",
        position = 20
    )
    String levelSection = "Levels";

    @ConfigSection(
        name = "Loot",
        description = "Settings for notifying when loot is dropped",
        position = 30
    )
    String lootSection = "Loot";

    @ConfigSection(
        name = "Death",
        description = "Settings for notifying when you die",
        position = 40
    )
    String deathSection = "Death";

    @ConfigSection(
        name = "Slayer",
        description = "Settings for notifying when you complete a slayer task",
        position = 50
    )
    String slayerSection = "Slayer";

    @ConfigSection(
        name = "Quests",
        description = "Settings for notifying when you complete a quest",
        position = 60
    )
    String questSection = "Quests";

    @ConfigSection(
        name = "Clue Scrolls",
        description = "Settings for notifying when you complete a clue scroll",
        position = 70
    )
    String clueSection = "Clue Scrolls";

    @ConfigSection(
        name = "Speedruns",
        description = "Settings for notifying when you finish a speedrun",
        position = 80
    )
    String speedrunSection = "Speedruns";

    @ConfigSection(
        name = "Kill Count",
        description = "Settings for notifying when you kill a boss",
        position = 90
    )
    String killCountSection = "Kill Count";

    @ConfigSection(
        name = "Combat Tasks",
        description = "Settings for notifying when you complete a combat achievement",
        position = 100
    )
    String combatTaskSection = "Combat Tasks";

    @ConfigSection(
        name = "Achievement Diary",
        description = "Settings for notifying when you complete an Achievement Diary",
        position = 110
    )
    String diarySection = "Achievement Diary";

    @ConfigItem(
        keyName = "maxRetries",
        name = "Webhook Max Retries",
        description = "The maximum number of retry attempts for sending a webhook message. Negative implies no attempts",
        position = -100,
        hidden = true
    )
    default int maxRetries() {
        return 3;
    }

    @ConfigItem(
        keyName = "baseRetryDelay",
        name = "Webhook Retry Base Delay",
        description = "The base number of milliseconds to wait before attempting a retry for a webhook message",
        position = -99,
        hidden = true
    )
    default long baseRetryDelay() {
        return 2000L;
    }

    @ConfigItem(
        keyName = "discordWebhook", // do not rename; would break old configs
        name = "Primary Webhook URLs",
        description = "The default webhook URL to send notifications to, if no override is specified.<br/>" +
            "You can target multiple webhooks by specifying their URLs on separate lines",
        position = -20
    )
    default String primaryWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "collectionWebhook",
        name = "Collection Webhook Override",
        description = "If non-empty, collection messages are sent to this URL, instead of the primary URL",
        position = -19,
        section = webhookSection
    )
    default String collectionWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "petWebhook",
        name = "Pet Webhook Override",
        description = "If non-empty, pet messages are sent to this URL, instead of the primary URL",
        position = -18,
        section = webhookSection
    )
    default String petWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "levelWebhook",
        name = "Level Webhook Override",
        description = "If non-empty, level up messages are sent to this URL, instead of the primary URL",
        position = -17,
        section = webhookSection
    )
    default String levelWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "lootWebhook",
        name = "Loot Webhook Override",
        description = "If non-empty, loot messages are sent to this URL, instead of the primary URL",
        position = -16,
        section = webhookSection
    )
    default String lootWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "deathWebhook",
        name = "Death Webhook Override",
        description = "If non-empty, death messages are sent to this URL, instead of the primary URL",
        position = -15,
        section = webhookSection
    )
    default String deathWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "slayerWebhook",
        name = "Slayer Webhook Override",
        description = "If non-empty, slayer messages are sent to this URL, instead of the primary URL",
        position = -14,
        section = webhookSection
    )
    default String slayerWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "questWebhook",
        name = "Quest Webhook Override",
        description = "If non-empty, quest messages are sent to this URL, instead of the primary URL",
        position = -13,
        section = webhookSection
    )
    default String questWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "clueWebhook",
        name = "Clue Webhook Override",
        description = "If non-empty, clue messages are sent to this URL, instead of the primary URL",
        position = -12,
        section = webhookSection
    )
    default String clueWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "speedrunWebhook",
        name = "Speedrun Webhook Override",
        description = "If non-empty, speedrun messages are sent to this URL, instead of the primary URL",
        position = -11,
        section = webhookSection
    )
    default String speedrunWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "killCountWebhook",
        name = "Kill Count Webhook Override",
        description = "If non-empty, kill count messages are sent to this URL, instead of the primary URL",
        position = -10,
        section = webhookSection
    )
    default String killCountWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "combatTaskWebhook",
        name = "Combat Task Webhook Override",
        description = "If non-empty, combat task messages are sent to this URL, instead of the primary URL",
        position = -9,
        section = webhookSection
    )
    default String combatTaskWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "diaryWebhook",
        name = "Diary Webhook Override",
        description = "If non-empty, achievement diary messages are sent to this URL, instead of the primary URL",
        position = -8,
        section = webhookSection
    )
    default String diaryWebhook() {
        return "";
    }

    @ConfigItem(
        keyName = "collectionLogEnabled",
        name = "Enable collection log",
        description = "Enable notifications for collection log.<br/>" +
            "Requires 'Chat > Collection log - New addition notification' setting to be enabled",
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
        position = 10,
        section = petSection
    )
    default boolean notifyPet() {
        return false;
    }

    @ConfigItem(
        keyName = "petSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 11,
        section = petSection
    )
    default boolean petSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "petNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username",
        position = 12,
        section = petSection
    )
    default String petNotifyMessage() {
        return "%USERNAME% has a funny feeling they are being followed";
    }

    @ConfigItem(
        keyName = "levelEnabled",
        name = "Enable level",
        description = "Enable notifications for gaining levels",
        position = 20,
        section = levelSection
    )
    default boolean notifyLevel() {
        return false;
    }

    @ConfigItem(
        keyName = "levelSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 21,
        section = levelSection
    )
    default boolean levelSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "levelInterval",
        name = "Notify Interval",
        description = "Interval between when a notification should be sent",
        position = 22,
        section = levelSection
    )
    default int levelInterval() {
        return 1;
    }

    @ConfigItem(
        keyName = "levelNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %SKILL% to insert the levelled skill(s)",
        position = 23,
        section = levelSection
    )
    default String levelNotifyMessage() {
        return "%USERNAME% has levelled %SKILL%";
    }

    @ConfigItem(
        keyName = "lootEnabled",
        name = "Enable loot",
        description = "Enable notifications for gaining loot",
        position = 30,
        section = lootSection
    )
    default boolean notifyLoot() {
        return false;
    }

    @ConfigItem(
        keyName = "lootSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 31,
        section = lootSection
    )
    default boolean lootSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "lootIcons",
        name = "Show loot icons",
        description = "Show icons for the loot obtained",
        position = 32,
        section = lootSection
    )
    default boolean lootIcons() {
        return false;
    }

    @ConfigItem(
        keyName = "minLootValue",
        name = "Min Loot value",
        description = "The minimum value of an item for a notification to be sent",
        position = 33,
        section = lootSection
    )
    default int minLootValue() {
        return 0;
    }

    @ConfigItem(
        keyName = "lootIncludePlayer",
        name = "Include PK Loot",
        description = "Allow notifications for loot from player kills",
        position = 34,
        section = lootSection
    )
    default boolean includePlayerLoot() {
        return true;
    }

    @ConfigItem(
        keyName = "lootNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %LOOT% to insert the loot and %SOURCE% to show the source of the loot",
        position = 35,
        section = lootSection
    )
    default String lootNotifyMessage() {
        return "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%";
    }

    @ConfigItem(
        keyName = "deathEnabled",
        name = "Enable Death",
        description = "Enable notifications for when you die",
        position = 40,
        section = deathSection
    )
    default boolean notifyDeath() {
        return false;
    }

    @ConfigItem(
        keyName = "deathSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 41,
        section = deathSection
    )
    default boolean deathSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "deathNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %VALUELOST% to insert the GE value of the stuff you lost",
        position = 42,
        section = deathSection
    )
    default String deathNotifyMessage() {
        return "%USERNAME% has died...";
    }

    @ConfigItem(
        keyName = "deathNotifPvpEnabled",
        name = "Distinguish PvP deaths",
        description = "Should the plugin use a different message for dying in PvP?",
        position = 43,
        section = deathSection
    )
    default boolean deathNotifPvpEnabled() {
        return true;
    }

    @ConfigItem(
        keyName = "deathNotifPvpMessage",
        name = "PvP notification message",
        description = "The message to be sent through the webhook. Use %PKER% to insert the killer, %USERNAME% to insert your username, %VALUELOST% to insert the GE value of the stuff you lost",
        position = 44,
        section = deathSection
    )
    default String deathNotifPvpMessage() {
        return "%USERNAME% has just been PKed by %PKER% for %VALUELOST% gp...";
    }

    @ConfigItem(
        keyName = "slayerEnabled",
        name = "Enable Slayer",
        description = "Enable notifications for when you complete a slayer task",
        position = 50,
        section = slayerSection
    )
    default boolean notifySlayer() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 51,
        section = slayerSection
    )
    default boolean slayerSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "slayerPointThreshold",
        name = "Min Slayer Points",
        description = "The minimum slayer task points to warrant a notification",
        position = 52,
        section = slayerSection
    )
    default int slayerPointThreshold() {
        return 0;
    }

    @ConfigItem(
        keyName = "slayerNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %TASK% to insert your task, %POINTS% to show how many points you obtained and %TASKCOUNT% to show how many tasks you have completed.",
        position = 53,
        section = slayerSection
    )
    default String slayerNotifyMessage() {
        return "%USERNAME% has completed a slayer task: %TASK%, getting %POINTS% points and making that %TASKCOUNT% tasks completed";
    }

    @ConfigItem(
        keyName = "questEnabled",
        name = "Enable Quest",
        description = "Enable notifications for when you complete a quest",
        position = 60,
        section = questSection
    )
    default boolean notifyQuest() {
        return false;
    }

    @ConfigItem(
        keyName = "questSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 61,
        section = questSection
    )
    default boolean questSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "questNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username and %QUEST% to insert the quest that you completed",
        position = 62,
        section = questSection
    )
    default String questNotifyMessage() {
        return "%USERNAME% has completed a quest: %QUEST%";
    }

    @ConfigItem(
        keyName = "clueEnabled",
        name = "Enable Clue Scrolls",
        description = "Enable notifications for when you complete a clue scroll",
        position = 70,
        section = clueSection
    )
    default boolean notifyClue() {
        return false;
    }

    @ConfigItem(
        keyName = "clueSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 71,
        section = clueSection
    )
    default boolean clueSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "clueShowItems",
        name = "Show Item Icons",
        description = "Show item icons gained from the clue",
        position = 72,
        section = clueSection
    )
    default boolean clueShowItems() {
        return false;
    }

    @ConfigItem(
        keyName = "clueMinValue",
        name = "Min Value",
        description = "The minimum value of the combined items for a notification to be sent",
        position = 73,
        section = clueSection
    )
    default int clueMinValue() {
        return 0;
    }

    @ConfigItem(
        keyName = "clueNotifMessage",
        name = "Notification Message",
        description = "The message to be sent through the webhook. Use %USERNAME% to insert your username, %CLUE% to insert the clue type, %LOOT% to show the loot obtained and %COUNT% to insert how many of those clue types you have completed",
        position = 74,
        section = clueSection
    )
    default String clueNotifyMessage() {
        return "%USERNAME% has completed a %CLUE% clue, they have completed %COUNT%.\nThey obtained:\n\n%LOOT%";
    }

    @ConfigItem(
        keyName = "speedrunEnabled",
        name = "Enable speedruns",
        description = "Enable notifications for when you complete a speedrun",
        position = 80,
        section = speedrunSection
    )
    default boolean notifySpeedrun() {
        return false;
    }

    @ConfigItem(
        keyName = "speedrunSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 81,
        section = speedrunSection
    )
    default boolean speedrunSendImage() {
        return true;
    }

    @ConfigItem(
        keyName = "speedrunPBOnly",
        name = "Notify on Personal Best only",
        description = "Enable notifications only for your best runs",
        position = 82,
        section = speedrunSection
    )
    default boolean speedrunPBOnly() {
        return true;
    }

    @ConfigItem(
        keyName = "speedrunPBMessage",
        name = "PB message",
        description = "%USERNAME% to insert your username, %QUEST% to insert the quest name, %TIME% to insert your new time",
        position = 83,
        section = speedrunSection
    )
    default String speedrunPBMessage() {
        return "%USERNAME% has just beat their personal best in a speedrun of %QUEST% with a time of %TIME%";
    }

    @ConfigItem(
        keyName = "speedrunMessage",
        name = "Notification message",
        description = "%USERNAME% to insert your username, %QUEST% to insert the quest name, %TIME% to insert your new time, %BEST% to insert your PB",
        position = 84,
        section = speedrunSection
    )
    default String speedrunMessage() {
        return "%USERNAME% has just finished a speedrun of %QUEST% with a time of %TIME% (their PB is %BEST%)";
    }

    @ConfigItem(
        keyName = "killCountEnabled",
        name = "Enable Kill Count",
        description = "Enable notifications for boss kill count milestones",
        position = 90,
        section = killCountSection
    )
    default boolean notifyKillCount() {
        return false;
    }

    @ConfigItem(
        keyName = "killCountSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 91,
        section = killCountSection
    )
    default boolean killCountSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "killCountInitial",
        name = "Initial Boss Kill",
        description = "Notify on the first ever kill of any boss",
        position = 92,
        section = killCountSection
    )
    default boolean killCountNotifyInitial() {
        return true;
    }

    @ConfigItem(
        keyName = "killCountInterval",
        name = "Kill Count Interval",
        description = "Interval between when a notification should be sent",
        position = 93,
        section = killCountSection
    )
    default int killCountInterval() {
        return 50;
    }

    @ConfigItem(
        keyName = "killCountMessage",
        name = "Notification Message",
        description = "The message to be sent to the webhook. Use %USERNAME% to insert your username, %BOSS% to insert the NPC name, %COUNT% to insert the kill count",
        position = 94,
        section = killCountSection
    )
    default String killCountMessage() {
        return "%USERNAME% has defeated %BOSS% with a completion count of %COUNT%";
    }

    @ConfigItem(
        keyName = "combatTaskEnabled",
        name = "Enable Combat Tasks",
        description = "Enable notifications for combat achievements",
        position = 100,
        section = combatTaskSection
    )
    default boolean notifyCombatTask() {
        return false;
    }

    @ConfigItem(
        keyName = "combatTaskSendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 101,
        section = combatTaskSection
    )
    default boolean combatTaskSendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "combatTaskMinTier",
        name = "Min Tier",
        description = "Minimum combat achievement tier to warrant a notification",
        position = 102,
        section = combatTaskSection
    )
    default CombatAchievementTier minCombatAchievementTier() {
        return CombatAchievementTier.EASY;
    }

    @ConfigItem(
        keyName = "combatTaskMessage",
        name = "Notification Message",
        description = "The message to be sent to the webhook. Use %USERNAME% to insert your username, %TIER% to insert the task tier, %TASK% to insert the task name",
        position = 103,
        section = combatTaskSection
    )
    default String combatTaskMessage() {
        return "%USERNAME% has completed %TIER% combat task: %TASK%";
    }

    @ConfigItem(
        keyName = "diaryEnabled",
        name = "Enable Diary",
        description = "Enable notifications for achievement diary completions",
        position = 110,
        section = diarySection
    )
    default boolean notifyAchievementDiary() {
        return false;
    }

    @ConfigItem(
        keyName = "diarySendImage",
        name = "Send Image",
        description = "Send image with the notification",
        position = 111,
        section = diarySection
    )
    default boolean diarySendImage() {
        return false;
    }

    @ConfigItem(
        keyName = "diaryMinDifficulty",
        name = "Min Difficulty",
        description = "Minimum achievement diary difficulty to warrant a notification",
        position = 112,
        section = diarySection
    )
    default AchievementDiary.Difficulty minDiaryDifficulty() {
        return AchievementDiary.Difficulty.EASY;
    }

    @ConfigItem(
        keyName = "diaryMessage",
        name = "Notification Message",
        description = "The message to be sent to the webhook. Use %USERNAME% to insert your username, %DIFFICULTY% to insert the diary difficulty, %AREA% to insert the diary area, %TOTAL% to insert the total diaries completed",
        position = 113,
        section = diarySection
    )
    default String diaryNotifyMessage() {
        return "%USERNAME% has completed the %DIFFICULTY% %AREA% Achievement Diary, for a total of %TOTAL% diaries completed";
    }

}
