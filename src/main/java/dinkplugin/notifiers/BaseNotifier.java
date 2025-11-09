package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.domain.SeasonalPolicy;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.util.AccountTypeTracker;
import dinkplugin.util.WorldTypeTracker;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

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
    protected EventBus eventBus;

    @Inject
    protected ScheduledExecutorService executor;

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
        // determine target url
        String override;
        if (StringUtils.isNotBlank(config.leaguesWebhook()) && config.seasonalPolicy() == SeasonalPolicy.FORWARD_TO_LEAGUES && WorldUtils.isSeasonal(client)) {
            override = config.leaguesWebhook();
        } else {
            override = overrideUrl;
        }
        String url = StringUtils.isNotBlank(override) ? override : config.primaryWebhook();

        // post notification to target url
        messageHandler.createMessage(url, sendImage, body);

        // notify other hub plugins
        executor.execute(() -> {
            Map<String, Object> metadata = body.getExtra() != null ? body.getExtra().sanitized() : Collections.emptyMap();
            var payload = new PluginMessage(SettingsManager.CONFIG_GROUP, body.getType().name(), metadata);
            eventBus.post(payload);
        });
    }

}
