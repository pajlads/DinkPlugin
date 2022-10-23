package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.SlayerNotificationData;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;

@Getter
@Setter
public class SlayerNotifier extends BaseNotifier {
    private String slayerTask = "";
    private String slayerPoints = "";
    private String slayerCompleted = "";

    @Inject
    public SlayerNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        if (plugin.isIgnoredWorld()) return;
        // Little jank, but it's a bit cleaner than having bools and checking in the main plugin class
        if (slayerPoints.isEmpty() || slayerTask.isEmpty() || slayerCompleted.isEmpty()) {
            return;
        }

        String notifyMessage = config.slayerNotifyMessage()
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
        body.setType(NotificationType.SLAYER);
        messageHandler.createMessage(config.slayerSendImage(), body);

        slayerTask = "";
        slayerPoints = "";
        slayerCompleted = "";
    }
}
