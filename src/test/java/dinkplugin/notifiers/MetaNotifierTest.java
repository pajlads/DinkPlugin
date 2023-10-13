package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LoginNotificationData;
import dinkplugin.notifiers.data.Progress;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    MetaNotifier notifier;

    int world = 420;
    int level = 50;
    long xp = Experience.getXpForLevel(level);
    int skillCount = Skill.values().length;
    String url = StringUtils.isNotBlank(PRIMARY_WEBHOOK_URL) ? PRIMARY_WEBHOOK_URL : "https://example.com";

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // update config mocks
        when(config.metadataWebhook()).thenReturn(url);

        // update client mocks
        when(client.getWorld()).thenReturn(world);

        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(1312);
        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(1477);

        when(client.getVarbitValue(CombatTaskNotifier.TOTAL_POINTS_ID)).thenReturn(1984);
        when(client.getVarbitValue(CombatTaskNotifier.GRANDMASTER_TOTAL_POINTS_ID)).thenReturn(2005);

        when(client.getVarbitValue(Varbits.DIARY_FALADOR_EASY)).thenReturn(1);
        when(client.getVarbitValue(Varbits.DIARY_VARROCK_EASY)).thenReturn(1);
        when(client.getVarbitValue(Varbits.DIARY_WILDERNESS_EASY)).thenReturn(1);

        when(client.getVarbitValue(Varbits.BA_GC)).thenReturn(666);

        when(client.getRealSkillLevel(any())).thenReturn(level);
        when(client.getTotalLevel()).thenReturn(skillCount * level);
        when(client.getOverallExperience()).thenReturn(skillCount * xp);

        when(client.getVarbitValue(QuestNotifier.COMPLETED_ID)).thenReturn(21);
        when(client.getVarbitValue(QuestNotifier.TOTAL_ID)).thenReturn(158);
        when(client.getVarpValue(VarPlayer.QUEST_POINTS)).thenReturn(43);
        when(client.getVarbitValue(QuestNotifier.QP_TOTAL_ID)).thenReturn(300);

        when(client.getVarbitValue(Varbits.SLAYER_POINTS)).thenReturn(2484);
        when(client.getVarbitValue(Varbits.SLAYER_TASK_STREAK)).thenReturn(300);

        // too lazy to mock script results, just return zero so excluded from serialization in tests
        when(client.getIntStack()).thenReturn(new int[1]);
    }

    @Test
    void testNotify() {
        // fire event
        notifier.onGameState(event(GameState.LOGGING_IN));
        notifier.onGameState(event(GameState.LOADING));
        notifier.onGameState(event(GameState.LOGGED_IN));
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // verify handled
        Map<String, Integer> levels = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> level));
        LoginNotificationData extra = new LoginNotificationData(world,
            Progress.of(1312, 1477),
            Progress.of(1984, 2005),
            Progress.of(3, 48),
            null,
            new LoginNotificationData.BarbarianAssault(666),
            new LoginNotificationData.SkillData(xp * skillCount, level * skillCount, levels),
            Progress.of(21, 158), Progress.of(43, 300),
            new LoginNotificationData.SlayerData(2484, 300)
        );
        verify(messageHandler).createMessage(
            url,
            false,
            NotificationBody.builder()
                .extra(extra)
                .text(buildTemplate(PLAYER_NAME + " logged into World " + world))
                .type(NotificationType.LOGIN)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyWithoutCollection() {
        // update client mock
        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(0);

        // fire events
        notifier.onGameState(event(GameState.LOGGING_IN));
        notifier.onGameState(event(GameState.LOADING));
        notifier.onGameState(event(GameState.LOGGED_IN));
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // verify handled
        Map<String, Integer> levels = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> level));
        LoginNotificationData extra = new LoginNotificationData(world,
            null, // collection log data should not be present
            Progress.of(1984, 2005),
            Progress.of(3, 48),
            null,
            new LoginNotificationData.BarbarianAssault(666),
            new LoginNotificationData.SkillData(xp * skillCount, level * skillCount, levels),
            Progress.of(21, 158), Progress.of(43, 300),
            new LoginNotificationData.SlayerData(2484, 300)
        );
        verify(messageHandler).createMessage(
            url,
            false,
            NotificationBody.builder()
                .extra(extra)
                .text(buildTemplate(PLAYER_NAME + " logged into World " + world))
                .type(NotificationType.LOGIN)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testDisabled() {
        // update config mock
        when(config.metadataWebhook()).thenReturn("");

        // fire event
        GameStateChanged event = new GameStateChanged();
        event.setGameState(GameState.LOGGED_IN);
        notifier.onGameState(event);
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static GameStateChanged event(GameState state) {
        GameStateChanged event = new GameStateChanged();
        event.setGameState(state);
        return event;
    }
}
