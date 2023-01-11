package dinkplugin.notifiers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LevelNotificationData;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LevelNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    LevelNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyLevel()).thenReturn(true);
        when(config.levelSendImage()).thenReturn(false);
        when(config.levelInterval()).thenReturn(5);
        when(config.levelNotifyMessage()).thenReturn("%USERNAME% has levelled %SKILL%");

        // init base level
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 0, 1, 1));
        notifier.onStatChanged(new StatChanged(Skill.ATTACK, 14_000_000, 99, 99));
        notifier.onStatChanged(new StatChanged(Skill.HUNTER, 300, 4, 4));
    }

    @Test
    void testNotify() {
        // fire skill event
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5), ImmutableMap.of("Agility", 5, "Attack", 99, "Hunter", 4)))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyVirtual() {
        // fire skill event
        notifier.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Attack to 100")
                .extra(new LevelNotificationData(ImmutableMap.of("Attack", 100), ImmutableMap.of("Agility", 1, "Attack", 100, "Hunter", 4)))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyTwo() {
        // fire skill events
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));
        notifier.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5 and Attack to 100")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Attack", 100), ImmutableMap.of("Agility", 5, "Attack", 100, "Hunter", 4)))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyMany() {
        // fire skill events
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));
        notifier.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));
        notifier.onStatChanged(new StatChanged(Skill.HUNTER, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5, Attack to 100, and Hunter to 5")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Attack", 100, "Hunter", 5), ImmutableMap.of("Agility", 5, "Attack", 100, "Hunter", 5)))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testIgnoreInterval() {
        // fire skill event
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 100, 2, 2));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyLevel()).thenReturn(false);

        // fire skill event
        notifier.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
