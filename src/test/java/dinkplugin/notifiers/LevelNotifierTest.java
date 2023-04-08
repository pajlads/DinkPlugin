package dinkplugin.notifiers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LevelNotificationData;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;
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
    int initialCombatLevel;
    LevelNotificationData.CombatLevel unchangedCombatLevel;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyLevel()).thenReturn(true);
        when(config.levelSendImage()).thenReturn(false);
        when(config.levelNotifyVirtual()).thenReturn(true);
        when(config.levelNotifyCombat()).thenReturn(true);
        when(config.levelInterval()).thenReturn(5);
        when(config.levelNotifyMessage()).thenReturn("%USERNAME% has levelled %SKILL%");

        // init base level
        when(client.getRealSkillLevel(any())).thenReturn(1);
        when(client.getRealSkillLevel(Skill.ATTACK)).thenReturn(99);
        when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(10);
        initialCombatLevel = Experience.getCombatLevel(99, 1, 1, 10, 1, 1, 1);
        unchangedCombatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel, false);
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 0, 1, 1));
        plugin.onStatChanged(new StatChanged(Skill.ATTACK, 14_000_000, 99, 99));
        plugin.onStatChanged(new StatChanged(Skill.HITPOINTS, 1200, 10, 10));
        plugin.onStatChanged(new StatChanged(Skill.HUNTER, 300, 4, 4));
    }

    @Test
    void testNotify() {
        // fire skill event
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5), ImmutableMap.of("Agility", 5, "Attack", 99, "Hitpoints", 10, "Hunter", 4), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyJump() {
        // fire skill event (4 => 6, skipping 5 while 5 is level interval)
        plugin.onStatChanged(new StatChanged(Skill.HUNTER, 200, 6, 6));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Hunter to 6")
                .extra(new LevelNotificationData(ImmutableMap.of("Hunter", 6), ImmutableMap.of("Agility", 1, "Attack", 99, "Hitpoints", 10, "Hunter", 6), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyVirtual() {
        // fire skill event
        plugin.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Attack to 100")
                .extra(new LevelNotificationData(ImmutableMap.of("Attack", 100), ImmutableMap.of("Agility", 1, "Attack", 100, "Hitpoints", 10, "Hunter", 4), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyTwo() {
        // fire skill events
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));
        plugin.onStatChanged(new StatChanged(Skill.HUNTER, 14_000_000, 99, 99));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5 and Hunter to 99")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Hunter", 99), ImmutableMap.of("Agility", 5, "Attack", 99, "Hitpoints", 10, "Hunter", 99), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyMany() {
        // fire skill events
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));
        plugin.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));
        plugin.onStatChanged(new StatChanged(Skill.HUNTER, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Agility to 5, Attack to 100, and Hunter to 5")
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Attack", 100, "Hunter", 5), ImmutableMap.of("Agility", 5, "Attack", 100, "Hitpoints", 10, "Hunter", 5), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyCombat() {
        // update config mocks
        when(config.levelInterval()).thenReturn(18); // won't trigger on hp @ 13, will trigger on combat level @ 36

        // fire skill event
        when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(13);
        plugin.onStatChanged(new StatChanged(Skill.HITPOINTS, 2000, 13, 13));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        LevelNotificationData.CombatLevel combatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel + 1, true);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Combat to 36")
                .extra(new LevelNotificationData(Collections.emptyMap(), ImmutableMap.of("Agility", 1, "Attack", 99, "Hitpoints", 13, "Hunter", 4), combatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyTwoCombat() {
        // update config mocks
        when(config.levelInterval()).thenReturn(1);

        // fire skill event
        when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(13);
        plugin.onStatChanged(new StatChanged(Skill.HITPOINTS, 2000, 13, 13));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        LevelNotificationData.CombatLevel combatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel + 1, true);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has levelled Hitpoints to 13 and Combat to 36")
                .extra(new LevelNotificationData(ImmutableMap.of("Hitpoints", 13), ImmutableMap.of("Agility", 1, "Attack", 99, "Hitpoints", 13, "Hunter", 4), combatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testIgnoreInterval() {
        // fire skill event
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 100, 2, 2));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreVirtual() {
        // update config mock
        when(config.levelNotifyVirtual()).thenReturn(false);

        // fire skill event
        plugin.onStatChanged(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

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
        plugin.onStatChanged(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
