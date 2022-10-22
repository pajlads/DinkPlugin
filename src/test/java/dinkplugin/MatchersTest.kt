package dinkplugin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

class MatchersTest {
    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ArgumentsSource(SlayerTaskProvider::class)
    fun `Slayer task completion regex finds match`(message: String, task: String) {
        val matcher = DinkPlugin.SLAYER_TASK_REGEX.matcher(message)
        assertTrue(matcher.find())
        assertEquals(task, matcher.group("task"))
    }

    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ValueSource(
        strings = [
            "Forsen: forsen",
            "You're assigned to kill kalphite; only 3 more to go.",
            "You've completed 234 tasks and received 15 points, giving you a total of 801; return to a Slayer master.",
        ]
    )
    fun `Slayer task completion regex does not match`(message: String) {
        val matcher = DinkPlugin.SLAYER_TASK_REGEX.matcher(message)
        assertFalse(matcher.find())
    }

    private class SlayerTaskProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
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
        )
    }

    @ParameterizedTest(name = "Collection log message should trigger {0}")
    @ArgumentsSource(CollectionLogProvider::class)
    fun `Collection log regex finds match`(message: String, item: String) {
        val matcher = DinkPlugin.COLLECTION_LOG_REGEX.matcher(message)
        assertTrue(matcher.find())
        assertEquals(item, matcher.group("itemName"))
    }

    @ParameterizedTest(name = "Collection log message should not trigger {0}")
    @ValueSource(
        strings = [
            "Forsen: forsen", // todo: add more bad examples
        ]
    )
    fun `Collection log regex does not match`(message: String) {
        val matcher = DinkPlugin.COLLECTION_LOG_REGEX.matcher(message)
        assertFalse(matcher.find())
    }

    private class CollectionLogProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
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
        )
    }

    @ParameterizedTest(name = "Pet message should trigger {0}")
    @ValueSource(
        strings = [
            "You have a funny feeling like you're being followed",
            "You have a funny feeling like you would have been followed",
            "You feel something weird sneaking into your backpack",
        ]
    )
    fun `Pet regex finds match`(message: String) {
        val matcher = DinkPlugin.PET_REGEX.matcher(message)
        assertTrue(matcher.find())
    }

    @ParameterizedTest(name = "Pet message should not trigger {0}")
    @ValueSource(
        strings = [
            "Forsen: forsen",
            "You feel like you forgot to turn the stove off",
        ]
    )
    fun `Pet regex does not match`(message: String) {
        val matcher = DinkPlugin.PET_REGEX.matcher(message)
        assertFalse(matcher.find())
    }

}
