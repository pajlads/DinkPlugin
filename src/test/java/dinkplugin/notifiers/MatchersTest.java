package dinkplugin.notifiers;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchersTest {

    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ArgumentsSource(SlayerTaskProvider.class)
    void SlayerTaskCompletionRegexFindsMatch(String message, String task) {
        Matcher matcher = SlayerNotifier.SLAYER_TASK_REGEX.matcher(message);
        assertTrue(matcher.find());
        assertEquals(task, matcher.group("task"));
    }

    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "You're assigned to kill kalphite; only 3 more to go.",
            "You've completed 234 tasks and received 15 points, giving you a total of 801; return to a Slayer master."
        }
    )
    void SlayerTaskCompletionRegexDoesNotMatch(String message) {
        Matcher matcher = SlayerNotifier.SLAYER_TASK_REGEX.matcher(message);
        assertFalse(matcher.find());
    }

    private static class SlayerTaskProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(
                    "You have completed your task! You killed 125 Kalphite. You gained 11,150 xp.",
                    "125 Kalphite"
                ),
                Arguments.of("You have completed your task! You killed 7 Ankous. You gained 75 xp.", "7 Ankous"),
                Arguments.of(
                    "You have completed your task! You killed 134 Abyssal demons. You gained 75 xp.",
                    "134 Abyssal demons"
                ),
                Arguments.of(
                    "You have completed your task! You killed 134 Fossil Island Wyverns. You gained 75 xp.",
                    "134 Fossil Island Wyverns"
                ),
                Arguments.of("You have completed your task! You killed 31 Kree'Arra. You gained 75 xp.", "31 Kree'Arra"),
                Arguments.of("You have completed your task! You killed 31 TzKal-Zuk. You gained 75 xp.", "31 TzKal-Zuk")
            );
        }
    }

    @ParameterizedTest(name = "Collection log message should trigger {0}")
    @ArgumentsSource(CollectionLogProvider.class)
    void collectionLogRegexFindsMatch(String message, String item) {
        Matcher matcher = CollectionNotifier.COLLECTION_LOG_REGEX.matcher(message);
        assertTrue(matcher.find());
        assertEquals(item, matcher.group("itemName"));
    }

    @ParameterizedTest(name = "Collection log message should not trigger {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen" // todo: add more bad examples
        }
    )
    void CollectionLogRegexDoesNotMatch(String message) {
        Matcher matcher = CollectionNotifier.COLLECTION_LOG_REGEX.matcher(message);
        assertFalse(matcher.find());
    }

    private static class CollectionLogProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("New item added to your collection log: Red d'hide body (t)",
                    "Red d'hide body (t)"),
                Arguments.of("New item added to your collection log: Rune full helm (g)",
                    "Rune full helm (g)"),
                Arguments.of("New item added to your collection log: Robin hood hat",
                    "Robin hood hat"),
                Arguments.of("New item added to your collection log: Amulet of glory (t4)",
                    "Amulet of glory (t4)"),
                Arguments.of("New item added to your collection log: Blue d'hide chaps (t)",
                    "Blue d'hide chaps (t)"),
                Arguments.of("New item added to your collection log: Lumberjack boots",
                    "Lumberjack boots")
            );
        }
    }

    @ParameterizedTest(name = "Pet message should trigger {0}")
    @ValueSource(
        strings = {
            "You have a funny feeling like you're being followed.",
            "You have a funny feeling like you would have been followed...",
            "You feel something weird sneaking into your backpack."
        }
    )
    void PetRegexFindsMatch(String message) {
        Matcher matcher = PetNotifier.PET_REGEX.matcher(message);
        assertTrue(matcher.find());
    }

    @ParameterizedTest(name = "Pet message should not trigger {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "You feel like you forgot to turn the stove off"
        }
    )
    void PetRegexDoesNotMatch(String message) {
        Matcher matcher = PetNotifier.PET_REGEX.matcher(message);
        assertFalse(matcher.find());
    }

    @ParameterizedTest(name = "Kill count message should trigger on: {0}")
    @ArgumentsSource(KillCountProvider.class)
    void killCountRegexFindsMatch(String message, Pair<String, Integer> expected) {
        assertEquals(expected, KillCountNotifier.parse(message).orElse(null));
    }

    @ParameterizedTest(name = "Kill count message should not trigger on: {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "Your heriboar harvest count is: 69."
        }
    )
    void killCountRegexDoesNotMatch(String message) {
        assertFalse(KillCountNotifier.parse(message).isPresent());
    }

    private static class KillCountProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("Your King Black Dragon kill count is: 581.", Pair.of("King Black Dragon", 581)),
                Arguments.of("Your Cerberus kill count is: 3273.", Pair.of("Cerberus", 3273))
            );
        }
    }

}
