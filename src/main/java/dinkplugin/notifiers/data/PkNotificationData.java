package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Utils;
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
        List<Field> fields = new ArrayList<>(1 + equipment.size());

        fields.add(
            new Field(
                "Location",
                Field.formatBlock("",
                    String.format("X: %d, Y: %d, Z: %d, World: %d", location.getX(), location.getY(), location.getPlane(), world)
                ),
                false
            )
        );

        equipment.forEach((slot, item) -> {
            String key = Utils.ucFirst(slot.toString());
            String value = Field.formatBlock("", item.getName());
            fields.add(new Field(key, value));
        });

        return fields;
    }
}
