package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

@Slf4j
@Singleton
public class NpcDropTable {
    private static final Set<Integer> COINS = ImmutableSet.of(ItemID.COINS, ItemID.COINS_995, ItemID.COINS_6964, ItemID.COINS_8890);
    private final Map<String, List<Item>> itemDropsByNpcName = new HashMap<>(1024);
    private @Inject Gson gson;

    public OptionalDouble getRarity(String npcName, int itemId, int quantity) {
        return itemDropsByNpcName.getOrDefault(npcName, Collections.emptyList())
            .stream()
            .filter(item -> item.matchesId(itemId))
            .filter(item -> item.matchesQuantity(quantity))
            .mapToDouble(Item::getProbability)
            .reduce(Double::sum);
    }

    @Inject
    void init() {
        try (Reader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/npc_drops.json")))) {
            itemDropsByNpcName.putAll(gson.fromJson(reader, new TypeToken<Map<String, List<Item>>>() {}.getType()));
        } catch (Exception e) {
            log.error("Failed to read npc drop table", e);
        }
    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    private static class Item {
        private static final Pair<Integer, Integer> UNPARSEABLE = Pair.of(-1, -1);
        private transient Pair<Integer, Integer> parsedQuantity = null;

        private @SerializedName("i") int id;
        private @SerializedName("q") String quantity;
        private @SerializedName("p") double probability;

        boolean matchesId(int itemId) {
            return id == itemId || (COINS.contains(id) && COINS.contains(itemId));
        }

        boolean matchesQuantity(int q) {
            Pair<Integer, Integer> parsed = getParsedQuantity();
            return parsed == UNPARSEABLE || (parsed.getLeft() <= q && q <= parsed.getRight());
        }

        private Pair<Integer, Integer> getParsedQuantity() {
            if (parsedQuantity == null) {
                parsedQuantity = parseQuantity(getQuantity());
            }
            return parsedQuantity;
        }

        private static Pair<Integer, Integer> parseQuantity(String quantity) {
            if (quantity == null || quantity.isEmpty()) return UNPARSEABLE;

            int delim = quantity.indexOf('-');
            String min, max;
            if (delim <= 0) {
                min = max = quantity;
            } else {
                min = quantity.substring(0, delim);
                max = quantity.substring(delim + 1);
            }

            try {
                return Pair.of(Integer.parseInt(min), Integer.parseInt(max));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse quantity {}", quantity);
                return UNPARSEABLE;
            }
        }
    }
}
