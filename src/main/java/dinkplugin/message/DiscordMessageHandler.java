package dinkplugin.message;

import com.google.gson.Gson;
import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.notifiers.data.NotificationData;
import dinkplugin.util.Utils;
import lombok.NonNull;
import lombok.SneakyThrows;
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
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DiscordMessageHandler {
    private final Gson gson;
    private final Client client;
    private final DrawManager drawManager;
    private final OkHttpClient httpClient;
    private final DinkPluginConfig config;
    private final ScheduledExecutorService executor;

    @Inject
    @VisibleForTesting
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

    public void createMessage(String webhookUrl, boolean sendImage, @NonNull NotificationBody<?> mBody) {
        if (StringUtils.isBlank(webhookUrl)) return;

        Collection<HttpUrl> urlList = Arrays.stream(StringUtils.split(webhookUrl, '\n'))
            .filter(StringUtils::isNotBlank)
            .map(HttpUrl::parse)
            .collect(Collectors.toList());
        if (urlList.isEmpty()) return;

        if (mBody.getPlayerName() == null)
            mBody = mBody.withPlayerName(Utils.getPlayerName(client));

        if (mBody.getAccountType() == null)
            mBody = mBody.withAccountType(client.getAccountType());

        if (config.discordRichEmbeds()) {
            mBody = injectContent(mBody, sendImage, config.embedFooterText(), config.embedFooterIcon());
        } else {
            mBody = mBody.withComputedDiscordContent(mBody.getText());
        }

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", gson.toJson(mBody));

        if (sendImage) {
            String screenshotFile = mBody.getType().getScreenshot();
            drawManager.requestNextFrameListener(image -> {
                try {
                    byte[] imageBytes = Utils.convertImageToByteArray(ImageUtil.bufferedImageFromImage(image));

                    reqBodyBuilder.addFormDataPart(
                        "file",
                        screenshotFile,
                        RequestBody.create(
                            MediaType.parse("image/png"),
                            imageBytes
                        )
                    );
                } catch (Exception e) {
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

    @SneakyThrows
    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody, int attempt) {
        CountDownLatch latch = isAsync() ? null : new CountDownLatch(1);
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

                if (latch != null)
                    latch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                log.trace("Successfully sent webhook message to {} after {} attempts", url, attempt + 1);
                response.close();

                if (latch != null)
                    latch.countDown();
            }
        });

        if (latch != null)
            latch.await();
    }

    @VisibleForTesting
    public boolean isAsync() {
        return true;
    }

    private static NotificationBody<?> injectContent(@NotNull NotificationBody<?> body, boolean screenshot, String footerText, String footerIcon) {
        NotificationType type = body.getType();
        NotificationData extra = body.getExtra();

        Author author = Author.builder()
            .name(body.getPlayerName())
            .iconUrl(Utils.getChatBadge(body.getAccountType()))
            .build();
        Footer footer = StringUtils.isBlank(footerText) ? null : Footer.builder()
            .text(StringUtils.truncate(footerText, Embed.MAX_FOOTER_LENGTH))
            .iconUrl(StringUtils.isBlank(footerIcon) ? null : footerIcon)
            .build();

        List<Embed> embeds = new ArrayList<>(body.getEmbeds() != null ? body.getEmbeds() : Collections.emptyList());
        embeds.add(0,
            Embed.builder()
                .author(author)
                .color(Utils.PINK)
                .title(type.getTitle())
                .description(StringUtils.truncate(body.getText(), Embed.MAX_DESCRIPTION_LENGTH))
                .image(screenshot ? new Embed.UrlEmbed("attachment://" + type.getScreenshot()) : null)
                .thumbnail(new Embed.UrlEmbed(type.getThumbnail()))
                .fields(extra != null ? extra.getFields() : Collections.emptyList())
                .footer(footer)
                .timestamp(footer != null ? Instant.now() : null)
                .build()
        );
        return body.withEmbeds(embeds);
    }
}
