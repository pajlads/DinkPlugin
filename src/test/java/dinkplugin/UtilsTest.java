package dinkplugin;

import dinkplugin.util.Utils;
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
    @ParameterizedTest(name = "Item stack should be reduced {0}")
    @ArgumentsSource(ItemStackReductionProvider.class)
    void itemStackShouldBeReduced(Collection<ItemStack> input, Collection<ItemStack> reduced) {
        Collection<ItemStack> output = Utils.reduceItemStack(input);
        assertTrue(reduced.containsAll(output));
        assertTrue(output.containsAll(reduced));
    }

    private static class ItemStackReductionProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(Collections.emptyList(), Collections.emptyList()),
                Arguments.of(Collections.singletonList(new ItemStack(69, 1, null)), Collections.singletonList(new ItemStack(69, 1, null))),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(70, 1, null)),
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(70, 1, null))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(69, 1, null)),
                    Collections.singletonList(new ItemStack(69, 2, null))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(69, 2, null)),
                    Collections.singletonList(new ItemStack(69, 3, null))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(70, 2, null), new ItemStack(69, 3, null)),
                    Arrays.asList(new ItemStack(69, 4, null), new ItemStack(70, 2, null))
                ),
                Arguments.of(
                    Arrays.asList(new ItemStack(69, 1, null), new ItemStack(69, 2, null), new ItemStack(69, 3, null)),
                    Collections.singletonList(new ItemStack(69, 6, null))
                )
            );
        }
    }
}
