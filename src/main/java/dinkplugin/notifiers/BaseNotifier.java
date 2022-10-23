package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;

import javax.inject.Inject;

public class BaseNotifier {
    protected final DinkPlugin plugin;
    protected final DiscordMessageHandler messageHandler;
    protected final DinkPluginConfig config;

    @Inject
    public BaseNotifier(DinkPlugin plugin) {
        this.plugin = plugin;
        this.messageHandler = plugin.getMessageHandler();
        this.config = plugin.getConfig();
    }

    public void handleNotify() {
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent("This is a base notification");
        messageHandler.createMessage(false, b);
    }
}
