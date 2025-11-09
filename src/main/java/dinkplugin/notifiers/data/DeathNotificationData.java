package dinkplugin.notifiers.data;

import dinkplugin.util.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeathNotificationData extends NotificationData {

    long valueLost;

    boolean isPvp;

    /**
     * @deprecated in favor of {@link #killerName}
     */
    @Nullable
    @Deprecated
    String pker;

    @Nullable
    String killerName;

    @Nullable
    Integer killerNpcId;

    @NotNull
    Collection<SerializedItemStack> keptItems;

    @NotNull
    Collection<SerializedItemStack> lostItems;

    @NotNull
    Region location;

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("valueLost", valueLost);
        m.put("isPvp", isPvp);
        if (killerName != null) m.put("killerName", killerName);
        if (killerNpcId != null) m.put("killerNpcId", killerNpcId);
        m.put("keptItems", keptItems.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        m.put("lostItems", lostItems.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        m.put("location", location.sanitized());
        return m;
    }
}
