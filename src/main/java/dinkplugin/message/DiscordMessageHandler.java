package dinkplugin.message;

import com.google.common.base.Strings;
import dinkplugin.DinkPlugin;
import dinkplugin.Utils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
public class DiscordMessageHandler {
    private final DinkPlugin plugin;

    @Inject
    public DiscordMessageHandler(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public <T> void createMessage(boolean sendImage, @NonNull NotificationBody<T> mBody) {
        if (mBody.getPlayerName() == null)
            mBody.setPlayerName(Utils.getPlayerName(plugin.getClient()));

        String webhookUrl = plugin.getConfig().discordWebhook();
        if (Strings.isNullOrEmpty(webhookUrl)) {
            return;
        }
        List<HttpUrl> urlList = new ArrayList<>();
        String[] strList = webhookUrl.split("\n");
        for (String urlString : strList) {
            if (urlString.isEmpty()) {
                continue;
            }
            urlList.add(HttpUrl.parse(urlString));
        }

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", GSON.toJson(mBody));

        if (sendImage) {
            plugin.getDrawManager().requestNextFrameListener(image -> {
                BufferedImage bufferedImage = (BufferedImage) image;
                byte[] imageBytes;
                try {
                    imageBytes = Utils.convertImageToByteArray(bufferedImage);
                } catch (IOException e) {
                    log.warn("There was an error creating bytes from captured image", e);
                    // Still send the message even if the image cannot be created
                    sendToMultiple(urlList, reqBodyBuilder);
                    return;
                }


                reqBodyBuilder.addFormDataPart(
                    "file",
                    "collectionImage.png",
                    RequestBody.create(
                        MediaType.parse("image/png"),
                        imageBytes
                    )
                );
                sendToMultiple(urlList, reqBodyBuilder);
            });
            return;
        }

        sendToMultiple(urlList, reqBodyBuilder);
    }

    private void sendToMultiple(List<HttpUrl> urls, MultipartBody.Builder requestBody) {
        for (HttpUrl url : urls) {
            sendMessage(url, requestBody);
        }
    }

    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody) {
        RequestBody body = requestBody.build();
        Request request = new Request.Builder()
            .url(url)
            .post(body)
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
