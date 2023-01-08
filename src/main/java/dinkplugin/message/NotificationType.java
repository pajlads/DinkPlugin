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
    SLAYER("Slayer Task", "slayerImage.png", WIKI_IMG_BASE_URL + "Slayer_icon.png"),
    SPEEDRUN("Quest Speedrunning", "speedrunImage.png", WIKI_IMG_BASE_URL + "Giant_stopwatch.png"),
    KILL_COUNT("Completion Count", "killCountImage.png", WIKI_IMG_BASE_URL + "Enchanted_gem.png"),
    COMBAT_ACHIEVEMENT("Combat Achievement", "combatTaskImage.png", WIKI_IMG_BASE_URL + "Combat_achievements.png"),
    ACHIEVEMENT_DIARY("Achievement Diary", "achievementDiaryImage.png", WIKI_IMG_BASE_URL + "Achievement_Diaries_icon.png");

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
