package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.Field;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.SimpleReplacement;
import dinkplugin.notifiers.data.ExternalNotificationData;
import net.runelite.client.events.PluginMessage;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExternalPluginNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    ExternalPluginNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // update config mocks
        when(config.notifyExternal()).thenReturn(true);
        when(config.externalImageOverride()).thenReturn(true);
    }

    @Test
    void testNotify() {
        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", samplePayload("https://example.com/")));

        // verify notification
        verifyCreateMessage(
            "https://example.com/",
            false,
            NotificationBody.builder()
                .type(NotificationType.EXTERNAL_PLUGIN)
                .playerName(PLAYER_NAME)
                .customTitle("My Title")
                .customFooter("Sent by MyExternalPlugin via Dink")
                .text(
                    Template.builder()
                        .template("Hello %TARGET% from %USERNAME%")
                        .replacement("%TARGET%", Replacements.ofText("world"))
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .build()
                )
                .extra(new ExternalNotificationData("MyExternalPlugin", List.of(new Field("sample key", "sample value")), Collections.singletonMap("hello", "world")))
                .build()
        );
    }

    @Test
    void testFallbackUrl() {
        // update config mocks
        String url = "https://discord.com/example";
        when(config.externalWebhook()).thenReturn(url);

        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", samplePayload(null)));

        // verify notification
        verifyCreateMessage(
            url,
            false,
            NotificationBody.builder()
                .type(NotificationType.EXTERNAL_PLUGIN)
                .playerName(PLAYER_NAME)
                .customTitle("My Title")
                .customFooter("Sent by MyExternalPlugin via Dink")
                .text(
                    Template.builder()
                        .template("Hello %TARGET% from %USERNAME%")
                        .replacement("%TARGET%", Replacements.ofText("world"))
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .build()
                )
                .extra(new ExternalNotificationData("MyExternalPlugin", List.of(new Field("sample key", "sample value")), Collections.singletonMap("hello", "world")))
                .build()
        );
    }

    @Test
    void testImage() {
        // update config mocks
        when(config.externalSendImage()).thenReturn(true);

        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", samplePayload(null)));

        // verify notification
        verifyCreateMessage(
            null,
            true,
            NotificationBody.builder()
                .type(NotificationType.EXTERNAL_PLUGIN)
                .playerName(PLAYER_NAME)
                .customTitle("My Title")
                .customFooter("Sent by MyExternalPlugin via Dink")
                .text(
                    Template.builder()
                        .template("Hello %TARGET% from %USERNAME%")
                        .replacement("%TARGET%", Replacements.ofText("world"))
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .build()
                )
                .extra(new ExternalNotificationData("MyExternalPlugin", List.of(new Field("sample key", "sample value")), Collections.singletonMap("hello", "world")))
                .build()
        );
    }

    @Test
    void testRequestImage() {
        // prepare payload
        var data = samplePayload(null);
        data.put("imageRequested", true);

        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", data));

        // verify notification
        verifyCreateMessage(
            null,
            true,
            NotificationBody.builder()
                .type(NotificationType.EXTERNAL_PLUGIN)
                .playerName(PLAYER_NAME)
                .customTitle("My Title")
                .customFooter("Sent by MyExternalPlugin via Dink")
                .text(
                    Template.builder()
                        .template("Hello %TARGET% from %USERNAME%")
                        .replacement("%TARGET%", Replacements.ofText("world"))
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .build()
                )
                .extra(new ExternalNotificationData("MyExternalPlugin", List.of(new Field("sample key", "sample value")), Collections.singletonMap("hello", "world")))
                .build()
        );
    }

    @Test
    void testRequestImageDenied() {
        // update config mocks
        when(config.externalImageOverride()).thenReturn(false);

        // prepare payload
        var data = samplePayload(null);
        data.put("imageRequested", true);

        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", data));

        // verify notification
        verifyCreateMessage(
            null,
            false,
            NotificationBody.builder()
                .type(NotificationType.EXTERNAL_PLUGIN)
                .playerName(PLAYER_NAME)
                .customTitle("My Title")
                .customFooter("Sent by MyExternalPlugin via Dink")
                .text(
                    Template.builder()
                        .template("Hello %TARGET% from %USERNAME%")
                        .replacement("%TARGET%", Replacements.ofText("world"))
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .build()
                )
                .extra(new ExternalNotificationData("MyExternalPlugin", List.of(new Field("sample key", "sample value")), Collections.singletonMap("hello", "world")))
                .build()
        );
    }

    @Test
    void testIgnoreNamespace() {
        // fire event
        plugin.onPluginMessage(new PluginMessage("DANK", "notify", samplePayload("https://example.com/")));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreName() {
        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "donk", samplePayload("https://example.com/")));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // update config mocks
        when(config.notifyExternal()).thenReturn(false);

        // fire event
        plugin.onPluginMessage(new PluginMessage("dink", "notify", samplePayload("https://example.com/")));

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static Map<String, Object> samplePayload(String url) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Hello %TARGET% from %USERNAME%");
        data.put("title", "My Title");
        data.put("thumbnail", "not a url . com");
        data.put("fields", List.of(new Field("sample key", "sample value")));
        data.put("replacements", Map.of("%TARGET%", new SimpleReplacement("world", null)));
        data.put("metadata", Map.of("hello", "world"));
        data.put("sourcePlugin", "MyExternalPlugin");
        if (url != null) {
            data.put("urls", Collections.singletonList(HttpUrl.parse(url)));
        }
        return data;
    }

}
