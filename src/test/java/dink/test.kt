package dink

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import universalDiscord.UniversalDiscordPlugin

class Matchers {
    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ValueSource(strings = [
        "You have completed your task! You killed 125 Kalphite. You gained 11,150 xp.",
        "You have completed your task! You killed 7 Ankous. You gained 75 xp.",
        "You have completed your task! You killed 134 Abyssal demons. You gained 75 xp.",
        "You have completed your task! You killed 134 Fossil Island Wyverns. You gained 75 xp.",
        "You have completed your task! You killed 31 Kree'Arra. You gained 75 xp.",
        "You have completed your task! You killed 31 TzKal-Zuk. You gained 75 xp.",
    ])
    fun `Slayer task completion regex finds match`(message: String) {
        var matcher = UniversalDiscordPlugin.SLAYER_TASK_REGEX.matcher(message);
        assertTrue(matcher.find())
    }

    @ParameterizedTest(name = "Slayer task completion message should trigger {0}")
    @ValueSource(strings = [
        "Forsen: forsen",
        "You're assigned to kill kalphite; only 3 more to go.",
        "You've completed 234 tasks and received 15 points, giving you a total of 801; return to a Slayer master.",
    ])
    fun `Slayer task completion regex does not match`(message: String) {
        var matcher = UniversalDiscordPlugin.SLAYER_TASK_REGEX.matcher(message);
        assertFalse(matcher.find())
    }
}
