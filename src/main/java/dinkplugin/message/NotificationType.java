package dinkplugin.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    CLUE("Clue Scroll"),
    COLLECTION("Collection Log"),
    DEATH("Player Death"),
    LEVEL("Level Up"),
    LOOT("Loot Drop"),
    PET("Pet Obtained"),
    QUEST("Quest Completed"),
    SLAYER("Slayer Task"),
    SPEEDRUN("Quest Speedrunning"),
    KILL_COUNT("Completion Count"),
    COMBAT_ACHIEVEMENT("Combat Achievement"),
    ACHIEVEMENT_DIARY("Achievement Diary");

    private final String title;
}
