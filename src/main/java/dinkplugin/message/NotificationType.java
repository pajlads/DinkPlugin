package dinkplugin.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    CLUE("Clue Scroll", "clueImage.png", "https://oldschool.runescape.wiki/images/Treasure_Trails_logo.jpg"),
    COLLECTION("Collection Log", "collectionImage.png", "https://oldschool.runescape.wiki/images/Collection_log_detail.png"),
    DEATH("Player Death", "deathImage.png", "https://oldschool.runescape.wiki/images/Death.png"),
    LEVEL("Level Up", "levelImage.png", "https://oldschool.runescape.wiki/images/Stats_icon.png"),
    LOOT("Loot Drop", "lootImage.png", "https://oldschool.runescape.wiki/images/Rare_drop_table.png"),
    PET("Pet Obtained", "petImage.png", "https://oldschool.runescape.wiki/images/Pet_list_icon.png"),
    QUEST("Quest Completed", "questImage.png", "https://oldschool.runescape.wiki/images/Quests.png"),
    SLAYER("Slayer Task", "slayerImage.png", "https://oldschool.runescape.wiki/images/Slayer_icon_%28detail%29.png"),
    SPEEDRUN("Quest Speedrunning", "speedrunImage.png", "https://oldschool.runescape.wiki/images/Quest_Speedrunning_logo.png"),
    KILL_COUNT("Completion Count", "killCountImage.png", "https://oldschool.runescape.wiki/images/Enchanted_gem_detail.png"),
    COMBAT_ACHIEVEMENT("Combat Achievement", "combatTaskImage.png", "https://oldschool.runescape.wiki/images/Combat_achievements_detail.png"),
    ACHIEVEMENT_DIARY("Achievement Diary", "achievementDiaryImage.png", "https://oldschool.runescape.wiki/images/Achievement_Diaries.png");

    private final String title;
    private final String screenshot;
    private final String thumbnail;
}
