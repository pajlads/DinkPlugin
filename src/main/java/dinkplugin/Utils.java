package dinkplugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dinkplugin.message.NotificationBody;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private static final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7513, 7514, 7769, 7770, 8025, 8026);

    private static final BinaryOperator<Item> SUM_ITEM_QUANTITIES = (a, b) -> new Item(a.getId(), a.getQuantity() + b.getQuantity());
    private static final BinaryOperator<ItemStack> SUM_ITEM_STACK_QUANTITIES = (a, b) -> new ItemStack(a.getId(), a.getQuantity() + b.getQuantity(), a.getLocation());

    @Varbit
    private static final int CASTLE_WARS_COUNTDOWN = 380;
    @Varbit
    private static final int CASTLE_WARS_CATAPULT_X_AXIS_OFFSET = 156;
    @Varbit
    private static final int CASTLE_WARS_CATAPULT_Y_AXIS_OFFSET = 157;

    private static final Set<Integer> NEVER_KEPT_ITEMS = ImmutableSet.of(
        ItemID.CLUE_BOX, ItemID.CHRONICLE, ItemID.LOOTING_BAG,
        ItemID.AMULET_OF_THE_DAMNED, ItemID.AMULET_OF_THE_DAMNED_FULL,
        ItemID.BRACELET_OF_ETHEREUM, ItemID.BRACELET_OF_ETHEREUM_UNCHARGED,
        ItemID.AVAS_ACCUMULATOR, ItemID.AVAS_ATTRACTOR, ItemID.MAGIC_SECATEURS,
        ItemID.SILLY_JESTER_HAT, ItemID.SILLY_JESTER_TOP, ItemID.SILLY_JESTER_TIGHTS, ItemID.SILLY_JESTER_BOOTS,
        ItemID.LUNAR_HELM, ItemID.LUNAR_TORSO, ItemID.LUNAR_LEGS, ItemID.LUNAR_GLOVES, ItemID.LUNAR_BOOTS,
        ItemID.LUNAR_CAPE, ItemID.LUNAR_AMULET, ItemID.LUNAR_RING, ItemID.LUNAR_STAFF
    );

    public static boolean isItemNeverKeptOnDeath(int itemId) {
        return NEVER_KEPT_ITEMS.contains(itemId);
    }

    public static boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public static boolean isPvpWorld(Set<WorldType> worldType) {
        return worldType.contains(WorldType.PVP) || worldType.contains(WorldType.DEADMAN);
    }

    public static boolean isPvpSafeZone(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PVP_WORLD_SAFE_ZONE);
        return widget != null && !widget.isSelfHidden();
    }

    public static boolean isCastleWars(Client client) {
        return client.getVarbitValue(CASTLE_WARS_COUNTDOWN) > 0 || client.getVarbitValue(CASTLE_WARS_CATAPULT_X_AXIS_OFFSET) > 0 || client.getVarbitValue(CASTLE_WARS_CATAPULT_Y_AXIS_OFFSET) > 0;
    }

    public static boolean isPestControl(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PEST_CONTROL_BLUE_SHIELD);
        return widget != null;
    }

    public static boolean isPlayerOwnedHouse(Client client) {
        return POH_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public static boolean isSafeArea(Client client) {
        return isCastleWars(client) || isPestControl(client) || isPlayerOwnedHouse(client);
    }

    public static String getPlayerName(Client client) {
        return client.getLocalPlayer().getName();
    }

    public static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static Collection<Item> getItems(Client client) {
        return Stream.of(InventoryID.INVENTORY, InventoryID.EQUIPMENT)
            .map(client::getItemContainer)
            .filter(Objects::nonNull)
            .map(ItemContainer::getItems)
            .flatMap(Arrays::stream)
            .filter(Objects::nonNull)
            .filter(item -> item.getId() >= 0) // -1 implies empty slot
            .collect(Collectors.toList());
    }

    public static <K, V> Map<K, V> reduce(@NotNull Iterable<V> items, Function<V, K> deriveKey, BinaryOperator<V> aggregate) {
        final Map<K, V> map = new HashMap<>();
        items.forEach(v -> map.merge(deriveKey.apply(v), v, aggregate));
        return map;
    }

    public static Map<Integer, Item> reduceItems(@NotNull Iterable<Item> items) {
        return reduce(items, Item::getId, SUM_ITEM_QUANTITIES);
    }

    @NotNull
    public static Collection<ItemStack> reduceItemStack(@NotNull Iterable<ItemStack> items) {
        return reduce(items, ItemStack::getId, SUM_ITEM_STACK_QUANTITIES).values();
    }

    public static SerializedItemStack stackFromItem(ItemManager itemManager, Item item) {
        int id = item.getId();
        int quantity = item.getQuantity();
        int price = itemManager.getItemPrice(id);
        ItemComposition composition = itemManager.getItemComposition(id);
        return new SerializedItemStack(id, quantity, price, composition.getName());
    }

    public static String getItemImageUrl(int itemId) {
        return "https://static.runelite.net/cache/item/icon/" + itemId + ".png";
    }

    public static String getNpcImageUrl(int npcId) {
        return String.format("https://chisel.weirdgloop.org/static/img/osrs-npc/%d_128.png", npcId);
    }

    public static List<NotificationBody.Embed> buildEmbeds(int[] itemIds) {
        return Arrays.stream(itemIds)
            .mapToObj(Utils::getItemImageUrl)
            .map(NotificationBody.UrlEmbed::new)
            .map(NotificationBody.Embed::new)
            .collect(Collectors.toList());
    }

    // Credit to: https://github.com/oliverpatrick/Enhanced-Discord-Notifications/blob/master/src/main/java/com/enhanceddiscordnotifications/EnhancedDiscordNotificationsPlugin.java
    // This method existed and seemed fairly solid.

    private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: [Qq]uest)?[!.]?$");
    private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: [Qq]uest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$");
    private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
    private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");

    public static String parseQuestWidget(final String text) {
        // "You have completed The Corsair Curse!"
        final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
        // "'One Small Favour' completed!"
        final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
        final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;
        if (!questMatchFinal.matches()) {
            return "Unable to find quest name!";
        }

        String quest = questMatchFinal.group("quest");
        String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

        if (verb.contains("kind of")) {
            quest += " partial completion";
        } else if (verb.contains("completely")) {
            quest += " II";
        }

        if (RFD_TAGS.stream().anyMatch((quest + verb)::contains)) {
            quest = "Recipe for Disaster - " + quest;
        }

        if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains)) {
            quest += " Quest";
        }

        return quest;
    }
}
