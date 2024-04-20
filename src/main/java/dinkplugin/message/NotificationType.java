package dinkplugin.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static dinkplugin.util.Utils.WIKI_IMG_BASE_URL;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    CLUE("Clue Scroll", "clueImage.png", WIKI_IMG_BASE_URL + "Clue_scroll_%28Song_of_the_Elves%29.png"),
    COLLECTION("Collection Log", "collectionImage.png", WIKI_IMG_BASE_URL + "Collection_log.png"),
    DEATH("Player Death", "deathImage.png", WIKI_IMG_BASE_URL + "Items_kept_on_death.png"),
    LEVEL("Level Up", "levelImage.png", WIKI_IMG_BASE_URL + "Stats_icon.png"),
    LOOT("Loot Drop", "lootImage.png", WIKI_IMG_BASE_URL + "Rare_drop_table.png"),
    PET("Pet Obtained", "petImage.png", WIKI_IMG_BASE_URL + "Call_follower.png"),
    QUEST("Quest Completed", "questImage.png", WIKI_IMG_BASE_URL + "Quest_point_icon.png"),
    SLAYER("Slayer Task", "slayerImage.png", WIKI_IMG_BASE_URL + "Slayer_helmet_(i).png"),
    SPEEDRUN("Quest Speedrunning", "speedrunImage.png", WIKI_IMG_BASE_URL + "Giant_stopwatch.png"),
    KILL_COUNT("Completion Count", "killCountImage.png", WIKI_IMG_BASE_URL + "Enchanted_gem.png"),
    COMBAT_ACHIEVEMENT("Combat Achievement", "combatTaskImage.png", WIKI_IMG_BASE_URL + "Combat_achievements.png"),
    ACHIEVEMENT_DIARY("Achievement Diary", "achievementDiaryImage.png", WIKI_IMG_BASE_URL + "Achievement_Diaries_icon.png"),
    BARBARIAN_ASSAULT_GAMBLE("Barbarian Assault Gamble", "baGambleImage.png", WIKI_IMG_BASE_URL + "Barbarian_Assault_logo.jpg"),
    PLAYER_KILL("Player Kill", "playerKillImage.png", WIKI_IMG_BASE_URL + "Skull_(status)_icon.png"),
    GROUP_STORAGE("Group Shared Storage", "groupStorage.png", WIKI_IMG_BASE_URL + "Coins_10000.png"),
    GRAND_EXCHANGE("Grand Exchange", "grandExchange.png", WIKI_IMG_BASE_URL + "Grand_Exchange_icon.png"),
    LEAGUES_AREA("Area Unlocked", "leaguesArea.png", WIKI_IMG_BASE_URL + "Trailblazer_Reloaded_League_-_%3F_Relic.png"),
    LEAGUES_RELIC("Relic Chosen", "leaguesRelic.png", WIKI_IMG_BASE_URL + "Trailblazer_Reloaded_League_-_relics_icon.png"),
    LEAGUES_TASK("Task Completed", "leaguesTask.png", WIKI_IMG_BASE_URL + "Trailblazer_Reloaded_League_icon.png"),
    LOGIN("Player Login", "login.png", WIKI_IMG_BASE_URL + "Prop_sword.png"),
    TRADE("Player Trade", "trade.png", WIKI_IMG_BASE_URL + "Inventory.png"),
    CHAT("Chat Notification", "chat.png", WIKI_IMG_BASE_URL + "Toggle_Chat_effects.png"),
    XP_MILESTONE("XP Milestone", "xpImage.png", WIKI_IMG_BASE_URL + "Lamp.png");

    private final String title;

    /**
     * Name of the screenshot file
     */
    private final String screenshot;

    /**
     * Link to the notifier icon
     */
    private final String thumbnail;

}
