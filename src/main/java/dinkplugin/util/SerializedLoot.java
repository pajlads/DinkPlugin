package dinkplugin.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

/**
 * Contains kill count observed by base runelite loot tracker plugin, stored in profile configuration.
 *
 * @see <a href="https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/ConfigLoot.java#L41">RuneLite class</a>
 */
@Data
@With
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class SerializedLoot {
    private int kills;
    private int[] drops;

    public int getQuantity(int itemId) {
        final int n = drops != null ? drops.length : 0;
        for (int i = 0; i < n; i += 2) {
            if (drops[i] == itemId) {
                return drops[i + 1];
            }
        }
        return 0;
    }
}
