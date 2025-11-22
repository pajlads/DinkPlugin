package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class SlayerNotificationData extends NotificationData {

    @NotNull
    String slayerTask;

    @NotNull
    String slayerCompleted;

    @NotNull
    String slayerPoints;

    @Nullable // if jagex changes format of slayerTask
    Integer killCount;

    @Nullable // if jagex changes format of slayerTask
    String monster;

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("slayerTask", slayerTask);
        m.put("slayerCompleted", slayerCompleted);
        m.put("slayerPoints", slayerPoints);
        if (killCount != null) m.put("killCount", killCount);
        if (monster != null) m.put("monster", monster);
        return m;
    }
}
