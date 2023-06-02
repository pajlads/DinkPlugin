package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

}
