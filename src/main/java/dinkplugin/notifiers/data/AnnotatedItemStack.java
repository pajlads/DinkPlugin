package dinkplugin.notifiers.data;

import dinkplugin.domain.LootCriteria;
import dinkplugin.util.Sanitizable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AnnotatedItemStack extends SerializedItemStack implements Sanitizable {
    private final Set<LootCriteria> criteria;

    public AnnotatedItemStack(int id, int quantity, int priceEach, String name, Set<LootCriteria> criteria) {
        super(id, quantity, priceEach, name);
        this.criteria = criteria;
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<>(super.sanitized());
        m.put("criteria", criteria.stream().map(Objects::toString).collect(Collectors.toSet()));
        return m;
    }

    public static AnnotatedItemStack of(SerializedItemStack i, Set<LootCriteria> criteria) {
        return new AnnotatedItemStack(i.getId(), i.getQuantity(), i.getPriceEach(), i.getName(), criteria);
    }
}
