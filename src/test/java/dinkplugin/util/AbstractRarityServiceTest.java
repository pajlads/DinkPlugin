package dinkplugin.util;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.MockedTestBase;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class AbstractRarityServiceTest extends MockedTestBase {
    private static final double DELTA = MathUtils.EPSILON;

    @Bind
    protected final Gson gson = RuneLiteAPI.GSON;

    @Bind
    protected final ItemManager itemManager = Mockito.mock(ItemManager.class);

    protected abstract AbstractRarityService getService();

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
    }

    protected void test(String npcName, int itemId, int quantity, double expectedProbability) {
        OptionalDouble rarity = getService().getRarity(npcName, itemId, quantity);
        assertTrue(rarity.isPresent());
        assertEquals(expectedProbability, rarity.getAsDouble(), DELTA);
    }

    protected void mockItem(int id, String name, boolean noted) {
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        when(item.getMembersName()).thenReturn(name);
        when(item.getNote()).thenReturn(noted ? 799 : -1);
        when(item.getLinkedNoteId()).thenReturn(noted ? id - 1 : id + 1);
        when(itemManager.getItemComposition(id)).thenReturn(item);
    }

    protected void mockItem(int id, String name) {
        this.mockItem(id, name, false);
    }
}
