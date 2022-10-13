package dinkplugin;

import static net.runelite.http.api.RuneLiteAPI.GSON;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
public class DiscordMessageHandler {
    private DinkPlugin plugin;

    @Inject
    public DiscordMessageHandler(DinkPlugin plugin) {
        this.plugin = plugin;
    }

    public void createMessage(String message, boolean sendImage, DiscordMessageBody mBody) {
        DiscordMessageBody messageBody = new DiscordMessageBody();
        if(mBody != null) {
            messageBody = mBody;
        }

        messageBody.setContent(message);
        String webhookUrl = plugin.config.discordWebhook();
        if(Strings.isNullOrEmpty(webhookUrl)) {
            return;
        }
        ArrayList<HttpUrl> urlList = new ArrayList<>();
        String[] strList = webhookUrl.split("\n");
        for (String urlString: strList) {
            if(Objects.equals(urlString, "")) {
                continue;
            }
            urlList.add(HttpUrl.parse(urlString));
        }

        MultipartBody.Builder reqBodyBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", GSON.toJson(messageBody));

        if (sendImage) {
            plugin.drawManager.requestNextFrameListener(image -> {
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

                reqBodyBuilder.addFormDataPart("file", "collectionImage.png",
                        RequestBody.create(MediaType.parse("image/png"), imageBytes));
                sendToMultiple(urlList, reqBodyBuilder);
            });
            return;
        }

        sendToMultiple(urlList, reqBodyBuilder);
    }

    private void sendToMultiple(ArrayList<HttpUrl> urls, MultipartBody.Builder requestBody) {
        for (HttpUrl url: urls) {
            sendMessage(url, requestBody);
        }
    }

    private void sendMessage(HttpUrl url, MultipartBody.Builder requestBody) {
        RequestBody body = requestBody.build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        plugin.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("There was an error sending the webhook message", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }
}
