package dinkplugin.notifiers.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder
@AllArgsConstructor
public class SerializedItemStack {
    private int id;
    private @With int quantity;
    private int priceEach;
    private String name;

    public long getTotalPrice() {
        return (long) priceEach * quantity;
    }
}
