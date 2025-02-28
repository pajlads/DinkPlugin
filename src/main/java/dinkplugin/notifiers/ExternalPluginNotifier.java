package dinkplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.domain.ExternalNotificationRequest;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.ExternalNotificationData;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import okhttp3.HttpUrl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Image;
import java.util.Map;

@Slf4j
@Singleton
public class ExternalPluginNotifier extends BaseNotifier {

    @Inject
    private Gson gson;

    @Override
    public boolean isEnabled() {
        return config.notifyExternal() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.externalWebhook();
    }

    public void onNotify(Map<String, Object> data) {
        if (!isEnabled()) {
            log.debug("Skipping requested external dink since notifier is disabled: {}", data);
            return;
        }

        // parse request
        ExternalNotificationRequest input;
        try {
            input = gson.fromJson(gson.toJsonTree(data), ExternalNotificationRequest.class);
        } catch (JsonSyntaxException e) {
            log.warn("Failed to parse requested webhook notification from an external plugin: {}", data, e);
            return;
        }

        var image = data.get("image");
        if (image instanceof Image) {
            input.setImage((Image) image);
        }

        // validate request
        if (input.getSourcePlugin() == null || input.getSourcePlugin().isBlank()) {
            log.info("Skipping externally-requested dink due to missing 'sourcePlugin': {}", data);
            return;
        }

        if (input.getText() == null || input.getText().isBlank()) {
            log.info("Skipping externally-requested dink due to missing 'text': {}", data);
            return;
        }

        if (input.getThumbnail() != null && HttpUrl.parse(input.getThumbnail()) == null) {
            log.debug("Replacing invalid thumbnail url: {}", input.getThumbnail());
            input.setThumbnail(NotificationType.EXTERNAL_PLUGIN.getThumbnail());
        }

        // process request
        this.handleNotify(input);
    }

    private void handleNotify(ExternalNotificationRequest input) {
        var player = Utils.getPlayerName(client);
        var template = Template.builder()
            .template(input.getText())
            .replacements(input.getReplacements())
            .replacement("%USERNAME%", Replacements.ofText(player))
            .build();

        var footer = String.format("Sent by %s, via Dink", input.getSourcePlugin());

        boolean image = input.getImage() != null ||
            client.getGameState().getState() >= GameState.LOGGING_IN.getState()
                && (config.externalSendImage() || input.isImageRequested() && config.externalImageOverride());
        var body = NotificationBody.builder()
            .type(NotificationType.EXTERNAL_PLUGIN)
            .playerName(player)
            .text(template)
            .customTitle(input.getTitle())
            .customFooter(footer)
            .thumbnailUrl(input.getThumbnail())
            .extra(new ExternalNotificationData(input.getFields()))
            .screenshotOverride(input.getImage())
            .build();

        var url = input.getSanitizedUrl();
        if (!url.isEmpty()) {
            log.info("{} requested a dink notification to an externally-specified url", input.getSourcePlugin());
        }

        createMessage(url, image, body);
    }

}
