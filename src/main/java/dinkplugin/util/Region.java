package dinkplugin.util;

import lombok.Value;
import net.runelite.api.Client;

@Value
public class Region {
    int regionId;
    int plane;
    boolean instanced;

    public static Region of(Client client, int regionId) {
        return new Region(regionId, client.getPlane(), client.isInInstancedRegion());
    }

    public static Region of(Client client) {
        return of(client, WorldUtils.getLocation(client).getRegionID());
    }
}
