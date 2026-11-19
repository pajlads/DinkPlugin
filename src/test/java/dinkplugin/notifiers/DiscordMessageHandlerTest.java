package dinkplugin.notifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import net.runelite.api.WorldType;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unlike the notifier tests, these inspect the serialized webhook payload, since
 * {@link #verifyCreateMessage} only sees the body before the handler enriches it.
 */
class DiscordMessageHandlerTest extends MockedNotifierTest {

    private static final String WEBHOOK_URL = "https://webhook.invalid/dink";

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // update config mocks
        when(config.includeLocation()).thenReturn(true);

        // update client mocks
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS, WorldType.HARDCORE_MAIN));
    }

    @Test
    void testNotifyWorldTypes() throws Exception {
        // fire notification
        JsonObject payload = sendNotification();

        // verify payload
        JsonArray expected = new JsonArray();
        expected.add("MEMBERS");
        expected.add("HARDCORE_MAIN");
        assertEquals(expected, payload.get("worldTypes"));
    }

    @Test
    void testNotifyEmptyWorldTypes() throws Exception {
        // update client mocks
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));

        // fire notification
        JsonObject payload = sendNotification();

        // verify payload
        assertEquals(new JsonArray(), payload.get("worldTypes"));
    }

    @Test
    void testIgnoreWorldTypesWithoutLocation() throws Exception {
        // update config mocks
        when(config.includeLocation()).thenReturn(false);

        // fire notification
        JsonObject payload = sendNotification();

        // verify payload
        assertFalse(payload.has("worldTypes"));
    }

    private JsonObject sendNotification() throws Exception {
        CompletableFuture<String> payload = new CompletableFuture<>();
        OkHttpClient capturingClient = httpClient.newBuilder()
            .addInterceptor(chain -> {
                Buffer buffer = new Buffer();
                chain.request().body().writeTo(buffer);
                payload.complete(buffer.readUtf8());
                return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(MediaType.parse("application/json"), "{}"))
                    .build();
            })
            .build();
        DiscordMessageHandler handler = new DiscordMessageHandler(gson, client, drawManager, capturingClient, config, executor, clientThread, discordService, imageCapture);

        handler.createMessage(
            WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate("test"))
                .type(NotificationType.DEATH)
                .build()
        );

        return gson.fromJson(payload.get(10, TimeUnit.SECONDS), JsonObject.class);
    }

}
