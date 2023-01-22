package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class LootNotificationData extends NotificationData {

    @NotNull
    List<SerializedItemStack> items;

    @NotNull
    String source;

    @Nullable
    @EqualsAndHashCode.Exclude
    Double rarity;

    public LootNotificationData(List<SerializedItemStack> items, String source) {
        this(items, source, null);
    }

    @Override
    public List<Field> getFields() {
        List<Field> fields = new LinkedList<>();

        fields.add(
            new Field(
                "Total Value",
                ItemUtils.formatGold(items.stream().mapToLong(SerializedItemStack::getTotalPrice).sum())
            )
        );

        if (rarity != null) {
            fields.add(
                new Field(
                    "Rarity",
                    String.format(
                        "```sql\n1 in %.1f (%s%%)\n```",
                        1 / rarity,
                        new BigDecimal(rarity * 100)
                            .round(new MathContext(3))
                            .toPlainString()
                    )
                )
            );
        }

        return fields;
    }
}
