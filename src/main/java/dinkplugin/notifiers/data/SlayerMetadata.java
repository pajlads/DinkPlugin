package dinkplugin.notifiers.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import org.jetbrains.annotations.Nullable;

@Data
@Setter(AccessLevel.NONE)
@RequiredArgsConstructor
@AllArgsConstructor
public class SlayerMetadata {
    private final boolean onTask;
    private @Nullable String task;
    private @Nullable String taskLocation;
    private @Nullable Integer initialAmount;
    private @Nullable Integer remainingAmount;

    public static SlayerMetadata from(boolean onTask, SlayerPluginService service) {
        if (!onTask) {
            return new SlayerMetadata(false);
        }
        return new SlayerMetadata(true, service.getTask(), service.getTaskLocation(), service.getInitialAmount(), service.getRemainingAmount());
    }
}
