package dinkplugin

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import net.runelite.client.game.ItemStack
import org.junit.jupiter.api.Assertions.assertTrue

class UtilsTest {
    @ParameterizedTest(name = "Item stack should be reduced {0}")
    @ArgumentsSource(ItemStackReductionProvider::class)
    fun `Item stack should be reduced`(input: Collection<ItemStack>, reduced: Collection<ItemStack>) {
        val output = Utils.reduceItemStack(input)
        assertTrue(reduced.containsAll(output))
        assertTrue(output.containsAll(reduced))
    }

    private class ItemStackReductionProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> = Stream.of(
            Arguments.of(emptyList<ItemStack>(), emptyList<ItemStack>()),
            Arguments.of(listOf(ItemStack(69, 1, null)), listOf(ItemStack(69, 1, null))),
            Arguments.of(
                listOf(ItemStack(69, 1, null), ItemStack(70, 1, null)),
                listOf(ItemStack(69, 1, null), ItemStack(70, 1, null))
            ),
            Arguments.of(
                listOf(ItemStack(69, 1, null), ItemStack(69, 1, null)),
                listOf(ItemStack(69, 2, null))
            ),
            Arguments.of(
                listOf(ItemStack(69, 1, null), ItemStack(69, 2, null)),
                listOf(ItemStack(69, 3, null))
            ),
            Arguments.of(
                listOf(ItemStack(69, 1, null), ItemStack(70, 2, null), ItemStack(69, 3, null)),
                listOf(ItemStack(69, 4, null), ItemStack(70, 2, null))
            ),
            Arguments.of(
                listOf(ItemStack(69, 1, null), ItemStack(69, 2, null), ItemStack(69, 3, null)),
                listOf(ItemStack(69, 6, null))
            )
        )
    }
}
