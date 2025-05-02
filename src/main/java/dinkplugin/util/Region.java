package dinkplugin.util;

import lombok.Value;
import net.runelite.api.Client;

import java.util.Objects;

@Value
public class Region {
    int regionId;
    int plane;
    boolean instanced;

    public static Region of(Client client, int regionId) {
        var wv = client.getTopLevelWorldView();
        return new Region(regionId, wv.getPlane(), wv.isInstance());
    }

    public static Region of(Client client) {
        return of(client, Objects.requireNonNull(WorldUtils.getLocation(client)).getRegionID());
    }
}
