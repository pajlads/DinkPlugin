package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class PlayerKillNotificationData extends NotificationData {

    String victimName;
    int victimCombatLevel;
    Map<KitType, SerializedItemStack> victimEquipment;
    @Nullable Integer world;
    @Nullable WorldPoint location;
    int myHitpoints;
    int myLastDamage;

    @Override
    public List<Field> getFields() {
        if (location == null)
            return super.getFields();

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

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("victimName", victimName);
        m.put("victimCombatLevel", victimCombatLevel);
        m.put("myHitpoints", myHitpoints);
        m.put("myLastDamage", myLastDamage);
        m.put("victimEquipment", victimEquipment.entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().sanitized()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        if (world != null) m.put("world", world);
        if (location != null) m.put("location", location);
        return m;
    }
}
