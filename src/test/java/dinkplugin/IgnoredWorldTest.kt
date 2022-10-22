package dinkplugin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import java.util.EnumSet;
import net.runelite.api.WorldType;

class IgnoredWorldTest {
    @ParameterizedTest(name = "World should not be ignored {0}")
    @ArgumentsSource(NonIgnoredWorldTypeProvider::class)
    fun `World should not be ignored`(worldType: Set<WorldType>) {
        assertFalse(DinkPlugin._isIgnoredWorld(worldType))
    }

    private class NonIgnoredWorldTypeProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
            Arguments.of(EnumSet.of(WorldType.MEMBERS)),
            Arguments.of(emptySet<WorldType>()),
            Arguments.of(EnumSet.of(WorldType.SKILL_TOTAL)),
            Arguments.of(EnumSet.of(WorldType.FRESH_START_WORLD)),
        )
    }

    @ParameterizedTest(name = "World should be ignored {0}")
    @ArgumentsSource(IgnoredWorldTypeProvider::class)
    fun `World should be ignored`(worldType: Set<WorldType>) {
        assertTrue(DinkPlugin._isIgnoredWorld(worldType))
    }

    private class IgnoredWorldTypeProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
            Arguments.of(EnumSet.of(WorldType.PVP_ARENA)),
            Arguments.of(EnumSet.of(WorldType.QUEST_SPEEDRUNNING)),
            Arguments.of(EnumSet.of(WorldType.NOSAVE_MODE)),
            Arguments.of(EnumSet.of(WorldType.TOURNAMENT_WORLD)),
            Arguments.of(EnumSet.of(WorldType.FRESH_START_WORLD, WorldType.NOSAVE_MODE)),
        )
    }
}
