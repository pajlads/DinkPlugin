package dinkplugin.notifiers;

import dinkplugin.domain.AchievementDiaries;
import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CombatAchievementData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CombatTaskNotifierTest extends MockedNotifierTest {

    @InjectMocks
    CombatTaskNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyCombatTask()).thenReturn(true);
        when(config.combatTaskSendImage()).thenReturn(false);
        when(config.combatTaskMessage()).thenReturn("%USERNAME% has completed %TIER% combat task: %TASK%");
        when(config.minCombatAchievementTier()).thenReturn(CombatAchievementTier.HARD);
    }

    @Test
    void testNotify() {
        // send fake message
        notifier.onGameMessage("Congratulations, you've completed a hard combat task: Whack-a-Mole.");

        // verify handled
        verify(messageHandler).createMessage(
            URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed %s combat task: %s", PLAYER_NAME, AchievementDiaries.Difficulty.HARD, "Whack-a-Mole"))
                .extra(new CombatAchievementData(CombatAchievementTier.HARD, "Whack-a-Mole"))
                .playerName(PLAYER_NAME)
                .type(NotificationType.COMBAT_ACHIEVEMENT)
                .build()
        );
    }

    @Test
    void testSkipped() {
        // send too easy achievement
        notifier.onGameMessage("Congratulations, you've completed an easy combat task: A Slow Death.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnore() {
        // send unrelated message
        notifier.onGameMessage("Congratulations, you've completed a gachi combat task: Swordfight with the homies.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyCombatTask()).thenReturn(false);

        // send fake message
        notifier.onGameMessage("Congratulations, you've completed a hard combat task: Whack-a-Mole.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
