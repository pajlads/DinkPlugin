package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class PkNotificationData extends NotificationData {

    String name;
    Map<KitType, SerializedItemStack> equipment;
    int world;
    WorldPoint location;
    int myHitpoints;
    int myLastDamage;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);

        fields.add(
            new Field(
                "World",
                Field.formatBlock("", String.valueOf(world))
            )
        );

        fields.add(
            new Field(
                "Location",
                Field.formatBlock("",
                    String.format("X: %d, Y: %d, Plane: %d", location.getX(), location.getY(), location.getPlane())
                )
            )
        );

        return fields;
    }
}
