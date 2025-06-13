package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.SeasonalPolicy;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.util.AccountTypeTracker;
import dinkplugin.util.WorldTypeTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@Slf4j
public abstract class BaseNotifier {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected AccountTypeTracker accountTracker;

    @Inject
    protected WorldTypeTracker worldTracker;

    @Inject
    protected Client client;

    @Inject
    private DiscordMessageHandler messageHandler;

    public boolean isEnabled() {
        return worldTracker.hasValidState() && accountTracker.hasValidState();
    }

    protected abstract String getWebhookUrl();

    protected final void createMessage(boolean sendImage, NotificationBody<?> body) {
        this.createMessage(getWebhookUrl(), sendImage, body);
    }

    protected final void createMessage(String overrideUrl, boolean sendImage, NotificationBody<?> body) {
        String override;
        if (StringUtils.isNotBlank(config.leaguesWebhook()) && config.seasonalPolicy() == SeasonalPolicy.FORWARD_TO_LEAGUES && client.getWorldType().contains(WorldType.SEASONAL)) {
            override = config.leaguesWebhook();
        } else {
            override = overrideUrl;
        }
        String url = StringUtils.isNotBlank(override) ? override : config.primaryWebhook();
        messageHandler.createMessage(url, sendImage, body);
    }

}
