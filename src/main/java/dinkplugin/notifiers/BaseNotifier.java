package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import net.runelite.api.Client;

import javax.inject.Inject;
import java.util.function.Function;

public abstract class BaseNotifier {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    protected DiscordMessageHandler messageHandler;

    public boolean isEnabled() {
        return !Utils.isIgnoredWorld(client.getWorldType());
    }

    protected final void createMessage(Function<DinkPluginConfig, Boolean> sendImage, NotificationBody<?> body) {
        messageHandler.createMessage(sendImage.apply(config), body);
    }

}
