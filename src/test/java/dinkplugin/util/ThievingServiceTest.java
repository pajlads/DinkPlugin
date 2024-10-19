package dinkplugin.util;

import com.google.inject.testing.fieldbinder.Bind;
import lombok.Getter;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ThievingServiceTest extends AbstractRarityServiceTest {

    @Bind
    @Getter
    private final ThievingService service = Mockito.spy(new ThievingService(gson, itemManager));

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // item mocks
        mockItem(ItemID.BLOOD_SHARD, "Blood shard");
        mockItem(ItemID.ENHANCED_CRYSTAL_TELEPORT_SEED, "Enhanced crystal teleport seed");
        mockItem(ItemID.UNCUT_DIAMOND, "Uncut diamond");
        mockItem(ItemID.CLUE_SCROLL_EASY, "Clue scroll (easy)");
        mockItem(ItemID.CLUE_SCROLL_EASY_2711, "Clue scroll (easy)");
        mockItem(ItemID.CLUE_SCROLL_MEDIUM, "Clue scroll (medium)");
        mockItem(ItemID.CLUE_SCROLL_MEDIUM_2809, "Clue scroll (medium)");
        mockItem(ItemID.CLUE_SCROLL_HARD, "Clue scroll (hard)");
        mockItem(ItemID.CLUE_SCROLL_HARD_3560, "Clue scroll (hard)");
        mockItem(ItemID.CLUE_SCROLL_ELITE, "Clue scroll (elite)");
        mockItem(ItemID.CLUE_SCROLL_ELITE_12157, "Clue scroll (elite)");
        mockItem(ItemID.CLUE_SCROLL_ELITE_12075, "Clue scroll (elite)");
        mockItem(ItemID.HAM_CLOAK, "Ham cloak");
        mockItem(ItemID.HAM_BOOTS, "Ham boots");
        mockItem(ItemID.SNAPE_GRASS_SEED, "Snape grass seed");
        mockItem(ItemID.SNAPDRAGON_SEED, "Snapdragon seed");
    }

    @Test
    void testFarmer() {
        test("Master Farmer", ItemID.SNAPE_GRASS_SEED, 1, 1.0 / 260);
        test("Master Farmer", ItemID.SNAPDRAGON_SEED, 1, 1.0 / 2083);

        assertFalse(service.getRarity("Farmer", ItemID.SNAPDRAGON_SEED, 1).isPresent());
    }

    @Test
    void testHam() {
        test("H.A.M. Member", ItemID.HAM_CLOAK, 1, 1.0 / 100);
        test("H.A.M. Member", ItemID.HAM_BOOTS, 1, 1.0 / 100);

        assertFalse(service.getRarity("Thief", ItemID.HAM_BOOTS, 1).isPresent());
    }

    @Test
    void testCitizen() {
        test("Wealthy citizen", ItemID.CLUE_SCROLL_EASY, 1, 1.0 / 85);

        assertFalse(service.getRarity("Wealthy citizen", ItemID.CLUE_SCROLL_HARD, 1).isPresent());
    }

    @Test
    void testPaladin() {
        test("Paladin", ItemID.CLUE_SCROLL_HARD, 1, 1.0 / 1000);
    }

    @Test
    void testGnome() {
        test("Gnome", ItemID.CLUE_SCROLL_MEDIUM, 1, 1.0 / 150);
    }

    @Test
    void testHero() {
        test("Hero", ItemID.CLUE_SCROLL_ELITE, 1, 1.0 / 1400);
        test("Hero", ItemID.CLUE_SCROLL_ELITE_12075, 1, 1.0 / 1400);
        test("Hero", ItemID.CLUE_SCROLL_ELITE_12157, 1, 1.0 / 1400);
    }

    @Test
    void testVyre() {
        test("Caninelle Draynar", ItemID.BLOOD_SHARD, 1, 1.0 / 5000);
        test("Grigor Rasputin", ItemID.BLOOD_SHARD, 1, 1.0 / 5000);
        test("Valentina Diaemus", ItemID.BLOOD_SHARD, 1, 1.0 / 5000);
    }

    @Test
    void testElf() {
        test("Arvel", ItemID.ENHANCED_CRYSTAL_TELEPORT_SEED, 1, 1.0 / 1024);
        test("Indis", ItemID.ENHANCED_CRYSTAL_TELEPORT_SEED, 1, 1.0 / 1024);
    }

    @Test
    void testHur() {
        test("TzHaar-Hur", ItemID.UNCUT_DIAMOND, 1, 1.0 / 195);

        assertFalse(service.getRarity("TzHaar-Hur", ItemID.UNCUT_DIAMOND, 2).isPresent());
    }

}
