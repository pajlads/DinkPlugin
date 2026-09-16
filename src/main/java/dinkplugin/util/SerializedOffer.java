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
    private long price;
    private long spent;

    public boolean equalsOffer(@NotNull GrandExchangeOffer o) {
        return state == o.getState() && id == o.getItemId() && quantity == o.getTotalQuantity()
            && price == (long) o.getPrice() && spent == (long) o.getSpent();
    }
}
