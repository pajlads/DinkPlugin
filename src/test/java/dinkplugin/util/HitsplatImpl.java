package dinkplugin.util;

import lombok.Value;
import net.runelite.api.Hitsplat;

@Value
public class HitsplatImpl implements Hitsplat {
    int hitsplatType;
    int amount;
    int disappearsOnGameCycle;
}
