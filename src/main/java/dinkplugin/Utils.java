package dinkplugin;

import com.google.common.collect.ImmutableList;
import dinkplugin.message.NotificationBody;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.WorldType;
import net.runelite.client.game.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private static final BinaryOperator<Item> SUM_ITEM_QUANTITIES = (a, b) -> new Item(a.getId(), a.getQuantity() + b.getQuantity());
    private static final BinaryOperator<ItemStack> SUM_ITEM_STACK_QUANTITIES = (a, b) -> new ItemStack(a.getId(), a.getQuantity() + b.getQuantity(), a.getLocation());

    public static boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public static String getPlayerName(Client client) {
        return client.getLocalPlayer().getName();
    }

    public static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static <T> List<T> concat(@NotNull T[] array1, @NotNull T[] array2) {
        final List<T> list = new ArrayList<>(array1.length + array2.length);
        list.addAll(Arrays.asList(array1));
        list.addAll(Arrays.asList(array2));
        return list;
    }

    public static Map<Integer, Item> reduceItems(@NotNull Iterable<Item> items) {
        final Map<Integer, Item> itemById = new HashMap<>();
        items.forEach(item -> itemById.merge(item.getId(), item, SUM_ITEM_QUANTITIES));
        return itemById;
    }

    @NotNull
    public static Collection<ItemStack> reduceItemStack(@NotNull Iterable<ItemStack> items) {
        final Map<Integer, ItemStack> itemById = new HashMap<>();
        items.forEach(item -> itemById.merge(item.getId(), item, SUM_ITEM_STACK_QUANTITIES));
        return itemById.values();
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
