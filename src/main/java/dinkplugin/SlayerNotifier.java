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
        NotificationBody<SlayerNotificationData> body = new NotificationBody<>();
        SlayerNotificationData extra = new SlayerNotificationData();
        extra.setSlayerPoints(slayerPoints);
        extra.setSlayerCompleted(slayerCompleted);
        extra.setSlayerTask(slayerTask);
        body.setExtra(extra);
        body.setContent(notifyMessage);
        plugin.messageHandler.createMessage(plugin.config.slayerSendImage(), body);

        slayerTask = "";
        slayerPoints = "";
        slayerCompleted = "";
    }
}
