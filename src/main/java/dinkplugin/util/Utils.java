package dinkplugin.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dinkplugin.domain.AccountType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.client.util.ColorUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@UtilityClass
public class Utils {

    public static final String WIKI_IMG_BASE_URL = "https://oldschool.runescape.wiki/images/";

    public final Color GREEN = ColorUtil.fromHex("006c4c"); // dark shade of PINK in CIELCh_uv color space
    public final Color PINK = ColorUtil.fromHex("#f40098"); // analogous to RED in CIELCh_uv color space
    public final Color RED = ColorUtil.fromHex("#ca2a2d"); // red used in pajaW

    /**
     * Converts text into "upper case first" form, as is used by OSRS for item names.
     *
     * @param text the string to be transformed
     * @return the text with only the first character capitalized
     */
    public String ucFirst(@NotNull String text) {
        if (text.length() < 2) return text.toUpperCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    /**
     * @param a some string
     * @param b another string
     * @return whether either string contains the other
     */
    public boolean containsEither(@NotNull String a, @NotNull String b) {
        if (a.length() >= b.length()) {
            return a.contains(b);
        } else {
            return b.contains(a);
        }
    }

    /**
     * @param client {@link Client}
     * @return the name of the local player
     */
    public String getPlayerName(Client client) {
        return client.getLocalPlayer().getName();
    }

    /**
     * Transforms the value from {@link Varbits#ACCOUNT_TYPE} to a convenient enum.
     *
     * @param client {@link Client}
     * @return {@link AccountType}
     * @apiNote This function should only be called from the client thread.
     */
    public AccountType getAccountType(@NotNull Client client) {
        return AccountType.get(client.getVarbitValue(Varbits.ACCOUNT_TYPE));
    }

    @Nullable
    public String getChatBadge(@NotNull AccountType type) {
        switch (type) {
            case IRONMAN:
                return WIKI_IMG_BASE_URL + "Ironman_chat_badge.png";
            case ULTIMATE_IRONMAN:
                return WIKI_IMG_BASE_URL + "Ultimate_ironman_chat_badge.png";
            case HARDCORE_IRONMAN:
                return WIKI_IMG_BASE_URL + "Hardcore_ironman_chat_badge.png";
            case GROUP_IRONMAN:
                return WIKI_IMG_BASE_URL + "Group_ironman_chat_badge.png";
            case HARDCORE_GROUP_IRONMAN:
                return WIKI_IMG_BASE_URL + "Hardcore_group_ironman_chat_badge.png";
            case UNRANKED_GROUP_IRONMAN:
                return WIKI_IMG_BASE_URL + "Unranked_group_ironman_chat_badge";
            default:
                return null;
        }
    }

    public BufferedImage rescale(BufferedImage input, double percent) {
        if (percent + Math.ulp(1.0) >= 1.0)
            return input;

        AffineTransform rescale = AffineTransform.getScaleInstance(percent, percent);
        AffineTransformOp operation = new AffineTransformOp(rescale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

        BufferedImage output = new BufferedImage((int) (input.getWidth() * percent), (int) (input.getHeight() * percent), input.getType());
        operation.filter(input, output);
        return output;
    }

    public byte[] convertImageToByteArray(BufferedImage bufferedImage, String format) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean foundWriter = ImageIO.write(bufferedImage, format, byteArrayOutputStream);
        if (!foundWriter)
            throw new IllegalArgumentException(String.format("Specified format '%s' was not in supported formats: %s", format, Arrays.toString(ImageIO.getWriterFormatNames())));
        return byteArrayOutputStream.toByteArray();
    }

    public boolean hasImage(@NotNull MultipartBody body) {
        return body.parts().stream().anyMatch(part -> {
            MediaType type = part.body().contentType();
            return type != null && "image".equals(type.type());
        });
    }

    public CompletableFuture<String> readClipboard() {
        CompletableFuture<String> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                future.complete(data);
            } catch (Exception e) {
                log.warn("Failed to read from clipboard", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> copyToClipboard(String text) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection content = new StringSelection(text);
                clipboard.setContents(content, content);
                future.complete(null);
            } catch (Exception e) {
                log.warn("Failed to copy to clipboard", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public <T> CompletableFuture<T> readJson(@NotNull OkHttpClient httpClient, @NotNull Gson gson, @NotNull String url, @NotNull TypeToken<T> type) {
        return readUrl(httpClient, url, reader -> gson.fromJson(reader, type.getType()));
    }

    public <T> CompletableFuture<T> readUrl(@NotNull OkHttpClient httpClient, @NotNull String url, @NotNull Function<Reader, T> transformer) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                assert response.body() != null;
                try (Reader reader = response.body().charStream()) {
                    future.complete(transformer.apply(reader));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    response.close();
                }
            }
        });
        return future;
    }

}
