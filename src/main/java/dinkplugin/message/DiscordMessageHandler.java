package dinkplugin.message;

import dinkplugin.DinkPlugin;
import dinkplugin.Utils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public class DiscordMessageHandler {
    private final DinkPlugin plugin;

    @Inject
    public DiscordMessageHandler(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public <T> void createMessage(boolean sendImage, @NonNull NotificationBody<T> mBody) {
        String webhookUrl = plugin.getConfig().discordWebhook();
        if (StringUtils.isBlank(webhookUrl)) return;

        Collection<HttpUrl> urlList = Arrays.stream(StringUtils.split(webhookUrl, '\n'))
            .filter(StringUtils::isNotBlank)
            .map(HttpUrl::parse)
            .collect(Collectors.toList());
        if (urlList.isEmpty()) return;

        if (mBody.getPlayerName() == null)
            mBody.setPlayerName(Utils.getPlayerName(plugin.getClient()));

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", GSON.toJson(mBody));

        if (sendImage) {
            plugin.getDrawManager().requestNextFrameListener(image -> {
                try {
                    byte[] imageBytes = Utils.convertImageToByteArray(ImageUtil.bufferedImageFromImage(image));

                    reqBodyBuilder.addFormDataPart(
                        "file",
                        "collectionImage.png",
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
        urls.forEach(url -> sendMessage(url, reqBodyBuilder));
    }

    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody) {
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build();
        plugin.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.warn("There was an error sending the webhook message", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                response.close();
            }
        });
    }
}
