package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.DiaryNotificationData;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("MagicConstant")
class DiaryNotifierTest extends MockedNotifierTest {

    private static final int COMPLETED_TASKS = 469;
    private static final int TOTAL_TASKS = 492;

    private static final int KARAMJA_TASKS_FROM_EASY_TO_HARD = 10 + 19 + 10;
    private static final int KARAMJA_TOTAL_TASKS = KARAMJA_TASKS_FROM_EASY_TO_HARD + 5;

    @Bind
    @InjectMocks
    DiaryNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyAchievementDiary()).thenReturn(true);
        when(config.diarySendImage()).thenReturn(false);
        when(config.minDiaryDifficulty()).thenReturn(AchievementDiary.Difficulty.MEDIUM);
        when(config.diaryNotifyMessage()).thenReturn("%USERNAME% has completed the %DIFFICULTY% %AREA% Diary, for a total of %TOTAL%");

        // init client mocks
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getIntStack()).thenReturn(
            new int[] { COMPLETED_TASKS }, // first COMPLETED_TASKS_SCRIPT_ID is run
            new int[] { TOTAL_TASKS }, // then TOTAL_TASKS_SCRIPT_ID is run
            new int[] { KARAMJA_TASKS_FROM_EASY_TO_HARD }, // then COMPLETED_AREA_TASKS_SCRIPT_ID
            new int[] { KARAMJA_TOTAL_TASKS } // then TOTAL_AREA_TASKS_SCRIPT_ID
        );
    }

    @Test
    void testNotifyFirst() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);
        when(config.minDiaryDifficulty()).thenReturn(AchievementDiary.Difficulty.EASY);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        int tasksCompleted = 11;
        int totalAreaTasks = 11 + 12 + 10 + 6;
        when(client.getIntStack()).thenReturn(
            new int[] { tasksCompleted }, // first COMPLETED_TASKS_SCRIPT_ID is run
            new int[] { TOTAL_TASKS }, // then TOTAL_TASKS_SCRIPT_ID is run
            new int[] { tasksCompleted }, // then COMPLETED_AREA_TASKS_SCRIPT_ID is run
            new int[] { totalAreaTasks } // then TOTAL_TASKS_SCRIPT_ID is run
        );
        int id = Varbits.DIARY_DESERT_EASY;
        plugin.onVarbitChanged(event(id, 1));

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(AchievementDiary.Difficulty.EASY, "Desert", 1))
                .extra(new DiaryNotificationData("Desert", AchievementDiary.Difficulty.EASY, 1, tasksCompleted, TOTAL_TASKS, tasksCompleted, totalAreaTasks))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyKaramja() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);
        int total = AchievementDiary.DIARIES.size() - 3;

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary started
        int id = Varbits.DIARY_KARAMJA_HARD;
        plugin.onVarbitChanged(event(id, 2));

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(AchievementDiary.Difficulty.HARD, "Karamja", total + 1))
                .extra(new DiaryNotificationData("Karamja", AchievementDiary.Difficulty.HARD, total + 1, COMPLETED_TASKS, TOTAL_TASKS, KARAMJA_TASKS_FROM_EASY_TO_HARD, KARAMJA_TOTAL_TASKS))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyKaramjaMessageBox() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);
        int total = AchievementDiary.DIARIES.size() - 3;

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        notifier.onMessageBox("Congratulations! You have completed all of the hard tasks in the Karamja area. Speak to Pirate Jackie the Fruit to claim your reward.");

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(AchievementDiary.Difficulty.HARD, "Karamja", total + 1))
                .extra(new DiaryNotificationData("Karamja", AchievementDiary.Difficulty.HARD, total + 1, COMPLETED_TASKS, TOTAL_TASKS, KARAMJA_TASKS_FROM_EASY_TO_HARD, KARAMJA_TOTAL_TASKS))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testDontNotifyKaramjaMessageBox() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        notifier.onMessageBox("Congratulations! You have completed all of the easy tasks in the Karamja area. Speak to Pirate Jackie the Fruit to claim your reward.");

        // verify no notification message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyWesternMessageBox() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);
        when(client.getVarbitValue(Varbits.DIARY_WESTERN_HARD)).thenReturn(0);
        int total = AchievementDiary.DIARIES.size() - 3 - 1;

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        int westernTasksFromEasyToHard = 11 + 13 + 13;
        int westernTasksTotal = westernTasksFromEasyToHard + 7;
        when(client.getIntStack()).thenReturn(
            new int[] { COMPLETED_TASKS }, // first COMPLETED_TASKS_SCRIPT_ID is run
            new int[] { TOTAL_TASKS }, // then TOTAL_TASKS_SCRIPT_ID is run
            new int[] { westernTasksFromEasyToHard }, // then COMPLETED_AREA_TASKS_SCRIPT_ID
            new int[] { westernTasksTotal } // then TOTAL_AREA_TASKS_SCRIPT_ID
        );
        notifier.onMessageBox("Congratulations! You have completed all of the hard tasks in the Western Province area. Speak to the Elder Gnome child at the Gnome Stronghold to claim your reward.");

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(AchievementDiary.Difficulty.HARD, "Western Provinces", total + 1))
                .extra(new DiaryNotificationData("Western Provinces", AchievementDiary.Difficulty.HARD, total + 1, COMPLETED_TASKS, TOTAL_TASKS, westernTasksFromEasyToHard, westernTasksTotal))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyCooldown() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);
        int total = AchievementDiary.DIARIES.size() - 3;

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        int id = Varbits.DIARY_KARAMJA_HARD;
        plugin.onVarbitChanged(event(id, 2));

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(AchievementDiary.Difficulty.HARD, "Karamja", total + 1))
                .extra(new DiaryNotificationData("Karamja", AchievementDiary.Difficulty.HARD, total + 1, COMPLETED_TASKS, TOTAL_TASKS, KARAMJA_TASKS_FROM_EASY_TO_HARD, KARAMJA_TOTAL_TASKS))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );

        // trigger message box
        notifier.onMessageBox("Congratulations! You have completed all of the hard tasks in the Karamja area. Speak to Pirate Jackie the Fruit to claim your reward.");

        // ensure no notification
        Mockito.verifyNoMoreInteractions(messageHandler);
    }

    @Test
    void testIgnoreKaramja() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary started
        int id = Varbits.DIARY_KARAMJA_HARD;
        plugin.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreCompleted() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger varbit event
        int id = Varbits.DIARY_DESERT_ELITE;
        plugin.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreUninitialized() {
        // trigger varbit event
        int id = Varbits.DIARY_DESERT_ELITE;
        plugin.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreDifficulty() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger varbit event
        int id = Varbits.DIARY_FALADOR_EASY;
        plugin.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyAchievementDiary()).thenReturn(false);

        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        int id = Varbits.DIARY_DESERT_ELITE;
        plugin.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static VarbitChanged event(int id, int value) {
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(id);
        event.setValue(value);
        return event;
    }

    private static Template buildTemplate(AchievementDiary.Difficulty difficulty, String area, int total) {
        return Template.builder()
            .template(String.format("%s has completed the %s {{area}} Diary, for a total of %d", PLAYER_NAME, difficulty, total))
            .replacement("{{area}}", Replacements.ofWiki(area, area + " Diary"))
            .build();
    }
}
