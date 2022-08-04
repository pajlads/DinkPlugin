package universalDiscord;

import javax.inject.Inject;

public class DeathNotifier {
    private final UniversalDiscordPlugin plugin;

    @Inject
    public DeathNotifier(UniversalDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify() {
        String notifyMessage = plugin.config.deathNotifyMessage().replaceAll("%USERNAME%", Utils.getPlayerName());
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.deathSendImage(), null);
    }
}
