package dinkplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.domain.ExternalNotificationRequest;
import dinkplugin.domain.ExternalScreenshotPolicy;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.ExternalNotificationData;
import dinkplugin.util.HttpUrlAdapter;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import okhttp3.HttpUrl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Slf4j
@Singleton
public class ExternalPluginNotifier extends BaseNotifier {

    private Gson gson;

    @Override
    public boolean isEnabled() {
        return config.notifyExternal() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.externalWebhook();
    }

    @Inject
    void init(Gson gson) {
        this.gson = gson.newBuilder()
            .registerTypeAdapter(HttpUrl.class, new HttpUrlAdapter())
            .create();
    }

    public void onNotify(Map<String, Object> data) {
        if (!isEnabled()) {
            log.debug("Skipping requested external dink since notifier is disabled: {}", data);
            return;
        }

        // ensure input urls are specified correctly
        if (!isUrlInputValid(data.get("urls"))) {
            log.warn("Skipping externally-requested dink due to invalid 'urls' format from {}", data.get("sourcePlugin"));
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

        var footer = String.format("Sent by %s via Dink", input.getSourcePlugin());

        var policy = config.externalSendImage();
        boolean image = policy != ExternalScreenshotPolicy.NEVER
            && client.getGameState().getState() >= GameState.LOGGING_IN.getState()
            && (policy == ExternalScreenshotPolicy.ALWAYS || input.isImageRequested());
        var body = NotificationBody.builder()
            .type(NotificationType.EXTERNAL_PLUGIN)
            .playerName(player)
            .text(template)
            .customTitle(input.getTitle())
            .customFooter(footer)
            .thumbnailUrl(input.getThumbnail())
            .extra(new ExternalNotificationData(input.getSourcePlugin(), input.getFields(), input.getMetadata()))
            .build();

        var urls = input.getUrls(this::getWebhookUrl);
        createMessage(urls, image, body);
    }

    private static boolean isUrlInputValid(Object urls) {
        if (urls == null) {
            return true; // use urls specified in dink's config
        }
        if (!(urls instanceof Iterable)) {
            return false; // we try to convert to list in ExternalNotificationRequest
        }
        for (Object url : (Iterable<?>) urls) {
            if (!(url instanceof HttpUrl))
                return false; // received non-HttpUrl; input should be rejected
        }
        return true; // all elements are HttpUrl instances; proceed as normal
    }

}
