package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import dinkplugin.message.Embed;
import dinkplugin.notifiers.data.SerializedItemStack;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.QuantityFormatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.runelite.api.ItemID.*;

@UtilityClass
public class ItemUtils {

    final String ITEM_CACHE_BASE_URL = "https://static.runelite.net/cache/item/";

    private final Set<Integer> NEVER_KEPT_ITEMS = ImmutableSet.of(
        CLUE_BOX, LOOTING_BAG,
        AMULET_OF_THE_DAMNED, AMULET_OF_THE_DAMNED_FULL,
        BRACELET_OF_ETHEREUM, BRACELET_OF_ETHEREUM_UNCHARGED,
        AVAS_ACCUMULATOR, AVAS_ATTRACTOR, MAGIC_SECATEURS,
        SILLY_JESTER_HAT, SILLY_JESTER_TOP, SILLY_JESTER_TIGHTS, SILLY_JESTER_BOOTS,
        LUNAR_HELM, LUNAR_TORSO, LUNAR_LEGS, LUNAR_GLOVES, LUNAR_BOOTS,
        LUNAR_CAPE, LUNAR_AMULET, LUNAR_RING, LUNAR_STAFF,
        SHATTERED_RELICS_ADAMANT_TROPHY, SHATTERED_RELICS_BRONZE_TROPHY, SHATTERED_RELICS_DRAGON_TROPHY,
        SHATTERED_RELICS_IRON_TROPHY, SHATTERED_RELICS_MITHRIL_TROPHY, SHATTERED_RELICS_RUNE_TROPHY, SHATTERED_RELICS_STEEL_TROPHY,
        TRAILBLAZER_ADAMANT_TROPHY, TRAILBLAZER_BRONZE_TROPHY, TRAILBLAZER_DRAGON_TROPHY, TRAILBLAZER_IRON_TROPHY,
        TRAILBLAZER_MITHRIL_TROPHY, TRAILBLAZER_RUNE_TROPHY, TRAILBLAZER_STEEL_TROPHY,
        TWISTED_ADAMANT_TROPHY, TWISTED_BRONZE_TROPHY, TWISTED_DRAGON_TROPHY, TWISTED_IRON_TROPHY,
        TWISTED_MITHRIL_TROPHY, TWISTED_RUNE_TROPHY, TWISTED_STEEL_TROPHY
    );

    private final BinaryOperator<Item> SUM_ITEM_QUANTITIES = (a, b) -> new Item(a.getId(), a.getQuantity() + b.getQuantity());
    private final BinaryOperator<ItemStack> SUM_ITEM_STACK_QUANTITIES = (a, b) -> new ItemStack(a.getId(), a.getQuantity() + b.getQuantity(), a.getLocation());

    public boolean isItemNeverKeptOnDeath(int itemId) {
        // https://oldschool.runescape.wiki/w/Items_Kept_on_Death#Items_that_are_never_kept
        // https://oldschoolrunescape.fandom.com/wiki/Items_Kept_on_Death#Items_that_are_never_kept
        return NEVER_KEPT_ITEMS.contains(itemId);
    }

    public long getPrice(@NotNull ItemManager itemManager, int itemId) {
        return getPrice(itemManager, itemId, null);
    }

    private int getPrice(@NotNull ItemManager manager, int itemId, @Nullable ItemComposition item) {
        int price = manager.getItemPrice(itemId);
        if (price <= 0) {
            ItemComposition ic = item != null ? item : manager.getItemComposition(itemId);
            price = ic.getPrice();
        }
        return price;
    }

    public Collection<Item> getItems(Client client) {
        return Stream.of(InventoryID.INVENTORY, InventoryID.EQUIPMENT)
            .map(client::getItemContainer)
            .filter(Objects::nonNull)
            .map(ItemContainer::getItems)
            .flatMap(Arrays::stream)
            .filter(Objects::nonNull)
            .filter(item -> item.getId() >= 0) // -1 implies empty slot
            .collect(Collectors.toList());
    }

    public <K, V> Map<K, V> reduce(@NotNull Iterable<V> items, Function<V, K> deriveKey, BinaryOperator<V> aggregate) {
        final Map<K, V> map = new LinkedHashMap<>();
        items.forEach(v -> map.merge(deriveKey.apply(v), v, aggregate));
        return map;
    }

    public Map<Integer, Item> reduceItems(@NotNull Iterable<Item> items) {
        return reduce(items, Item::getId, SUM_ITEM_QUANTITIES);
    }

    @NotNull
    public Collection<ItemStack> reduceItemStack(@NotNull Iterable<ItemStack> items) {
        return reduce(items, ItemStack::getId, SUM_ITEM_STACK_QUANTITIES).values();
    }

    public SerializedItemStack stackFromItem(ItemManager itemManager, Item item) {
        return stackFromItem(itemManager, item.getId(), item.getQuantity());
    }

    public SerializedItemStack stackFromItem(ItemManager itemManager, int id, int quantity) {
        ItemComposition composition = itemManager.getItemComposition(id);
        int price = getPrice(itemManager, id, composition);
        return new SerializedItemStack(id, quantity, price, composition.getName());
    }

    public String getItemImageUrl(int itemId) {
        return ITEM_CACHE_BASE_URL + "icon/" + itemId + ".png";
    }

    public String getNpcImageUrl(int npcId) {
        return String.format("https://chisel.weirdgloop.org/static/img/osrs-npc/%d_128.png", npcId);
    }

    public List<Embed> buildEmbeds(int[] itemIds) {
        return Arrays.stream(itemIds)
            .mapToObj(ItemUtils::getItemImageUrl)
            .map(Embed::ofImage)
            .collect(Collectors.toList());
    }

    public String formatGold(long amount) {
        return String.format("```ldif\n%s gp\n```", QuantityFormatter.quantityToStackSize(amount));
    }

}
