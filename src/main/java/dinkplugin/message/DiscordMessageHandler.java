package dinkplugin.message;

import com.google.gson.Gson;
import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.PlayerLookupService;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.NotificationData;
import dinkplugin.util.DiscordProfile;
import dinkplugin.util.Utils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Component;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.discord.DiscordService;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DiscordMessageHandler {
    public static final @Component int PRIVATE_CHAT_WIDGET = WidgetUtil.packComponentId(InterfaceID.PRIVATE_CHAT, 0);

    private final Gson gson;
    private final Client client;
    private final DrawManager drawManager;
    private final OkHttpClient httpClient;
    private final DinkPluginConfig config;
    private final ScheduledExecutorService executor;
    private final ClientThread clientThread;
    private final DiscordService discordService;

    @Inject
    @VisibleForTesting
    public DiscordMessageHandler(Gson gson, Client client, DrawManager drawManager, OkHttpClient httpClient, DinkPluginConfig config, ScheduledExecutorService executor, ClientThread clientThread, DiscordService discordService) {
        this.gson = gson;
        this.client = client;
        this.drawManager = drawManager;
        this.config = config;
        this.executor = executor;
        this.clientThread = clientThread;
        this.discordService = discordService;
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
            .addInterceptor(chain -> {
                Request request = chain.request();
                Response response = chain.proceed(request);
                String method = request.method();
                // http status code 307 can be useful for custom webhook handlers to redirect requests as seen in https://github.com/pajlads/DinkPlugin/issues/482
                // however, runelite uses okhttp 3.14.9, which does not follow RFC 7231 for code 307 (or RFC 7238 for code 308).
                // while this was fixed in okhttp 4.6.0 (released on 2020-04-28), we need this interceptor to patch this issue for now
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    int code = response.code();
                    if (code == 307 || code == 308) {
                        String redirectUrl = response.header("Location");
                        if (redirectUrl != null) {
                            Request updatedRequest = request.newBuilder().url(redirectUrl).build();
                            return chain.proceed(updatedRequest);
                        }
                    }
                }
                return response;
            })
            .build();
    }

    public void createMessage(String webhookUrl, boolean sendImage, @NonNull NotificationBody<?> inputBody) {
        if (StringUtils.isBlank(webhookUrl)) return;

        Collection<HttpUrl> urlList = Arrays.stream(StringUtils.split(webhookUrl, '\n'))
            .filter(StringUtils::isNotBlank)
            .map(HttpUrl::parse)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (urlList.isEmpty()) return;

        NotificationBody<?> mBody = enrichBody(inputBody, sendImage);
        if (sendImage) {
            // optionally hide chat for privacy in screenshot
            boolean alreadyCaptured = mBody.getScreenshotOverride() != null;
            boolean chatHidden = Utils.hideWidget(!alreadyCaptured && config.screenshotHideChat(), client, ComponentID.CHATBOX_FRAME);
            boolean whispersHidden = Utils.hideWidget(!alreadyCaptured && config.screenshotHideChat(), client, PRIVATE_CHAT_WIDGET);

            captureScreenshot(config.screenshotScale() / 100.0, chatHidden, whispersHidden, mBody.getScreenshotOverride())
                .thenApply(image ->
                    RequestBody.create(MediaType.parse("image/" + image.getKey()), image.getValue())
                )
                .exceptionally(e -> {
                    log.warn("There was an error creating bytes from captured image", e);
                    return null;
                })
                .thenAccept(image -> sendToMultiple(urlList, mBody, image));
        } else {
            sendToMultiple(urlList, mBody, null);
        }
    }

    private void sendToMultiple(Collection<HttpUrl> urls, NotificationBody<?> body, @Nullable RequestBody image) {
        urls.forEach(url -> sendMessage(url, injectThreadName(url, body, false), image, 0));
    }

    private void sendMessage(HttpUrl url, NotificationBody<?> mBody, @Nullable RequestBody image, int attempt) {
        BiConsumer<NotificationBody<?>, Throwable> retry = (body, e) -> {
            log.trace(String.format("Failed to send webhook message to %s on attempt %d", url, attempt), e);

            if (attempt == 0) {
                log.warn("There was an error sending the webhook message", e);
            }

            int maxRetries = config.maxRetries();
            if (attempt < maxRetries) {
                long baseDelay = config.baseRetryDelay();
                if (baseDelay > 0) {
                    long delay = baseDelay * (1L << Math.min(attempt, 16)); // exponential backoff
                    executor.schedule(() -> sendMessage(url, body, image, attempt + 1), delay, TimeUnit.MILLISECONDS);
                    log.debug("Scheduled webhook message for retry in {} milliseconds", delay);
                } else {
                    log.debug("Skipping retry attempts for failed webhook since base delay is not positive");
                }
            } else if (maxRetries > 0) {
                log.warn("Exhausted retry attempts when sending the webhook message", e);
            } else {
                log.debug("Skipping retry attempts for failed webhook since max retries is not positive");
            }
        };

        executor.execute(() -> {
            Request request = new Request.Builder()
                .url(url)
                .post(createBody(mBody, image))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.trace("Successfully sent webhook message to {} after {} attempts", url, attempt + 1);
                } else {
                    String body = response.body() != null ? response.body().string() : null;

                    // Update thread_name to comply with discord forum channel specification
                    if (response.code() == 400 && "application/json".equals(response.header("Content-Type"))) {
                        DiscordErrorMessage error = gson.fromJson(body, DiscordErrorMessage.class);

                        // "Webhooks posted to forum channels must have a thread_name or thread_id"
                        if (error.getCode() == 220001) {
                            retry.accept(
                                injectThreadName(url, mBody, true),
                                new RuntimeException(error.getMessage())
                            );
                            return;
                        }

                        // "Webhooks can only create threads in forum channels"
                        if (error.getCode() == 220003) {
                            retry.accept(mBody.withThreadName(null), new RuntimeException(error.getMessage()));
                            return;
                        }
                    }

                    // retry with no change to NotificationBody
                    retry.accept(mBody, new RuntimeException(String.format("Received unsuccessful http response: %d - %s - %s", response.code(), response.message(), body)));
                }
            } catch (Exception e) {
                retry.accept(mBody, e);
            }
        });
    }

    private NotificationBody<?> enrichBody(NotificationBody<?> mBody, boolean sendImage) {
        if (mBody.getPlayerName() == null) {
            mBody = mBody.withPlayerName(Utils.getPlayerName(client));
        }

        if (mBody.getAccountType() == null) {
            mBody = mBody.withAccountType(Utils.getAccountType(client));
        }

        if (mBody.getDinkAccountHash() == null) {
            long id = client.getAccountHash();
            if (id != -1) {
                mBody = mBody.withDinkAccountHash(Utils.dinkHash(id));
            }
        }

        if (!config.ignoreSeasonal() && !mBody.isSeasonalWorld() && client.getWorldType().contains(WorldType.SEASONAL)) {
            mBody = mBody.withSeasonalWorld(true);
        }

        NotificationBody.NotificationBodyBuilder<?> builder = mBody.toBuilder();

        if (config.sendDiscordUser()) {
            builder.discordUser(DiscordProfile.of(discordService.getCurrentUser()));
        }

        if (config.sendClanName()) {
            ClanChannel clan = client.getClanChannel(ClanID.CLAN);
            if (clan != null) {
                builder.clanName(clan.getName());
            }
        }

        if (config.sendGroupIronClanName()) {
            ClanChannel gim = client.getClanChannel(ClanID.GROUP_IRONMAN);
            if (gim != null) {
                builder.groupIronClanName(gim.getName());
            }
        }

        if (config.discordRichEmbeds()) {
            builder.embeds(computeEmbeds(mBody, sendImage, config));
        } else {
            builder.computedDiscordContent(mBody.getText().evaluate(false));
        }

        return builder.build();
    }

    private NotificationBody<?> injectThreadName(HttpUrl url, NotificationBody<?> mBody, boolean force) {
        Collection<String> queryParams = url.queryParameterNames();
        if (force || (queryParams.contains("forum") && !queryParams.contains("thread_id"))) {
            String type = mBody.isSeasonalWorld() ? "Seasonal - " + mBody.getType().getTitle() : mBody.getType().getTitle();
            String threadName = Template.builder()
                .template(config.threadNameTemplate())
                .replacementBoundary("%")
                .replacement("%TYPE%", Replacements.ofText(type))
                .replacement("%MESSAGE%", mBody.getText())
                .replacement("%USERNAME%", Replacements.ofText(mBody.getPlayerName()))
                .build()
                .evaluate(false);
            return mBody.withThreadName(Utils.truncate(StringUtils.normalizeSpace(threadName), NotificationBody.MAX_THREAD_NAME_LENGTH));
        }
        return mBody;
    }

    private MultipartBody createBody(NotificationBody<?> mBody, @Nullable RequestBody image) {
        MultipartBody.Builder requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", gson.toJson(mBody));
        if (image != null) {
            String screenshotFileName = mBody.getType().getScreenshot();
            requestBody.addFormDataPart("file", screenshotFileName, image);
        }
        return requestBody.build();
    }

    /**
     * Captures the next frame and applies the specified rescaling
     * while abiding by {@link Embed#MAX_IMAGE_SIZE}.
     *
     * @param scalePercent {@link DinkPluginConfig#screenshotScale()} divided by 100.0
     * @param chatHidden Whether the chat widget should be unhidden
     * @param whispersHidden Whether the whispers widget should be unhidden
     * @param screenshotOverride an optional image to use instead of grabbing a frame from {@link DrawManager}
     * @return future of the image byte array by the image format name
     * @apiNote scalePercent should be in (0, 1]
     * @implNote the image format is either "png" (lossless) or "jpeg" (lossy), both of which can be used in MIME type
     */
    private CompletableFuture<Map.Entry<String, byte[]>> captureScreenshot(double scalePercent, boolean chatHidden, boolean whispersHidden, @Nullable Image screenshotOverride) {
        CompletableFuture<Image> future = new CompletableFuture<>();
        if (screenshotOverride != null) {
            executor.execute(() -> future.complete(screenshotOverride));
        } else {
            drawManager.requestNextFrameListener(img -> {
                // unhide any widgets we hid (scheduled for client thread)
                Utils.unhideWidget(chatHidden, client, clientThread, ComponentID.CHATBOX_FRAME);
                Utils.unhideWidget(whispersHidden, client, clientThread, PRIVATE_CHAT_WIDGET);

                // resolve future on separate thread
                executor.execute(() -> future.complete(img));
            });
        }
        return future.thenApply(ImageUtil::bufferedImageFromImage)
            .thenApply(input -> Utils.rescale(input, scalePercent))
            .thenApply(image -> {
                try {
                    String format = "png"; // lossless
                    return Pair.of(format, Utils.convertImageToByteArray(image, format));
                } catch (IOException e) {
                    throw new CompletionException("Could not convert image to byte array", e);
                }
            })
            .thenApply(pair -> {
                byte[] bytes = pair.getValue();
                int n = bytes.length;
                if (n <= Embed.MAX_IMAGE_SIZE)
                    return pair; // already compliant; no further rescale necessary

                // calculate scale factor to comply with MAX_IMAGE_SIZE
                double factor = Math.sqrt(1.0 * Embed.MAX_IMAGE_SIZE / n);

                // bytes => original image => rescaled image => updated bytes
                try (InputStream is = new ByteArrayInputStream(bytes)) {
                    String format = "jpeg"; // lossy
                    BufferedImage rescaled = Utils.rescale(ImageIO.read(is), factor);
                    return Pair.of(format, Utils.convertImageToByteArray(rescaled, format));
                } catch (Exception e) {
                    throw new CompletionException("Failed to resize image below Discord size limit", e);
                }
            });
    }

    private static List<Embed> computeEmbeds(@NotNull NotificationBody<?> body, boolean screenshot, DinkPluginConfig config) {
        NotificationType type = body.getType();
        NotificationData extra = body.getExtra();
        String footerText = config.embedFooterText();
        String footerIcon = config.embedFooterIcon();
        PlayerLookupService playerLookupService = config.playerLookupService();

        Author author = Author.builder()
            .name(body.getPlayerName())
            .url(playerLookupService.getPlayerUrl(body.getPlayerName()))
            .iconUrl(Utils.getChatBadge(body.getAccountType(), body.isSeasonalWorld()))
            .build();
        Footer footer = StringUtils.isBlank(footerText) ? null : Footer.builder()
            .text(Utils.truncate(footerText, Embed.MAX_FOOTER_LENGTH))
            .iconUrl(StringUtils.isBlank(footerIcon) ? null : footerIcon)
            .build();
        String thumbnail = body.getThumbnailUrl() != null
            ? body.getThumbnailUrl()
            : type.getThumbnail();

        List<Embed> embeds = new ArrayList<>(body.getEmbeds() != null ? body.getEmbeds() : Collections.emptyList());
        embeds.add(0,
            Embed.builder()
                .author(author)
                .color(Utils.PINK)
                .title(body.isSeasonalWorld() ? "[Seasonal] " + type.getTitle() : type.getTitle())
                .description(Utils.truncate(body.getText().evaluate(config.discordRichEmbeds()), Embed.MAX_DESCRIPTION_LENGTH))
                .image(screenshot ? new Embed.UrlEmbed("attachment://" + type.getScreenshot()) : null)
                .thumbnail(new Embed.UrlEmbed(thumbnail))
                .fields(extra != null ? extra.getFields() : Collections.emptyList())
                .footer(footer)
                .timestamp(footer != null ? Instant.now() : null)
                .build()
        );
        return embeds;
    }

}
