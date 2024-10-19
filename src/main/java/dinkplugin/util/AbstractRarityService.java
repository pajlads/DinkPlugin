package dinkplugin.util;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractRarityService {

    protected final Gson gson;
    protected final ItemManager itemManager;
    protected final Map<String, Collection<RareDrop>> dropsBySourceName;

    AbstractRarityService(String resourceName, int expectedSize, Gson gson, ItemManager itemManager) {
        this.gson = gson;
        this.itemManager = itemManager;
        this.dropsBySourceName = new HashMap<>(expectedSize);

        Map<String, List<RawDrop>> raw;
        try (InputStream is = getClass().getResourceAsStream(resourceName);
             Reader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
            raw = gson.fromJson(reader, new TypeToken<Map<String, List<RawDrop>>>() {}.getType());
        } catch (Exception e) {
            log.error("Failed to read monster drop rates", e);
            return;
        }

        raw.forEach((sourceName, rawDrops) -> {
            ArrayList<RareDrop> drops = rawDrops.stream()
                .map(RawDrop::transform)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            drops.trimToSize();
            dropsBySourceName.put(sourceName, drops);
        });
    }

    public OptionalDouble getRarity(String sourceName, int itemId, int quantity) {
        ItemComposition composition = itemId >= 0 ? itemManager.getItemComposition(itemId) : null;
        int canonical = composition != null && composition.getNote() != -1 ? composition.getLinkedNoteId() : itemId;
        String itemName = composition != null ? composition.getMembersName() : "";
        Collection<Integer> variants = new HashSet<>(
            ItemVariationMapping.getVariations(ItemVariationMapping.map(canonical))
        );
        return dropsBySourceName.getOrDefault(sourceName, Collections.emptyList())
            .stream()
            .filter(drop -> drop.getMinQuantity() <= quantity && quantity <= drop.getMaxQuantity())
            .filter(drop -> {
                int id = drop.getItemId();
                if (id == itemId) return true;
                return variants.contains(id) && itemName.equals(itemManager.getItemComposition(id).getMembersName());
            })
            .mapToDouble(RareDrop::getProbability)
            .reduce(Double::sum);
    }

    @Value
    protected static class RareDrop {
        int itemId;
        int minQuantity;
        int maxQuantity;
        double probability;
    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    private static class RawDrop {
        private @SerializedName("i") int itemId;
        private @SerializedName("r") Integer rolls;
        private @SerializedName("d") double denominator;
        private @SerializedName("q") Integer quantity;
        private @SerializedName("m") Integer quantMin;
        private @SerializedName("n") Integer quantMax;

        Collection<RareDrop> transform() {
            int rounds = rolls != null ? rolls : 1;
            int q = quantity != null ? quantity : 1;
            int min = quantMin != null ? quantMin : q;
            int max = quantMax != null ? quantMax : q;
            double prob = 1 / denominator;

            if (rounds == 1) {
                return List.of(new RareDrop(itemId, min, max, prob));
            }
            List<RareDrop> drops = new ArrayList<>(rounds);
            for (int successCount = 1; successCount <= rounds; successCount++) {
                double density = MathUtils.binomialProbability(prob, rounds, successCount);
                drops.add(new RareDrop(itemId, min * successCount, max * successCount, density));
            }
            return drops;
        }
    }
}
