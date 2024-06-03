package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RareItemStack extends SerializedItemStack {
    private final Double rarity;

    public RareItemStack(int id, int quantity, int priceEach, String name, Double rarity) {
        super(id, quantity, priceEach, name);
        this.rarity = rarity;
    }

    public static RareItemStack of(SerializedItemStack i, Double rarity) {
        return new RareItemStack(i.getId(), i.getQuantity(), i.getPriceEach(), i.getName(), rarity);
    }
}
