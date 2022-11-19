package dinkplugin.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.runelite.api.Varbits.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AchievementDiariesTest {

    @Test
    @DisplayName("Test that all Achievement Diaries have been parsed")
    void testParsingSuccessful() {
        Map<Integer, AchievementDiaries.Diary> diaries = AchievementDiaries.INSTANCE.getDiaries();

        assertEquals(48, diaries.size());
        assertEquals(new AchievementDiaries.Diary(DIARY_VARROCK_EASY, "Varrock", AchievementDiaries.Difficulty.EASY), diaries.get(DIARY_VARROCK_EASY));
        assertEquals(new AchievementDiaries.Diary(DIARY_FALADOR_MEDIUM, "Falador", AchievementDiaries.Difficulty.MEDIUM), diaries.get(DIARY_FALADOR_MEDIUM));
        assertEquals(new AchievementDiaries.Diary(DIARY_DESERT_HARD, "Desert", AchievementDiaries.Difficulty.HARD), diaries.get(DIARY_DESERT_HARD));
        assertEquals(new AchievementDiaries.Diary(DIARY_KARAMJA_ELITE, "Karamja", AchievementDiaries.Difficulty.ELITE), diaries.get(DIARY_KARAMJA_ELITE));
        assertEquals(new AchievementDiaries.Diary(DIARY_KARAMJA_HARD, "Karamja", AchievementDiaries.Difficulty.HARD), diaries.get(DIARY_KARAMJA_HARD));
        assertNull(diaries.get(3610));
        assertNull(diaries.get(PRAYER_AUGURY));
        assertNull(diaries.get(KOUREND_FAVOR_ARCEUUS));
    }

}
