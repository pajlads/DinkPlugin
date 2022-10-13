package universalDiscord;

import lombok.Builder;

@Builder
class SerializedItemStack {
    private int id;
    private int quantity;
    private int priceEach;
    private String name;
}
