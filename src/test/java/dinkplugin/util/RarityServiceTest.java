package dinkplugin.util;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.MockedTestBase;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RarityServiceTest extends MockedTestBase {

    private static final double DELTA = MathUtils.EPSILON;

    @Bind
    private final Gson gson = RuneLiteAPI.GSON;

    @Mock
    @Bind
    private ItemManager itemManager;

    @Spy
    @Bind
    private RarityService service;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // default item mock
        Mockito.doAnswer(invocation -> {
            ItemComposition comp = mock(ItemComposition.class);
            when(comp.getMembersName()).thenReturn("?");
            when(comp.getNote()).thenReturn(-1);
            return comp;
        }).when(itemManager).getItemComposition(anyInt());

        // actual item mocks
        mockItem(ItemID.DRAGON_SPEAR, "Dragon spear");
        mockItem(ItemID.DRAGON_AXE, "Dragon axe");
        mockItem(ItemID.SHARK, "Shark");
        mockItem(ItemID.SNAPE_GRASS, "Snape grass");
        mockItem(ItemID.GOBLIN_SKULL, "Goblin skull");
        mockItem(ItemID.COINS, "Coins");
        mockItem(ItemID.COINS_995, "Coins");
        mockItem(ItemID.ARMADYL_HILT, "Armadyl hilt");
        mockItem(ItemID.STEEL_ARROW, "Steel arrow");
        mockItem(ItemID.SILVER_ORE, "Silver ore");
        mockItem(ItemID.SILVER_ORE + 1, "Silver ore", true);
        mockItem(ItemID.UNCUT_RUBY, "Uncut ruby");
        mockItem(ItemID.ECUMENICAL_KEY, "Ecumenical key");
        mockItem(ItemID.DEATH_RUNE, "Death rune");
        mockItem(ItemID.SNAPE_GRASS_SEED, "Snape grass seed");
        mockItem(ItemID.DRAGONSTONE, "Dragonstone");
        mockItem(ItemID.ANCIENT_SHARD, "Ancient shard");
        mockItem(ItemID.LARRANS_KEY, "Larran's key");
        mockItem(ItemID.UNIDENTIFIED_RARE_FOSSIL, "Unidentified rare fossil");
        mockItem(ItemID.GRIMY_GUAM_LEAF, "Grimy guam leaf");
        mockItem(ItemID.DUST_BATTLESTAFF, "Dust battlestaff");
        mockItem(ItemID.GRIMY_AVANTOE, "Grimy avantoe");
        mockItem(ItemID.FIRE_TALISMAN, "Fire talisman");
        mockItem(ItemID.CLUE_SCROLL_ELITE, "Clue scroll (elite)");
        mockItem(ItemID.CLUE_SCROLL_ELITE_12157, "Clue scroll (elite)"); // wiki prefers this ID
    }

    @Test
    @DisplayName("Ensure accurate rarity for very rare drops")
    void testVeryRare() {
        test("Aberrant spectre", ItemID.DRAGON_SPEAR, 1, 1.0 / 139_810);
    }

    @Test
    @DisplayName("Ensure accurate rarity for rare drops")
    void testRare() {
        test("Dagannoth Supreme", ItemID.DRAGON_AXE, 1, 1.0 / 128);
    }

    @Test
    @DisplayName("Ensure accurate rarity for uncommon drops")
    void testUncommon() {
        test("Dagannoth Supreme", ItemID.SHARK, 5, 5.0 / 128);
    }

    @Test
    @DisplayName("Ensure accurate rarity for common drops")
    void testCommon() {
        test("Bree", ItemID.SNAPE_GRASS, 5, 7.0 / 127);
    }

    @Test
    @DisplayName("Ensure drops are de-duplicated across combat levels")
    void testCombatLevel() {
        test("Goblin", ItemID.GOBLIN_SKULL, 1, 1.0 / 4);
    }

    @Test
    @DisplayName("Ensure drop is matched despite which coin item variation is present")
    void testCoins() {
        test("Cave goblin miner", ItemID.COINS, 6, 1 / 6.4);
        test("Cave goblin miner", ItemID.COINS_995, 6, 1 / 6.4);
    }

    @Test
    @DisplayName("Ensure correct drop rate is found when an item has different rates for different quantities")
    void testSingleRepeated() {
        test("Gorak", ItemID.COINS, 475, 1.0 / 16);
    }

    @Test
    @DisplayName("Ensure drop is matched when NPC has a non-alphanumeric name")
    void testSpecialName() {
        test("Kree'arra", ItemID.ARMADYL_HILT, 1, 1.0 / 508);
    }

    @Test
    @DisplayName("Ensure drop is matched when quantity covers a range")
    void testRange() {
        test("Bree", ItemID.COINS, 1_469, 124.0 / 16_129);
    }

    @Test
    @DisplayName("Ensure probabilities are summed when multiple ranges apply")
    void testRangeRepeated() {
        test("Dagannoth Supreme", ItemID.STEEL_ARROW, 150, 1.0 / 1024 + 5.0 / 128);
    }

    @Test
    @DisplayName("Ensure correct range is used when there are multiple with non-overlapping domains")
    void testRangesExclusive() {
        test("Kree'arra", ItemID.COINS, 20_750, 1.25 / 127);
    }

    @Test
    @DisplayName("Treat noted items as un-noted in accordance with wiki item rendering")
    void testNoted() {
        test("Dagannoth Supreme", ItemID.SILVER_ORE + 1, 100, 1.0 / 1024);
    }

    @Test
    @DisplayName("Use standard drop rate when ring of wealth offers better drop rate")
    void testRingWealthMissing() {
        test("Dagannoth Supreme", ItemID.UNCUT_RUBY, 1, 1.0 / 182);
    }

    @Test
    @DisplayName("Use worst drop rate for ecumenical key assuming no Combat Achievements")
    void testEcumenical() {
        test("Gorak", ItemID.ECUMENICAL_KEY, 1, 1.0 / 60);
    }

    @Test
    @DisplayName("Ensure correct drop rate is calculated when multiple rolls are performed")
    void testMultipleRolls() {
        double p = 1.0 / 25;
        test("Vorkath", ItemID.DEATH_RUNE, 800, p * p);
        test("Vorkath", ItemID.DEATH_RUNE, 400, 2 * p * (1 - p));
    }

    @Test
    @DisplayName("Ensure accurate drop rate for rare seed table")
    void testSeeds() {
        test("Sarachnis", ItemID.SNAPE_GRASS_SEED, 5, 1.0 / 2_950);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for rate drop table")
    void testRareTable() {
        test("Dagannoth Supreme", ItemID.DRAGONSTONE, 1, 1.0 / 1_024);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for catacombs table")
    void testCatacombs() {
        test("Hellhound", ItemID.ANCIENT_SHARD, 1, 1.0 / 256);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for wilderness slayer table")
    void testSlayerWildy() {
        test("Moss giant", ItemID.LARRANS_KEY, 1, 1.0 / 533);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for fossil table")
    void testFossil() {
        test("Lobstrosity", ItemID.UNIDENTIFIED_RARE_FOSSIL, 1, 1.0 / 700);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for herb table")
    void testHerb() {
        test("Hydra", ItemID.GRIMY_GUAM_LEAF, 1, 1.0 / 128);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for superior slayer table")
    void testSuperiorSlayer() {
        test("Cockathrice", ItemID.DUST_BATTLESTAFF, 1, 1.0 / 341);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for useful herb table")
    void testHerbUseful() {
        test("Rune dragon", ItemID.GRIMY_AVANTOE, 1, 1 / 50.8);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for talisman table")
    void testTalisman() {
        test("Wallasalki", ItemID.FIRE_TALISMAN, 1, 1.0 / 112);
    }

    @Test
    @DisplayName("Ensure accurate drop rate for clue scrolls")
    void testClueScroll() {
        test("Dagannoth Supreme", ItemID.CLUE_SCROLL_ELITE, 1, 1.0 / 750);
    }

    @Test
    @DisplayName("Drop should not match if quantity is wrong")
    void testInvalidQuantity() {
        OptionalDouble rarity = service.getRarity("Dagannoth Supreme", ItemID.SILVER_ORE, 42);
        assertFalse(rarity.isPresent());
    }

    @Test
    @DisplayName("Ensure accurate drop rate for 'Nothing'")
    void testNothing() {
        test("Air elemental", -1, 0, 1 / 128.2);
    }

    @Test
    @DisplayName("Ignore 'Nothing' if an 'Always' drop is present")
    void testFakeNothing() {
        // bones are always dropped, so loot tracker kc is correct, even though wiki includes a 'Nothing' entry
        OptionalDouble rarity = service.getRarity("Afflicted", -1, 0);
        assertFalse(rarity.isPresent());
    }

    @Test
    @DisplayName("Ensure monster name excludes any parenthetical suffix from the wiki")
    void testSuffix() {
        // RarityCalculator removes wiki's (monster) suffix: https://oldschool.runescape.wiki/w/TzHaar-Mej_(monster)
        test("TzHaar-Mej", ItemID.OBSIDIAN_CAPE, 1, 1.0 / 4_096);
    }

    private void test(String npcName, int itemId, int quantity, double expectedProbability) {
        OptionalDouble rarity = service.getRarity(npcName, itemId, quantity);
        assertTrue(rarity.isPresent());
        assertEquals(expectedProbability, rarity.getAsDouble(), DELTA);
    }

    private void mockItem(int id, String name, boolean noted) {
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        when(item.getMembersName()).thenReturn(name);
        when(item.getNote()).thenReturn(noted ? 799 : -1);
        when(item.getLinkedNoteId()).thenReturn(noted ? id - 1 : id + 1);
        when(itemManager.getItemComposition(id)).thenReturn(item);
    }

    private void mockItem(int id, String name) {
        this.mockItem(id, name, false);
    }
}
