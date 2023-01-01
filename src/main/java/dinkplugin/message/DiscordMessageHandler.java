package dinkplugin.message;

import com.google.gson.Gson;
import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.util.Utils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DiscordMessageHandler {
    private static final Footer FOOTER = Footer.builder()
        .text("Powered by Dink")
        .iconUrl("https://github.com/pajlads/DinkPlugin/raw/master/icon.png")
        .build();

    private final Gson gson;
    private final Client client;
    private final DrawManager drawManager;
    private final OkHttpClient httpClient;
    private final DinkPluginConfig config;
    private final ScheduledExecutorService executor;

    @Inject
    public DiscordMessageHandler(Gson gson, Client client, DrawManager drawManager, OkHttpClient httpClient, DinkPluginConfig config, ScheduledExecutorService executor) {
        this.gson = gson;
        this.client = client;
        this.drawManager = drawManager;
        this.config = config;
        this.executor = executor;
        this.httpClient = httpClient.newBuilder()
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .header("User-Agent", DinkPlugin.USER_AGENT)
                    .build();
                Interceptor.Chain updatedChain = chain;
                // Allow longer timeout when writing a screenshot file to overcome slow internet speeds
                if (request.body() instanceof MultipartBody && Utils.hasImage((MultipartBody) request.body())) {
                    updatedChain = chain.withWriteTimeout(Math.max(config.imageWriteTimeout(), 0), TimeUnit.SECONDS);
                }
                return updatedChain.proceed(request);
            })
            .build();
    }

    public <T> void createMessage(String webhookUrl, boolean sendImage, @NonNull NotificationBody<T> mBody) {
        if (StringUtils.isBlank(webhookUrl)) return;

        Collection<HttpUrl> urlList = Arrays.stream(StringUtils.split(webhookUrl, '\n'))
            .filter(StringUtils::isNotBlank)
            .map(HttpUrl::parse)
            .collect(Collectors.toList());
        if (urlList.isEmpty()) return;

        if (mBody.getPlayerName() == null)
            mBody.setPlayerName(Utils.getPlayerName(client));

        injectContent(mBody, sendImage);

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", gson.toJson(mBody));

        if (sendImage) {
            drawManager.requestNextFrameListener(image -> {
                try {
                    byte[] imageBytes = Utils.convertImageToByteArray(ImageUtil.bufferedImageFromImage(image));

                    reqBodyBuilder.addFormDataPart(
                        "file",
                        mBody.getScreenshotFile(),
                        RequestBody.create(
                            MediaType.parse("image/png"),
                            imageBytes
                        )
                    );
                } catch (IOException e) {
                    log.warn("There was an error creating bytes from captured image", e);
                } finally {
                    sendToMultiple(urlList, reqBodyBuilder);
                }
            });
        } else {
            sendToMultiple(urlList, reqBodyBuilder);
        }
    }

    private void sendToMultiple(Collection<HttpUrl> urls, MultipartBody.Builder reqBodyBuilder) {
        urls.forEach(url -> sendMessage(url, reqBodyBuilder, 0));
    }

    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody, int attempt) {
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.trace(String.format("Failed to send webhook message to %s on attempt %d", url, attempt), e);

                if (attempt == 0) {
                    log.warn("There was an error sending the webhook message", e);
                }

                int maxRetries = config.maxRetries();
                if (attempt < maxRetries) {
                    long baseDelay = config.baseRetryDelay();
                    if (baseDelay > 0) {
                        long delay = baseDelay * (1L << Math.min(attempt, 16)); // exponential backoff
                        executor.schedule(() -> sendMessage(url, requestBody, attempt + 1), delay, TimeUnit.MILLISECONDS);
                        log.debug("Scheduled webhook message for retry in {} milliseconds", delay);
                    } else {
                        log.debug("Skipping retry attempts for failed webhook since base delay is not positive");
                    }
                } else if (maxRetries > 0) {
                    log.warn("Exhausted retry attempts when sending the webhook message", e);
                } else {
                    log.debug("Skipping retry attempts for failed webhook since max retries is not positive");
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                log.trace("Successfully sent webhook message to {} after {} attempts", url, attempt + 1);
                response.close();
            }
        });
    }

    private static void injectContent(@NotNull NotificationBody<?> body, boolean screenshot) {
        List<Embed> embeds = new ArrayList<>(body.getEmbeds() != null ? body.getEmbeds() : Collections.emptyList());
        embeds.add(0,
            Embed.builder()
                .author(new Author(body.getPlayerName()))
                .color(Utils.PINK)
                .title(body.getType().getTitle())
                .description(body.getContent())
                .image(screenshot ? new Embed.UrlEmbed("attachment://" + body.getScreenshotFile()) : null)
                .footer(FOOTER)
                .timestamp(Instant.now())
                .build()
        );
        body.setEmbeds(embeds);
    }
}
