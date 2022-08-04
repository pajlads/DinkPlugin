package universalDiscord;

import static net.runelite.http.api.RuneLiteAPI.GSON;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DrawManager;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Slf4j
public class DiscordMessageHandler {
    private UniversalDiscordPlugin plugin;

    @Inject
    public DiscordMessageHandler(UniversalDiscordPlugin plugin) {
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

        HttpUrl url = HttpUrl.parse(webhookUrl);
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
                    sendMessage(url, reqBodyBuilder);
                    return;
                }

                reqBodyBuilder.addFormDataPart("file", "collectionImage.png",
                        RequestBody.create(MediaType.parse("image/png"), imageBytes));
                sendMessage(url, reqBodyBuilder);
            });
            return;
        }

        sendMessage(url, reqBodyBuilder);
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
