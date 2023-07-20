package dinkplugin.util;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.jetbrains.annotations.NotNull;

@Data
@Setter(AccessLevel.PRIVATE)
public class SerializedOffer {
    private GrandExchangeOfferState state;
    private @SerializedName("itemId") int id;
    private @SerializedName("totalQuantity") int quantity;
    private int price;
    private int spent;

    public boolean equals(@NotNull GrandExchangeOffer o) {
        return state == o.getState() && id == o.getItemId() && quantity == o.getTotalQuantity()
            && price == o.getPrice() && spent == o.getSpent();
    }
}
