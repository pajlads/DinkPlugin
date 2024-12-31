package dinkplugin.notifiers.data;

import dinkplugin.domain.LootCriteria;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AnnotatedItemStack extends SerializedItemStack {
    private final Set<LootCriteria> criteria;

    public AnnotatedItemStack(int id, int quantity, int priceEach, String name, Set<LootCriteria> criteria) {
        super(id, quantity, priceEach, name);
        this.criteria = criteria;
    }

    public static AnnotatedItemStack of(SerializedItemStack i, Set<LootCriteria> criteria) {
        return new AnnotatedItemStack(i.getId(), i.getQuantity(), i.getPriceEach(), i.getName(), criteria);
    }
}
