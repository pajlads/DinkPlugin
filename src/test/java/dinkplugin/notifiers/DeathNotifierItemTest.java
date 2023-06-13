package dinkplugin.notifiers;

import net.runelite.api.Item;
import net.runelite.api.ItemID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static dinkplugin.notifiers.DeathNotifier.splitItemsByKept;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathNotifierItemTest {
    private static final Pair<Item, Long> BOND, EGG, GRAIN, POT, SALMON, TUNA, BAG, CLUE, JESTER, TROPHY, AVA;

    @Test
    void testSplit() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(EGG, GRAIN, POT, SALMON, TUNA), 3);
        assertEquals(asList(EGG, GRAIN, POT), split.getLeft());
        assertEquals(asList(SALMON, TUNA), split.getRight());
    }

    @Test
    void testSplitStackable() {
        int total = 30, keep = 3;
        long price = 2;
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(
            singletonList(Pair.of(new Item(ItemID.FEATHER, total), price)),
            keep
        );

        Pair<Item, Long> feather = Pair.of(new Item(ItemID.FEATHER, 1), price);
        assertEquals(
            asList(feather, feather, feather), // IntStream.range(0, keep).mapToObj(i -> feather).collect(Collectors.toList()),
            split.getLeft()
        );
        assertEquals(
            singletonList(Pair.of(new Item(ItemID.FEATHER, total - keep), price)),
            split.getRight()
        );
    }

    @Test
    void testSplitProtected() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(EGG, GRAIN, POT, SALMON, TUNA), 4);
        assertEquals(asList(EGG, GRAIN, POT, SALMON), split.getLeft());
        assertEquals(singletonList(TUNA), split.getRight());
    }

    @Test
    void testSplitBond() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(BOND, BOND, BOND, BOND, BOND, EGG), 3);
        assertEquals(asList(BOND, BOND, BOND, BOND, BOND, EGG), split.getLeft());
        assertTrue(split.getRight().isEmpty());
    }

    @Test
    void testSplitSkulled() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(BOND, GRAIN), 0);
        assertEquals(singletonList(BOND), split.getLeft());
        assertEquals(singletonList(GRAIN), split.getRight());
    }

    @Test
    void testSplitSkulledProtect() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(BOND, GRAIN, EGG), 1);
        assertEquals(asList(BOND, GRAIN), split.getLeft());
        assertEquals(singletonList(EGG), split.getRight());
    }

    @Test
    void testSplitSkulledProtectOrdered() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(BOND, EGG, GRAIN), 1);
        assertEquals(asList(BOND, EGG), split.getLeft());
        assertEquals(singletonList(GRAIN), split.getRight());
    }

    @Test
    void testSplitNeverKept() {
        Pair<List<Pair<Item, Long>>, List<Pair<Item, Long>>> split = splitItemsByKept(asList(BAG, EGG, CLUE, BOND, JESTER, TROPHY, GRAIN, AVA, POT, TUNA), 3);
        assertEquals(asList(EGG, BOND, GRAIN, POT), split.getLeft());
        assertEquals(asList(BAG, CLUE, JESTER, TROPHY, AVA, TUNA), split.getRight());
    }

    static {
        Function<Integer, Pair<Item, Long>> item = id -> Pair.of(new Item(id, 1), 0L);

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
