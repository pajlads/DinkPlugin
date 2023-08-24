package dinkplugin.util;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
public class SerializedLoot {
    private int kills;
}
