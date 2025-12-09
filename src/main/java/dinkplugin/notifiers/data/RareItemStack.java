package dinkplugin.notifiers.data;

import dinkplugin.domain.LootCriteria;
import dinkplugin.util.Sanitizable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RareItemStack extends AnnotatedItemStack implements Sanitizable {
    private final double rarity;

    public RareItemStack(int id, int quantity, int priceEach, String name, Set<LootCriteria> criteria, double rarity) {
        super(id, quantity, priceEach, name, criteria);
        this.rarity = rarity;
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = super.sanitized();
        m.put("rarity", rarity);
        return m;
    }

    public static RareItemStack of(AnnotatedItemStack i, double rarity) {
        return new RareItemStack(i.getId(), i.getQuantity(), i.getPriceEach(), i.getName(), i.getCriteria(), rarity);
    }
}
