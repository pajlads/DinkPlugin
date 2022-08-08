package universalDiscord;

import javax.inject.Inject;

public class SlayerNotifier {
    private final UniversalDiscordPlugin plugin;

    @Inject
    public SlayerNotifier(UniversalDiscordPlugin plugin) {
        this.plugin = plugin;
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
