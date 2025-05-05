package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.domain.FilterMode;
import dinkplugin.domain.SeasonalPolicy;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.util.Utils;
import dinkplugin.util.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Set;

@Slf4j
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
        if (config.seasonalPolicy() == SeasonalPolicy.REJECT && world.contains(WorldType.SEASONAL)) {
            return false;
        }
        if (WorldUtils.isIgnoredWorld(world)) {
            return false;
        }
        return accountPassesConfig();
    }

    /**
     * @return whether the player name passes the configured filtered RSNs and the account type is permitted
     */
    protected boolean accountPassesConfig() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        if (config.nameFilterMode() != FilterMode.ALLOW && config.deniedAccountTypes().contains(Utils.getAccountType(client))) {
            log.info("Skipping notification from {} due to denied account type", getClass().getSimpleName());
            return false;
        }
        return settingsManager.isNamePermitted(player.getName());
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
