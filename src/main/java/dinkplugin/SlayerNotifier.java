package dinkplugin;

import javax.inject.Inject;
import java.util.Objects;

public class SlayerNotifier extends BaseNotifier {
    public String slayerTask = "";
    public String slayerPoints = "";
    public String slayerCompleted = "";

    @Inject
    public SlayerNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        // Little jank, but it's a bit cleaner than having bools and checking in the main plugin class
        if (Objects.equals(slayerPoints, "")
            || Objects.equals(slayerTask, "")
            || Objects.equals(slayerCompleted, "")) {
            return;
        }

        String notifyMessage = plugin.config.slayerNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%TASK%", slayerTask)
            .replaceAll("%TASKCOUNT%", slayerCompleted)
            .replaceAll("%POINTS%", slayerPoints);
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.slayerSendImage(), null);

        slayerTask = "";
        slayerPoints = "";
        slayerCompleted = "";
    }
}
