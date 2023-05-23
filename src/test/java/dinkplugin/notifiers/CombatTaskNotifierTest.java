package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
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

    @Bind
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
        when(config.combatTaskUnlockMessage()).thenReturn("%USERNAME% has unlocked the rewards for the %COMPLETED% tier, by completing the combat task: %TASK%");
        when(config.minCombatAchievementTier()).thenReturn(CombatAchievementTier.HARD);

        // init client mocks
        when(client.getVarbitValue(
            CombatTaskNotifier.CUM_POINTS_VARBIT_BY_TIER.get(CombatAchievementTier.EASY)
        )).thenReturn(33);
        when(client.getVarbitValue(
            CombatTaskNotifier.CUM_POINTS_VARBIT_BY_TIER.get(CombatAchievementTier.MEDIUM)
        )).thenReturn(115);
        when(client.getVarbitValue(
            CombatTaskNotifier.CUM_POINTS_VARBIT_BY_TIER.get(CombatAchievementTier.HARD)
        )).thenReturn(304);
        when(client.getVarbitValue(
            CombatTaskNotifier.CUM_POINTS_VARBIT_BY_TIER.get(CombatAchievementTier.MASTER)
        )).thenReturn(1465);
        when(client.getVarbitValue(
            CombatTaskNotifier.CUM_POINTS_VARBIT_BY_TIER.get(CombatAchievementTier.GRANDMASTER)
        )).thenReturn(2005);
    }

    @Test
    void testNotify() {
        // update mock
        when(client.getVarbitValue(CombatTaskNotifier.TOTAL_POINTS_ID)).thenReturn(200);

        // send fake message
        notifier.onTick();
        notifier.onGameMessage("Congratulations, you've completed a hard combat task: Whack-a-Mole.");

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has completed %s combat task: {{task}}", PLAYER_NAME, "Hard"))
                        .replacement("{{task}}", Replacements.ofWiki("Whack-a-Mole"))
                        .build()
                )
                .extra(new CombatAchievementData(CombatAchievementTier.HARD, "Whack-a-Mole", 3, 200, 85, 189, null))
                .playerName(PLAYER_NAME)
                .type(NotificationType.COMBAT_ACHIEVEMENT)
                .build()
        );
    }

    @Test
    void testNotifyUnlock() {
        // init thresholds
        notifier.onTick();

        // calculate points
        int oldPoints = 1460;
        CombatAchievementTier taskTier = CombatAchievementTier.GRANDMASTER;
        int newPoints = oldPoints + taskTier.getPoints();
        when(client.getVarbitValue(CombatTaskNotifier.TOTAL_POINTS_ID)).thenReturn(newPoints);

        // fire completion message
        notifier.onGameMessage("Congratulations, you've completed a grandmaster combat task: No Pressure (6 points).");

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildUnlockTemplate("Master", "No Pressure"))
                .extra(new CombatAchievementData(CombatAchievementTier.GRANDMASTER, "No Pressure", 6, 1466, 1466 - 1465, 2005 - 1465, CombatAchievementTier.MASTER))
                .playerName(PLAYER_NAME)
                .type(NotificationType.COMBAT_ACHIEVEMENT)
                .build()
        );
    }

    @Test
    void testNotifyUnlockGrand() {
        // init thresholds
        notifier.onTick();

        // calculate points
        int oldPoints = 1999;
        CombatAchievementTier taskTier = CombatAchievementTier.GRANDMASTER;
        int newPoints = oldPoints + taskTier.getPoints();
        when(client.getVarbitValue(CombatTaskNotifier.TOTAL_POINTS_ID)).thenReturn(newPoints);

        // fire completion message
        notifier.onGameMessage("Congratulations, you've completed a grandmaster combat task: No Pressure (6 points).");

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildUnlockTemplate("Grandmaster", "No Pressure"))
                .extra(new CombatAchievementData(CombatAchievementTier.GRANDMASTER, "No Pressure", 6, 2005, null, null, CombatAchievementTier.GRANDMASTER))
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

    private static Template buildUnlockTemplate(String tier, String task) {
        return Template.builder()
            .template(String.format("%s has unlocked the rewards for the %s tier, by completing the combat task: {{task}}", PLAYER_NAME, tier))
            .replacement("{{task}}", Replacements.ofWiki(task))
            .build();
    }
}
