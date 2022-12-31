package dinkplugin;

import dinkplugin.util.WorldUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Set;
import java.util.stream.Stream;
import java.util.EnumSet;

import net.runelite.api.WorldType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoredWorldTest {
    @ParameterizedTest(name = "World should not be ignored {0}")
    @ArgumentsSource(NonIgnoredWorldTypeProvider.class)
    void worldShouldNotBeIgnored(Set<WorldType> worldType) {
        assertFalse(WorldUtils.isIgnoredWorld(worldType));
    }

    private static class NonIgnoredWorldTypeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(EnumSet.of(WorldType.MEMBERS)),
                Arguments.of(EnumSet.noneOf(WorldType.class)),
                Arguments.of(EnumSet.of(WorldType.SKILL_TOTAL)),
                Arguments.of(EnumSet.of(WorldType.FRESH_START_WORLD))
            );
        }
    }

    @ParameterizedTest(name = "World should be ignored {0}")
    @ArgumentsSource(IgnoredWorldTypeProvider.class)
    void worldShouldBeIgnored(Set<WorldType> worldType) {
        assertTrue(WorldUtils.isIgnoredWorld(worldType));
    }

    private static class IgnoredWorldTypeProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(EnumSet.of(WorldType.PVP_ARENA)),
                Arguments.of(EnumSet.of(WorldType.QUEST_SPEEDRUNNING)),
                Arguments.of(EnumSet.of(WorldType.NOSAVE_MODE)),
                Arguments.of(EnumSet.of(WorldType.TOURNAMENT_WORLD)),
                Arguments.of(EnumSet.of(WorldType.FRESH_START_WORLD, WorldType.NOSAVE_MODE))
            );
        }
    }
}
