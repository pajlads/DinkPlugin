package dinkplugin.notifiers;

import net.runelite.api.Item;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dinkplugin.notifiers.DeathNotifier.splitItemsByKept;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathNotifierItemTest {
    private static final Map.Entry<Item, Long> BOND, EGG, GRAIN, POT, SALMON, TUNA, BAG, CLUE, JESTER, TROPHY, AVA;

    @Test
    void testSplit() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(EGG, GRAIN, POT, SALMON, TUNA), 3);
        assertEquals(asList(EGG, GRAIN, POT), split.getKey());
        assertEquals(asList(SALMON, TUNA), split.getValue());
    }

    @Test
    void testSplitStackable() {
        int total = 30, keep = 3;
        long price = 2;
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(
            singletonList(Map.entry(new Item(ItemID.FEATHER, total), price)),
            keep
        );

        Map.Entry<Item, Long> feather = Map.entry(new Item(ItemID.FEATHER, 1), price);
        assertEquals(
            asList(feather, feather, feather), // IntStream.range(0, keep).mapToObj(i -> feather).collect(Collectors.toList()),
            split.getKey()
        );
        assertEquals(
            singletonList(Map.entry(new Item(ItemID.FEATHER, total - keep), price)),
            split.getValue()
        );
    }

    @Test
    void testSplitProtected() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(EGG, GRAIN, POT, SALMON, TUNA), 4);
        assertEquals(asList(EGG, GRAIN, POT, SALMON), split.getKey());
        assertEquals(singletonList(TUNA), split.getValue());
    }

    @Test
    void testSplitBond() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(BOND, BOND, BOND, BOND, BOND, EGG), 3);
        assertEquals(asList(BOND, BOND, BOND, BOND, BOND, EGG), split.getKey());
        assertTrue(split.getValue().isEmpty());
    }

    @Test
    void testSplitSkulled() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(BOND, GRAIN), 0);
        assertEquals(singletonList(BOND), split.getKey());
        assertEquals(singletonList(GRAIN), split.getValue());
    }

    @Test
    void testSplitSkulledProtect() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(BOND, GRAIN, EGG), 1);
        assertEquals(asList(BOND, GRAIN), split.getKey());
        assertEquals(singletonList(EGG), split.getValue());
    }

    @Test
    void testSplitSkulledProtectOrdered() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(BOND, EGG, GRAIN), 1);
        assertEquals(asList(BOND, EGG), split.getKey());
        assertEquals(singletonList(GRAIN), split.getValue());
    }

    @Test
    void testSplitNeverKept() {
        Map.Entry<List<Map.Entry<Item, Long>>, List<Map.Entry<Item, Long>>> split = splitItemsByKept(asList(BAG, EGG, CLUE, BOND, JESTER, TROPHY, GRAIN, AVA, POT, TUNA), 3);
        assertEquals(asList(EGG, BOND, GRAIN, POT), split.getKey());
        assertEquals(asList(BAG, CLUE, JESTER, TROPHY, AVA, TUNA), split.getValue());
    }

    static {
        Function<Integer, Map.Entry<Item, Long>> item = id -> Map.entry(new Item(id, 1), 0L);

        BOND = item.apply(ItemID.OLD_SCHOOL_BOND);
        EGG = item.apply(ItemID.EGG);
        GRAIN = item.apply(ItemID.GRAIN);
        POT = item.apply(ItemID.POT);
        SALMON = item.apply(ItemID.SALMON);
        TUNA = item.apply(ItemID.TUNA);
        BAG = item.apply(ItemID.LOOTING_BAG);
        CLUE = item.apply(ItemID.CLUE_BOX);
        JESTER = item.apply(ItemID.SILLY_JESTER_HAT);
        TROPHY = item.apply(ItemID.TRAILBLAZER_DRAGON_TROPHY);
        AVA = item.apply(ItemID.AVAS_ACCUMULATOR);
    }
}
