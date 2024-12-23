package dinkplugin.notifiers.data;

import dinkplugin.domain.LootCriteria;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RareItemStack extends AnnotatedItemStack {
    private final double rarity;

    public RareItemStack(int id, int quantity, int priceEach, String name, Set<LootCriteria> criteria, double rarity) {
        super(id, quantity, priceEach, name, criteria);
        this.rarity = rarity;
    }

    public static RareItemStack of(AnnotatedItemStack i, double rarity) {
        return new RareItemStack(i.getId(), i.getQuantity(), i.getPriceEach(), i.getName(), i.getCriteria(), rarity);
    }
}
