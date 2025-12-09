package dinkplugin.notifiers.data;

import dinkplugin.util.Sanitizable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class SerializedItemStack implements Sanitizable {
    private int id;
    private int quantity;
    private int priceEach;
    private String name;

    public long getTotalPrice() {
        return (long) priceEach * quantity;
    }

    @Override
    public Map<String, Object> sanitized() {
        return Map.of("id", id, "quantity", quantity, "priceEach", priceEach, "name", name);
    }
}
