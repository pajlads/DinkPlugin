package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.util.WorldUtils;
import net.runelite.api.Client;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

public abstract class BaseNotifier {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected SettingsManager settingsManager;

    @Inject
    protected Client client;

    @Inject
    private DiscordMessageHandler messageHandler;

    public boolean isEnabled() {
        return !WorldUtils.isIgnoredWorld(client.getWorldType()) && settingsManager.isNamePermitted(client.getLocalPlayer().getName());
    }

    protected abstract String getWebhookUrl();

    protected final void createMessage(boolean sendImage, NotificationBody<?> body) {
        this.createMessage(getWebhookUrl(), sendImage, body);
    }

    protected final void createMessage(String overrideUrl, boolean sendImage, NotificationBody<?> body) {
        String url = StringUtils.isNotBlank(overrideUrl) ? overrideUrl : config.primaryWebhook();
        messageHandler.createMessage(url, sendImage, body);
    }

}
