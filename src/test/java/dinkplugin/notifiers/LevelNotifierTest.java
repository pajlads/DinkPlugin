package dinkplugin.notifiers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LevelNotificationData;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
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
        when(config.levelIntervalOverride()).thenReturn(95);

        // init base level
        when(client.getRealSkillLevel(any())).thenReturn(1);
        mockLevel(Skill.ATTACK, 99);
        mockLevel(Skill.HITPOINTS, 10);
        mockLevel(Skill.HUNTER, 4);
        mockLevel(Skill.SLAYER, 96);
        initialCombatLevel = Experience.getCombatLevel(99, 1, 1, 10, 1, 1, 1);
        unchangedCombatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel, false);
        statChange(new StatChanged(Skill.AGILITY, 0, 1, 1));
        statChange(new StatChanged(Skill.ATTACK, 14_000_000, 99, 99));
        statChange(new StatChanged(Skill.HITPOINTS, 1200, 10, 10));
        statChange(new StatChanged(Skill.HUNTER, 300, 4, 4));
        statChange(new StatChanged(Skill.SLAYER, 9_800_000, 96, 96));
        notifier.onTick();
    }

    @Test
    void testNotify() {
        Map<String, Integer> expectedSkills = skillsMap("Agility", 5);
        int totalLevel = expectedSkills.values().stream().mapToInt(i -> i).sum();
        when(client.getTotalLevel()).thenReturn(totalLevel);
        when(config.levelNotifyMessage()).thenReturn("%USERNAME% has levelled %SKILL%, achieving a total level of %TOTAL_LEVEL%");

        // fire skill event
        statChange(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to 5, achieving a total level of {{total}}")
                        .replacement("{{skill}}", Replacements.ofWiki("Agility"))
                        .replacement("{{total}}", Replacements.ofText(String.valueOf(totalLevel)))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyJump() {
        Map<String, Integer> expectedSkills = skillsMap("Hunter", 6);

        // fire skill event (4 => 6, skipping 5 while 5 is level interval)
        statChange(new StatChanged(Skill.HUNTER, 200, 6, 6));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to 6")
                        .replacement("{{skill}}", Replacements.ofWiki("Hunter"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Hunter", 6), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyVirtual() {
        Map<String, Integer> expectedSkills = skillsMap("Attack", 100);

        // fire skill event
        statChange(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to 100")
                        .replacement("{{skill}}", Replacements.ofWiki("Attack"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Attack", 100), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyMaxExperience() {
        Map<String, Integer> expectedSkills = skillsMap("Hunter", 127);

        // fire skill event
        statChange(new StatChanged(Skill.HUNTER, 200_000_000, 99, 126));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to Max XP (200M)")
                        .replacement("{{skill}}", Replacements.ofWiki("Hunter"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Hunter", 127), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyTwo() {
        Map<String, Integer> expectedSkills = skillsMap(
            new String[] { "Agility", "Hunter" },
            new int[] { 5, 99 }
        );

        // fire skill events
        statChange(new StatChanged(Skill.AGILITY, 400, 5, 5));
        statChange(new StatChanged(Skill.HUNTER, 14_000_000, 99, 99));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{s1}} to 5 and {{s2}} to 99")
                        .replacement("{{s1}}", Replacements.ofWiki("Agility"))
                        .replacement("{{s2}}", Replacements.ofWiki("Hunter"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Hunter", 99), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyMany() {
        Map<String, Integer> expectedSkills = skillsMap(
            new String[] { "Agility", "Attack", "Hunter" },
            new int[] { 5, 100, 5 }
        );

        // fire skill events
        statChange(new StatChanged(Skill.AGILITY, 400, 5, 5));
        statChange(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));
        statChange(new StatChanged(Skill.HUNTER, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{s1}} to 5, {{s2}} to 100, and {{s3}} to 5")
                        .replacement("{{s1}}", Replacements.ofWiki("Agility"))
                        .replacement("{{s2}}", Replacements.ofWiki("Attack"))
                        .replacement("{{s3}}", Replacements.ofWiki("Hunter"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Agility", 5, "Attack", 100, "Hunter", 5), expectedSkills, unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyCombat() {
        Map<String, Integer> expectedSkills = skillsMap("Hitpoints", 13);

        // update config mocks
        when(config.levelInterval()).thenReturn(18); // won't trigger on hp @ 13, will trigger on combat level @ 36

        // fire skill event
        mockLevel(Skill.HITPOINTS, 13);
        statChange(new StatChanged(Skill.HITPOINTS, 2000, 13, 13));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        LevelNotificationData.CombatLevel combatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel + 1, true);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to 36")
                        .replacement("{{skill}}", Replacements.ofWiki("Combat", "Combat level"))
                        .build()
                )
                .extra(new LevelNotificationData(Collections.emptyMap(), expectedSkills, combatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testNotifyTwoCombat() {
        Map<String, Integer> expectedSkills = skillsMap("Hitpoints", 13);

        // update config mocks
        when(config.levelInterval()).thenReturn(1);

        // fire skill event
        mockLevel(Skill.HITPOINTS, 13);
        statChange(new StatChanged(Skill.HITPOINTS, 2000, 13, 13));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // verify handled
        LevelNotificationData.CombatLevel combatLevel = new LevelNotificationData.CombatLevel(initialCombatLevel + 1, true);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{s1}} to 13 and {{s2}} to 36")
                        .replacement("{{s1}}", Replacements.ofWiki("Hitpoints"))
                        .replacement("{{s2}}", Replacements.ofWiki("Combat", "Combat level"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Hitpoints", 13), expectedSkills, combatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    @DisplayName("Ensure the combat level notification isn't fired when notifyLevel is disabled")
    void testDisabledCombatLevel() {
        // update config mocks
        when(config.notifyLevel()).thenReturn(false);
        when(config.levelInterval())
            .thenReturn(
                18); // won't trigger on hp @ 13, will trigger on combat level @ 36

        // fire skill event
        mockLevel(Skill.HITPOINTS, 13);
        statChange(new StatChanged(Skill.HITPOINTS, 2000, 13, 13));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreInterval() {
        // fire skill event
        statChange(new StatChanged(Skill.AGILITY, 100, 2, 2));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testOverrideInterval() {
        // fire skill event
        statChange(new StatChanged(Skill.SLAYER, 10_695_000, 97, 97));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure a notification occurred
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has levelled {{skill}} to 97")
                        .replacement("{{skill}}", Replacements.ofWiki("Slayer"))
                        .build()
                )
                .extra(new LevelNotificationData(ImmutableMap.of("Slayer", 97), skillsMap("Slayer", 97), unchangedCombatLevel))
                .type(NotificationType.LEVEL)
                .build()
        );
    }

    @Test
    void testIgnoreVirtual() {
        // update config mock
        when(config.levelNotifyVirtual()).thenReturn(false);

        // fire skill event
        statChange(new StatChanged(Skill.ATTACK, 15_000_000, 99, 100));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreMaxExperience() {
        // update config mock
        when(config.levelNotifyVirtual()).thenReturn(false);

        // fire skill event
        statChange(new StatChanged(Skill.HUNTER, 200_000_000, 99, 126));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreAlreadyMaxExperience() {
        // update skill mocks
        Skill skill = Skill.CONSTRUCTION;
        when(client.getRealSkillLevel(skill)).thenReturn(99);
        when(client.getSkillExperience(skill)).thenReturn(Experience.MAX_SKILL_XP);
        when(config.levelNotifyVirtual()).thenReturn(false);
        statChange(new StatChanged(skill, Experience.MAX_SKILL_XP, 99, 126));
        when(config.levelNotifyVirtual()).thenReturn(true);

        // fire skill event
        statChange(new StatChanged(skill, 200_000_001, 99, 126));

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
        statChange(new StatChanged(Skill.AGILITY, 400, 5, 5));

        // let ticks pass
        IntStream.range(0, 4).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private void statChange(StatChanged event) {
        when(client.getRealSkillLevel(event.getSkill())).thenReturn(event.getLevel());
        plugin.onStatChanged(event);
    }

    private void mockLevel(Skill skill, int level) {
        when(client.getRealSkillLevel(skill)).thenReturn(level);
        when(client.getSkillExperience(skill)).thenReturn(Experience.getXpForLevel(level));
    }

    private Map<String, Integer> skillsMap(String skill, int level) {
        return skillsMap(new String[] { skill }, new int[] { level });
    }

    private Map<String, Integer> skillsMap(String[] skills, int[] updatedLevels) {
        Map<String, Integer> m = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, client::getRealSkillLevel));
        for (int i = 0; i < skills.length; i++) {
            m.put(skills[i], updatedLevels[i]);
        }
        return m;
    }
}
