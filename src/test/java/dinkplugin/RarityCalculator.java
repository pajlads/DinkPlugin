package dinkplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.Value;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Tag("generator")
class RarityCalculator {

    private static final String MONSTERS_URL = "https://raw.githubusercontent.com/Flipping-Utilities/parsed-osrs/main/data/monsters/all-monsters.json";

    private static final Comparator<Transformed> COMPARATOR = Comparator.comparingInt(Transformed::getItemId)
        .thenComparing(Transformed::getDenominator)
        .thenComparing(Transformed::getRolls, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(Transformed::getQuantity, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Transformed::getQuantMin, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Transformed::getQuantMax, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Pattern PARENTHETICAL_SUFFIX = Pattern.compile("\\s\\(.+\\)$");

    private final Gson gson = new GsonBuilder().create();
    private final OkHttpClient httpClient = new OkHttpClient();

    @Test
    void calculateMonsterRates() throws IOException {
        List<Monster> monsters;
        try (Response response = httpClient.newCall(new Request.Builder().url(MONSTERS_URL).build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Could not read parsed monsters file");
            }
            monsters = gson.fromJson(response.body().charStream(), new TypeToken<List<Monster>>() {}.getType());
        }

        SortedMap<String, Collection<Transformed>> map = new TreeMap<>();
        for (Monster npc : monsters) {
            Collection<Transformed> drops = npc.getTransformed();
            if (drops.isEmpty()) continue;

            String name = PARENTHETICAL_SUFFIX.matcher(Text.removeTags(npc.getName()).replace("&#039;", "'")).replaceFirst("");
            map.putIfAbsent(name, drops);
        }

        String output = gson.toJson(map).replace(".00,", ",");
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./src/main/resources/npc_drops.json"))) {
            writer.write(output);
            writer.newLine();
        }
    }

    @Data
    static class Monster {
        private int id;
        private String name;
        private List<Drop> drops;

        Collection<Transformed> getTransformed() {
            if (drops == null || drops.isEmpty()) return Collections.emptyList();
            SortedSet<Transformed> set = new TreeSet<>(COMPARATOR);
            Set<Integer> alwaysDropped = drops.stream()
                .filter(d -> "Always".equals(d.getRarity()))
                .map(Drop::getItemId)
                .collect(Collectors.toSet());
            for (Drop drop : drops) {
                if (alwaysDropped.contains(drop.getItemId())) continue;
                Transformed transformed = drop.transform();
                if (transformed == null) continue;
                if (!alwaysDropped.isEmpty() && transformed.getItemId() < 0) continue;
                set.add(transformed);
            }
            return set;
        }
    }

    @Data
    static class Drop {
        private static final Set<Integer> COINS = Set.of(
            ItemID.COINS, ItemID.COINS_2, ItemID.COINS_3, ItemID.COINS_4, ItemID.COINS_5, ItemID.COINS_25,
            ItemID.COINS_100, ItemID.COINS_250, ItemID.COINS_1000, ItemID.COINS_10000
        );

        private String name;
        private String quantity;
        private String rarity;
        private Integer itemId;

        public Transformed transform() {
            if ("Nothing".equalsIgnoreCase(name)) {
                this.itemId = -1;
                this.quantity = "0";
            }
            if ("Tooth half of key (moon key)".equalsIgnoreCase(name)) {
                // Workaround for https://discord.com/channels/790429747364626452/954397870889529385/1294355009798017044
                this.itemId = ItemID.VARLAMORE_KEY_HALF_1;
            }
            if (itemId == null || rarity == null || quantity == null) return null;
            if (rarity.equals("Always") || rarity.equals("Varies") || rarity.equals("Random") || rarity.equals("Once") || rarity.equals("Unknown") || rarity.equals("?")) return null;
            if (quantity.equals("Unknown") || quantity.equals("N/A")) return null;

            int item = COINS.contains(itemId) ? ItemID.FAKE_COINS : itemId;

            String cleanQuantity = StringUtils.removeEnd(quantity, "\u00A0");
            Integer q, min, max;
            String[] quantParts = StringUtils.split(cleanQuantity, "–;");
            if (quantParts.length == 1) {
                q = Integer.parseInt(cleanQuantity.trim());
                min = max = null;
            } else {
                q = null;
                min = Integer.parseInt(quantParts[0].trim());
                max = Integer.parseInt(quantParts[quantParts.length - 1].trim());
            }

            int rollsDelim = rarity.indexOf("×");
            Integer rolls;
            String cleanRarity;
            if (rollsDelim < 0) {
                rolls = null;
                cleanRarity = rarity.replace("~", "");
            } else {
                rolls = Integer.parseInt(rarity.substring(0, rollsDelim - 1).trim());
                cleanRarity = rarity.substring(rollsDelim + 2).replace("~", "");
            }

            BigDecimal denom;
            switch (cleanRarity) {
                case "Common":
                    denom = BigDecimal.valueOf(10);
                    break;
                case "Uncommon":
                    denom = BigDecimal.valueOf(50);
                    break;
                case "Rare":
                    denom = BigDecimal.valueOf(400);
                    break;
                case "Very rare":
                    denom = BigDecimal.valueOf(2000);
                    break;
                default:
                    String fraction = cleanRarity.endsWith("%") ? cleanRarity.substring(0, cleanRarity.length() - 1) + "/100" : cleanRarity;
                    String[] parts = StringUtils.split(fraction, '/');
                    if (parts.length != 2) throw new IllegalArgumentException(rarity);
                    double d = Double.parseDouble(parts[1]) / Double.parseDouble(parts[0]);
                    denom = BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_EVEN);
                    break;
            }

            return new Transformed(item, rolls, denom, q, min, max);
        }
    }

    @Value
    static class Transformed {
        @SerializedName("i") int itemId;
        @SerializedName("r") Integer rolls;
        @SerializedName("d") BigDecimal denominator;
        @SerializedName("q") Integer quantity;
        @SerializedName("m") Integer quantMin;
        @SerializedName("n") Integer quantMax;
    }
}
