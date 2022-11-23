package dinkplugin.notifiers;

import dinkplugin.domain.AchievementDiaries;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DiaryNotificationData;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("MagicConstant")
class DiaryNotifierTest extends MockedNotifierTest {

    @InjectMocks
    DiaryNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyAchievementDiary()).thenReturn(true);
        when(config.diarySendImage()).thenReturn(false);
        when(config.minDiaryDifficulty()).thenReturn(AchievementDiaries.Difficulty.MEDIUM);
        when(config.diaryNotifyMessage()).thenReturn("%USERNAME% has completed the %DIFFICULTY% %AREA% Diary, for a total of %TOTAL%");

        // init client mocks
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
    }

    @Test
    void testNotifyFirst() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary completion
        int id = Varbits.DIARY_DESERT_ELITE;
        notifier.onVarbitChanged(event(id, 1));

        // verify notification message
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed the %s %s Diary, for a total of %d", PLAYER_NAME, AchievementDiaries.Difficulty.ELITE, "Desert", 1))
                .extra(new DiaryNotificationData("Desert", AchievementDiaries.Difficulty.ELITE, 1))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyKaramja() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);
        int total = AchievementDiaries.INSTANCE.getDiaries().size() - 3;

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary started
        int id = Varbits.DIARY_KARAMJA_HARD;
        notifier.onVarbitChanged(event(id, 2));

        // verify notification message
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed the %s %s Diary, for a total of %d", PLAYER_NAME, AchievementDiaries.Difficulty.HARD, "Karamja", total + 1))
                .extra(new DiaryNotificationData("Karamja", AchievementDiaries.Difficulty.HARD, total + 1))
                .type(NotificationType.ACHIEVEMENT_DIARY)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testIgnoreKaramja() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger diary started
        int id = Varbits.DIARY_KARAMJA_HARD;
        notifier.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testIgnoreCompleted() {
        // initially many diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(1);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger varbit event
        int id = Varbits.DIARY_DESERT_ELITE;
        notifier.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testIgnoreUninitialized() {
        // trigger varbit event
        int id = Varbits.DIARY_DESERT_ELITE;
        notifier.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testIgnoreDifficulty() {
        // initially 0 diary completions
        when(client.getVarbitValue(anyInt())).thenReturn(0);

        // perform enough ticks to trigger diary initialization
        IntStream.range(0, 16).forEach(i -> notifier.onTick());

        // trigger varbit event
        int id = Varbits.DIARY_FALADOR_EASY;
        notifier.onVarbitChanged(event(id, 1));

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    private static VarbitChanged event(int id, int value) {
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(id);
        event.setValue(value);
        return event;
    }

}
