package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.AccountType;
import dinkplugin.domain.LeagueRelicTier;
import dinkplugin.domain.LeagueTaskDifficulty;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LeaguesAreaNotificationData;
import dinkplugin.notifiers.data.LeaguesRelicNotificationData;
import dinkplugin.notifiers.data.LeaguesTaskNotificationData;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled; // unused when there's an active leagues
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.EnumSet;

import static dinkplugin.notifiers.LeaguesNotifier.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaguesNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    LeaguesNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // client mocks
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));
        when(client.getVarbitValue(Varbits.ACCOUNT_TYPE)).thenReturn(AccountType.IRONMAN.ordinal());
        when(client.getVarbitValue(LeaguesNotifier.LEAGUES_VERSION)).thenReturn(LeaguesNotifier.CURRENT_LEAGUE_VERSION);

        // config mocks
        when(config.notifyLeagues()).thenReturn(true);
        when(config.leaguesAreaUnlock()).thenReturn(true);
        when(config.leaguesRelicUnlock()).thenReturn(true);
        when(config.leaguesTaskCompletion()).thenReturn(true);
        when(config.leaguesTaskMinTier()).thenReturn(LeagueTaskDifficulty.HARD);
    }

    @Test
    void notifyArea() {
        // update client mocks
        int tasksCompleted = 200;
        int totalPoints = 100 * 10 + 100 * 40;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);
        when(client.getVarbitValue(LeaguesNotifier.TWO_AREAS)).thenReturn(2);
        when(client.getVarbitValue(LeaguesNotifier.THREE_AREAS)).thenReturn(4);
        when(client.getVarbitValue(LeaguesNotifier.FOUR_AREAS)).thenReturn(8);

        // fire event
        notifier.onGameMessage("Congratulations, you've unlocked a new area: Kandarin.");

        // verify notification
        String area = "Kandarin";
        int tasksUntilNextArea = THIRD_AREA_TASKS - tasksCompleted;
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_AREA)
                .text(
                    Template.builder()
                        .template(String.format("%s selected their second region: {{area}}.", PLAYER_NAME))
                        .replacement("{{area}}", Replacements.ofWiki(area, CURRENT_LEAGUE_NAME + " League/Areas/" + area))
                        .build()
                )
                .extra(new LeaguesAreaNotificationData(area, 2, tasksCompleted, tasksUntilNextArea))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void notifyAreaKaramja() {
        // update client mocks
        int tasksCompleted = 2;
        int totalPoints = 2 * 10;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);
        when(client.getVarbitValue(LeaguesNotifier.TWO_AREAS)).thenReturn(2);

        // fire event
        notifier.onGameMessage("Congratulations, you've unlocked a new area: Karamja.");

        // verify notification
        String area = "Karamja";
        int tasksUntilNextArea = FIRST_AREA_TASKS - tasksCompleted;
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_AREA)
                .text(
                    Template.builder()
                        .template(String.format("%s selected their zeroth region: {{area}}.", PLAYER_NAME))
                        .replacement("{{area}}", Replacements.ofWiki(area, CURRENT_LEAGUE_NAME + " League/Areas/" + area))
                        .build()
                )
                .extra(new LeaguesAreaNotificationData(area, 0, tasksCompleted, tasksUntilNextArea))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void notifyRelic() {
        // update client mocks
        int tasksCompleted = 2;
        int totalPoints = 2 * 10;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);

        // fire event
        notifier.onGameMessage("Congratulations, you've unlocked a new Relic: Animal Wrangler.");

        // verify notification
        String relic = "Animal Wrangler";
        int pointsUntilNextTier = LeagueRelicTier.TWO.getDefaultPoints() - totalPoints;
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_RELIC)
                .text(
                    Template.builder()
                        .template(String.format("%s unlocked a Tier 1 Relic: {{relic}}.", PLAYER_NAME))
                        .replacement("{{relic}}", Replacements.ofWiki(relic))
                        .build()
                )
                .extra(new LeaguesRelicNotificationData(relic, 1, 0, totalPoints, pointsUntilNextTier))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void notifyTask() {
        // update client mocks
        int tasksCompleted = 101;
        int totalPoints = 100 * 10 + 80;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard task: The Frozen Door.");

        // verify notification
        String taskName = "The Frozen Door";
        LeagueTaskDifficulty difficulty = LeagueTaskDifficulty.HARD;
        int tasksUntilNextArea = SECOND_AREA_TASKS - tasksCompleted;
        int pointsUntilNextRelic = LeagueRelicTier.THREE.getDefaultPoints() - totalPoints;
        int pointsUntilNextTrophy = 2_500 - totalPoints;
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_TASK)
                .text(
                    Template.builder()
                        .template(String.format("%s completed a %s task: {{task}}.", PLAYER_NAME, "Hard"))
                        .replacement("{{task}}", Replacements.ofWiki(taskName, CURRENT_LEAGUE_NAME + " League/Tasks"))
                        .build()
                )
                .extra(new LeaguesTaskNotificationData(taskName, difficulty, difficulty.getPoints(), totalPoints, tasksCompleted, tasksUntilNextArea, pointsUntilNextRelic, pointsUntilNextTrophy, null))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void notifyTaskTrophyBronze() {
        // update client mocks
        int tasksCompleted = 119;
        int totalPoints = 100 * 10 + 80 * 19; // 2520 >= 2500
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard task: The Frozen Door.");

        // verify notification
        String taskName = "The Frozen Door";
        LeagueTaskDifficulty difficulty = LeagueTaskDifficulty.HARD;
        int tasksUntilNextArea = SECOND_AREA_TASKS - tasksCompleted;
        int pointsUntilNextRelic = LeagueRelicTier.FIVE.getDefaultPoints() - totalPoints;
        int pointsUntilNextTrophy = 5_000 - totalPoints;
        String trophy = "Bronze";
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_TASK)
                .text(
                    Template.builder()
                        .template(String.format("%s completed a %s task, {{task}}, unlocking the {{trophy}} trophy!", PLAYER_NAME, "Hard"))
                        .replacement("{{task}}", Replacements.ofWiki(taskName, CURRENT_LEAGUE_NAME + " League/Tasks"))
                        .replacement("{{trophy}}", Replacements.ofWiki(trophy, CURRENT_LEAGUE_NAME + " " + trophy.toLowerCase() + " trophy"))
                        .build()
                )
                .extra(new LeaguesTaskNotificationData(taskName, difficulty, difficulty.getPoints(), totalPoints, tasksCompleted, tasksUntilNextArea, pointsUntilNextRelic, pointsUntilNextTrophy, trophy))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void notifyTaskTrophyIron() {
        // update mocks
        int tasksCompleted = 200;
        int totalPoints = 100 * 10 + 100 * 40; // 5000 >= 5000
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);
        when(config.leaguesTaskMinTier()).thenReturn(LeagueTaskDifficulty.EASY);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a medium task: Equip Amy's Saw.");

        // verify notification
        String taskName = "Equip Amy's Saw";
        LeagueTaskDifficulty difficulty = LeagueTaskDifficulty.MEDIUM;
        int tasksUntilNextArea = THIRD_AREA_TASKS - tasksCompleted;
        int pointsUntilNextRelic = LeagueRelicTier.SIX.getDefaultPoints() - totalPoints;
        int pointsUntilNextTrophy = 10_000 - totalPoints;
        String trophy = "Iron";
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.LEAGUES_TASK)
                .text(
                    Template.builder()
                        .template(String.format("%s completed a %s task, {{task}}, unlocking the {{trophy}} trophy!", PLAYER_NAME, "Medium"))
                        .replacement("{{task}}", Replacements.ofWiki(taskName, CURRENT_LEAGUE_NAME + " League/Tasks"))
                        .replacement("{{trophy}}", Replacements.ofWiki(trophy, CURRENT_LEAGUE_NAME + " " + trophy.toLowerCase() + " trophy"))
                        .build()
                )
                .extra(new LeaguesTaskNotificationData(taskName, difficulty, difficulty.getPoints(), totalPoints, tasksCompleted, tasksUntilNextArea, pointsUntilNextRelic, pointsUntilNextTrophy, trophy))
                .playerName(PLAYER_NAME)
                .seasonalWorld(true)
                .build()
        );
    }

    @Test
    void ignoreTaskTier() {
        // update config mock
        when(config.leaguesTaskMinTier()).thenReturn(LeagueTaskDifficulty.ELITE);

        // update client mocks
        int tasksCompleted = 101;
        int totalPoints = 100 * 10 + 40;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard task: The Frozen Door.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnored() {
        // update config mocks
        when(config.leaguesAreaUnlock()).thenReturn(false);
        when(config.leaguesRelicUnlock()).thenReturn(false);
        when(config.leaguesTaskCompletion()).thenReturn(false);

        // update client mocks
        int tasksCompleted = 101;
        int totalPoints = 100 * 10 + 80;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);
        when(client.getVarbitValue(LeaguesNotifier.TWO_AREAS)).thenReturn(2);
        when(client.getVarbitValue(LeaguesNotifier.THREE_AREAS)).thenReturn(4);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard task: The Frozen Door.");
        notifier.onGameMessage("Congratulations, you've unlocked a new Relic: Animal Wrangler.");
        notifier.onGameMessage("Congratulations, you've unlocked a new area: Kandarin.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // update config mocks
        when(config.notifyLeagues()).thenReturn(false);

        // update client mocks
        int tasksCompleted = 101;
        int totalPoints = 100 * 10 + 80;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);
        when(client.getVarbitValue(LeaguesNotifier.TWO_AREAS)).thenReturn(2);
        when(client.getVarbitValue(LeaguesNotifier.THREE_AREAS)).thenReturn(4);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard task: The Frozen Door.");
        notifier.onGameMessage("Congratulations, you've unlocked a new Relic: Animal Wrangler.");
        notifier.onGameMessage("Congratulations, you've unlocked a new area: Kandarin.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void ignoreIrrelevant() {
        // update client mocks
        int tasksCompleted = 101;
        int totalPoints = 100 * 10 + 80;
        when(client.getVarbitValue(LeaguesNotifier.TASKS_COMPLETED_ID)).thenReturn(tasksCompleted);
        when(client.getVarpValue(LeaguesNotifier.POINTS_EARNED_ID)).thenReturn(totalPoints);

        // fire event
        notifier.onGameMessage("Congratulations, you've completed a hard combat task: Ready to Pounce.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }
}
