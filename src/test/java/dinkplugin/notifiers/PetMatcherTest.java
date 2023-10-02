package dinkplugin.notifiers;

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

class PetMatcherTest {

    @ParameterizedTest(name = "Pet message should trigger {0}")
    @ValueSource(
        strings = {
            "You have a funny feeling like you're being followed.",
            "You have a funny feeling like you would have been followed...",
            "You feel something weird sneaking into your backpack."
        }
    )
    void petRegexFindsMatch(String message) {
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
    void petRegexDoesNotMatch(String message) {
        Matcher matcher = PetNotifier.PET_REGEX.matcher(message);
        assertFalse(matcher.find());
    }

    @ParameterizedTest(name = "Pet name should be parsed from: {0}")
    @ArgumentsSource(ClanPetProvider.class)
    void petNameMatches(String message, String user, String pet) {
        Matcher matcher = PetNotifier.CLAN_REGEX.matcher(message);
        assertTrue(matcher.find());
        assertEquals(user, matcher.group("user"));
        assertEquals(pet, matcher.group("pet"));
    }

    @ParameterizedTest(name = "Pet name should be not parsed from {0}")
    @ValueSource(
        strings = {
            "Forsen: forsen",
            "You feel like you forgot to turn the stove off"
        }
    )
    void petNameDoesNotMatch(String message) {
        Matcher matcher = PetNotifier.CLAN_REGEX.matcher(message);
        assertFalse(matcher.find());
    }

    private static class ClanPetProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of("[Log Dog LLC] Clipper has a funny feeling like he's being followed: Little nightmare at 2,678 killcount.", "Clipper", "Little nightmare"),
                Arguments.of("[Muu] Majin Muu has a funny feeling like she's being followed: Herbi at 3,054 harvest count.", "Majin Muu", "Herbi"),
                Arguments.of("[Dankermen] pajdank has a funny feeling like he's being followed: Rocky at 5,850,317 XP.", "pajdank", "Rocky"),
                /*
                You have a funny feeling like you're being followed.
                New item added to your collection log: Lil' creator
                CL Locked received a drop: Lil' creator
                [Boss Locked] CL Locked has a funny feeling like he's being followed: Lil' creator at 22 crates.
                [Boss Locked] CL Locked received a new collection log item: Lil' creator (86/1476)
                From https://youtu.be/6jFmC8E0ypI?t=858
                 */
                Arguments.of("[Boss Locked] CL Locked has a funny feeling like he's being followed: Lil' creator at 22 crates.", "CL Locked", "Lil' creator"),
                Arguments.of("[Outer Void] Dash Inc feels something weird sneaking into his backpack: Heron at 8,426,283 XP.", "Dash Inc", "Heron")
            );
        }
    }

}
