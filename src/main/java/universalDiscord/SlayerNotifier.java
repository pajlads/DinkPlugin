package universalDiscord;

import javax.inject.Inject;

public class SlayerNotifier extends BaseNotifier {

    @Inject
    public SlayerNotifier(UniversalDiscordPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String task, String points, String taskCount) {
        String notifyMessage = plugin.config.slayerNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%TASK%", task)
                .replaceAll("%TASKCOUNT%", taskCount)
                .replaceAll("%POINTS%", points);
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.slayerSendImage(), null);
    }
}
