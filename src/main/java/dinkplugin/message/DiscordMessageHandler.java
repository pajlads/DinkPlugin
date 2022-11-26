package dinkplugin.message;

import dinkplugin.Utils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class DiscordMessageHandler {
    private final Client client;
    private final DrawManager drawManager;
    private final OkHttpClient httpClient;

    public <T> void createMessage(String webhookUrl, boolean sendImage, @NonNull NotificationBody<T> mBody) {
        if (StringUtils.isBlank(webhookUrl)) return;

        Collection<HttpUrl> urlList = Arrays.stream(StringUtils.split(webhookUrl, '\n'))
            .filter(StringUtils::isNotBlank)
            .map(HttpUrl::parse)
            .collect(Collectors.toList());
        if (urlList.isEmpty()) return;

        if (mBody.getPlayerName() == null)
            mBody.setPlayerName(Utils.getPlayerName(client));

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", GSON.toJson(mBody));

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
        urls.forEach(url -> sendMessage(url, reqBodyBuilder));
    }

    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody) {
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build();
        httpClient.newCall(request).enqueue(new Callback() {
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
