package dinkplugin.util;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.MockedTestBase;
import net.runelite.api.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcDropTableTest extends MockedTestBase {

    private static final double DELTA = 0.00000001;

    @Bind
    private final Gson gson = RuneLiteAPI.GSON;

    @Spy
    @Bind
    private NpcDropTable dropTable;

    @Test
    void testRare() {
        OptionalDouble rarity = dropTable.getRarity("Aberrant spectre", ItemID.DRAGON_SPEAR, 1);
        assertTrue(rarity.isPresent());
        assertEquals(1.0 / 139_810, rarity.getAsDouble(), DELTA);
    }

    @Test
    void testCommon() {
        OptionalDouble rarity = dropTable.getRarity("Goblin", ItemID.GOBLIN_SKULL, 1);
        assertTrue(rarity.isPresent());
        assertEquals(1.0 / 4.0, rarity.getAsDouble(), DELTA);
    }

    @Test
    void testCoins() {
        OptionalDouble rarity = dropTable.getRarity("Cave goblin miner", ItemID.COINS, 6);
        assertTrue(rarity.isPresent());
        assertEquals(1 / 6.4, rarity.getAsDouble(), DELTA);
    }

    @Test
    void testSingleRepeated() {
        OptionalDouble rarity = dropTable.getRarity("Gorak", ItemID.COINS, 475);
        assertTrue(rarity.isPresent());
        assertEquals(1.0 / 16, rarity.getAsDouble(), DELTA);
    }

    @Test
    void testRange() {
        OptionalDouble rarity = dropTable.getRarity("Bree", ItemID.COINS, 1469);
        assertTrue(rarity.isPresent());
        assertEquals(1 / 130.1, rarity.getAsDouble(), DELTA);
    }

    @Test
    void testRangeRepeated() {
        OptionalDouble rarity = dropTable.getRarity("Bree", ItemID.COINS, 1312);
        assertTrue(rarity.isPresent());
        assertEquals(1 / 2.048 + 1 / 169.3, rarity.getAsDouble(), DELTA);
    }

}
