package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import net.runelite.api.Client;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.function.Function;

public abstract class BaseNotifier {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    private DiscordMessageHandler messageHandler;

    public boolean isEnabled() {
        return !Utils.isIgnoredWorld(client.getWorldType());
    }

    protected abstract String getWebhookUrl();

    protected final void createMessage(Function<DinkPluginConfig, Boolean> sendImage, NotificationBody<?> body) {
        String overrideUrl = getWebhookUrl();
        String url = StringUtils.isNotBlank(overrideUrl) ? overrideUrl : config.primaryWebhook();
        messageHandler.createMessage(url, sendImage.apply(config), body);
    }

}
