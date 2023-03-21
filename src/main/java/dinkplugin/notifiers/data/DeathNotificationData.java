package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeathNotificationData extends NotificationData {

    long valueLost;

    boolean isPvp;

    /**
     * @deprecated in favor of {@link #getKillerName()}
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

}
