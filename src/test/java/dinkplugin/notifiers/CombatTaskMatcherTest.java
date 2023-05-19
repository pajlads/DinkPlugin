package dinkplugin.notifiers;

import dinkplugin.domain.CombatAchievementTier;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static dinkplugin.domain.CombatAchievementTier.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CombatTaskMatcherTest {

    @ParameterizedTest(name = "Combat Achievement regex should match: {0}")
    @ArgumentsSource(AchievementProvider.class)
    void shouldParse(String message, Pair<CombatAchievementTier, String> expected) {
        assertEquals(expected, CombatTaskNotifier.parse(message).orElse(null));
    }

    @ParameterizedTest(name = "Combat Achievement regex should not match: {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "Your heriboar harvest count is: 69.",
            "Your King Black Dragon kill count is: 581.",
            "You have completed your task! You killed 31 TzKal-Zuk. You gained 75 xp.",
            "Congratulations, you just advanced a Strength level.",
            "Congratulations, you've completed a gachi combat task: Swordfight with the homies."
        }
    )
    void shouldNotParse(String message) {
        assertFalse(CombatTaskNotifier.parse(message).isPresent());
    }

    private static class AchievementProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(
                    "Congratulations, you've completed an easy combat task: Defence? What Defence?.",
                    Pair.of(EASY, "Defence? What Defence?")
                ),
                Arguments.of(
                    "Congratulations, you've completed an easy combat task: A Slow Death.",
                    Pair.of(EASY, "A Slow Death")
                ),
                Arguments.of(
                    "Congratulations, you've completed a medium combat task: Fire in the Hole!.",
                    Pair.of(MEDIUM, "Fire in the Hole!")
                ),
                Arguments.of(
                    "Congratulations, you've completed a medium combat task: I'd rather Be Illiterate.",
                    Pair.of(MEDIUM, "I'd rather Be Illiterate")
                ),
                Arguments.of(
                    "Congratulations, you've completed a hard combat task: Whack-a-Mole.",
                    Pair.of(HARD, "Whack-a-Mole")
                ),
                Arguments.of(
                    "Congratulations, you've completed a master combat task: Nibblers, Begone!.",
                    Pair.of(MASTER, "Nibblers, Begone!")
                ),
                Arguments.of(
                    "Congratulations, you've completed a grandmaster combat task: Inferno Grandmaster.",
                    Pair.of(GRANDMASTER, "Inferno Grandmaster")
                ),
                Arguments.of(
                    "Congratulations, you've completed a hard combat task: I Can't Reach That (3 points).",
                    Pair.of(HARD, "I Can't Reach That")
                )
            );
        }
    }

}
