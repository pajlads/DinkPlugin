package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;

import javax.inject.Inject;

public class BaseNotifier {
    protected final DinkPlugin plugin;
    protected final DiscordMessageHandler messageHandler;

    @Inject
    public BaseNotifier(DinkPlugin plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
    }

    public void handleNotify() {
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent("This is a base notification");
        messageHandler.createMessage(false, b);
    }
}
