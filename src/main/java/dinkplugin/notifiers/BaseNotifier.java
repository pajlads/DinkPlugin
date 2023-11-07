package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.util.WorldUtils;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Set;

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
        Set<WorldType> world = client.getWorldType();
        if (config.ignoreSeasonal() && world.contains(WorldType.SEASONAL)) {
            return false;
        }
        if (WorldUtils.isIgnoredWorld(world)) {
            return false;
        }
        return settingsManager.isNamePermitted(client.getLocalPlayer().getName());
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
