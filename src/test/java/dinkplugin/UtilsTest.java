package dinkplugin;

import dinkplugin.util.ItemUtils;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import net.runelite.client.game.ItemStack;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void coinVariations() {
        assertTrue(ItemUtils.COIN_VARIATIONS.contains(ItemID.COINS));
        assertTrue(ItemUtils.COIN_VARIATIONS.contains(ItemID.COINS_995));
        assertTrue(ItemUtils.COIN_VARIATIONS.contains(ItemID.COINS_6964));
        assertTrue(ItemUtils.COIN_VARIATIONS.contains(ItemID.COINS_8890));
    }

    @ParameterizedTest(name = "Item stack should be reduced {0}")
    @ArgumentsSource(ItemStackReductionProvider.class)
    void itemStackShouldBeReduced(Collection<ItemStack> input, Collection<ItemStack> reduced) {
        Collection<ItemStack> output = ItemUtils.reduceItemStack(input);
        assertTrue(reduced.containsAll(output));
        assertTrue(output.containsAll(reduced));
    }

    private static class ItemStackReductionProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(Collections.emptyList(), Collections.emptyList()),
                Arguments.of(Collections.singletonList(new ItemStack(69, 1)), Collections.singletonList(new ItemStack(69, 1))),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(70, 1)),
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(70, 1))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(69, 1)),
                    Collections.singletonList(new ItemStack(69, 2))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(69, 2)),
                    Collections.singletonList(new ItemStack(69, 3))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(70, 2), new ItemStack(69, 3)),
                    Arrays.asList(new ItemStack(69, 4), new ItemStack(70, 2))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1), new ItemStack(69, 2), new ItemStack(69, 3)),
                    Collections.singletonList(new ItemStack(69, 6))
                )
            );
        }
    }
}
