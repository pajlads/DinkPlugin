package universalDiscord;

import javax.inject.Inject;

public class BaseNotifier {
    protected final UniversalDiscordPlugin plugin;

    @Inject
    public BaseNotifier(UniversalDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify() {
        plugin.messageHandler.createMessage("This is a base notification", false, null);
    }
}
