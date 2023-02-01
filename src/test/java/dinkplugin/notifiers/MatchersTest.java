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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchersTest {

    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ArgumentsSource(SlayerTaskProvider.class)
    void slayerTaskCompletionRegexFindsMatch(String message, String task) {
        Matcher matcher = SlayerNotifier.SLAYER_TASK_REGEX.matcher(message);
        assertTrue(matcher.find());
        assertEquals(task, matcher.group("task"));
    }

    @ParameterizedTest(name = "Slayer task completion message should not trigger {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "You're assigned to kill kalphite; only 3 more to go.",
            "You've completed 234 tasks and received 15 points, giving you a total of 801; return to a Slayer master."
        }
    )
    void slayerTaskCompletionRegexDoesNotMatch(String message) {
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
    void collectionLogRegexDoesNotMatch(String message) {
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

    @ParameterizedTest(name = "Kill count message should trigger on: {0}")
    @ArgumentsSource(KillCountProvider.class)
    void killCountRegexFindsMatch(String message, Pair<String, Integer> expected) {
        assertEquals(expected, KillCountNotifier.parseBoss(message).orElse(null));
    }

    @ParameterizedTest(name = "Kill count message should not trigger on: {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "Your heriboar harvest count is: 69."
        }
    )
    void killCountRegexDoesNotMatch(String message) {
        assertFalse(KillCountNotifier.parseBoss(message).isPresent());
    }

    private static class KillCountProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                // standard kill count string
                Arguments.of("Your King Black Dragon kill count is: 581.", Pair.of("King Black Dragon", 581)),
                Arguments.of("Your Cerberus kill count is: 3273.", Pair.of("Cerberus", 3273)),
                Arguments.of("Your K'ril Tsutsaroth kill count is: 481", Pair.of("K'ril Tsutsaroth", 481)),
                Arguments.of("Your TzTok-Jad kill count is: 46", Pair.of("TzTok-Jad", 46)),

                // skilling special case
                Arguments.of("Your subdued Wintertodt count is: 359", Pair.of("Wintertodt", 359)),

                // minigame special cases
                Arguments.of("Your Barrows chest count is: 268", Pair.of("Barrows", 268)),
                Arguments.of("Your Gauntlet completion count is: 8", Pair.of("Crystalline Hunllef", 8)),
                Arguments.of("Your Corrupted Gauntlet completion count is: 109", Pair.of("Corrupted Hunllef", 109)),

                // raid special cases
                Arguments.of("Your completed Theatre of Blood: Entry Mode count is: 1", Pair.of("Theatre of Blood: Entry Mode", 1)),
                Arguments.of("Your completed Theatre of Blood count is: 951", Pair.of("Theatre of Blood", 951)),
                Arguments.of("Your completed Theatre of Blood: Hard Mode count is: 2", Pair.of("Theatre of Blood: Hard Mode", 2)),
                Arguments.of("Your completed Chambers of Xeric count is: 138", Pair.of("Chambers of Xeric", 138)),
                Arguments.of("Your completed Chambers of Xeric Challenge Mode count is: 138", Pair.of("Chambers of Xeric Challenge Mode", 138)),
                Arguments.of("Your completed Tombs of Amascut: Entry Mode count is: 7", Pair.of("Tombs of Amascut: Entry Mode", 7)),
                Arguments.of("Your completed Tombs of Amascut count is: 101", Pair.of("Tombs of Amascut", 101)),
                Arguments.of("Your completed Tombs of Amascut: Expert Mode count is: 3", Pair.of("Tombs of Amascut: Expert Mode", 3))
            );
        }
    }

    @ParameterizedTest(name = "Gamble message should trigger {0}")
    @ArgumentsSource(GambleProvider.class)
    void gambleRegexFindsMatch(String message, GambleNotifier.ParsedData data) {
        assertEquals(data, GambleNotifier.parse(message));
    }

    @ParameterizedTest(name = "Gamble message should not trigger {0}")
    @ValueSource(
        strings = {
            "Coal (x 150)! Low level gamble count: 14.",
            "Rune full helm! Medium level gamble count: 3.",
            "High level gamble count: 100."
        }
    )
    void gambleRegexDoesNotMatch(String message) {
        assertNull(GambleNotifier.parse(message));
    }

    private static class GambleProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                Arguments.of("Magic seed! High level gamble count: 1.", new GambleNotifier.ParsedData("Magic seed", 1, null, 1)),
                Arguments.of("Limpwurt root (x 37)! High level gamble count: 65.", new GambleNotifier.ParsedData("Limpwurt root", 37, null, 65)),
                Arguments.of("Farseer helm! Clue scroll (elite)! High level gamble count: 774.", new GambleNotifier.ParsedData("Farseer helm", 1, "Clue scroll (elite)", 774)),
                Arguments.of("Law rune (x 270)! Clue scroll (elite)! High level gamble count: 1762.", new GambleNotifier.ParsedData("Law rune", 270, "Clue scroll (elite)", 1762))
            );
        }
    }

}
