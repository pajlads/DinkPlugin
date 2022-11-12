package dinkplugin.domain;

import net.runelite.api.Varbits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AchievementDiariesTest {

    @Test
    @DisplayName("Test that all Achievement Diaries have been parsed")
    void testParsingSuccessful() {
        Map<Integer, AchievementDiaries.Diary> diaries = AchievementDiaries.INSTANCE.getDiaries();

        assertEquals(48, diaries.size());
        assertEquals(new AchievementDiaries.Diary(4485, "Desert", AchievementDiaries.Difficulty.HARD), diaries.get(4485));
        assertEquals(new AchievementDiaries.Diary(4566, "Karamja", AchievementDiaries.Difficulty.ELITE), diaries.get(4566));
        assertEquals(new AchievementDiaries.Diary(3610, "Karamja", AchievementDiaries.Difficulty.HARD), diaries.get(3610));
        assertNull(diaries.get(3611));
        assertNull(diaries.get(Varbits.PRAYER_AUGURY));
        assertNull(diaries.get(Varbits.KOUREND_FAVOR_ARCEUUS));
    }

}
