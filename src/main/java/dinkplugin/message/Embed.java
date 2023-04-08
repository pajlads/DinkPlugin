package dinkplugin.message;

import com.google.gson.annotations.JsonAdapter;
import dinkplugin.util.ColorAdapter;
import dinkplugin.util.InstantAdapter;
import lombok.Builder;
import lombok.Value;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

/**
 * A rich embed object used for notifications.
 * <p>
 * Notifications build one big embed with the fields of this class from {@link NotificationBody}
 */
@Value
@Builder
public class Embed {
    // The max size of the image before we rescale it to fit Discords file upload limits https://discord.com/developers/docs/reference#uploading-files
    public static final int MAX_IMAGE_SIZE = 8_000_000; // 8MB
    public static final int MAX_DESCRIPTION_LENGTH = 4096;
    public static final int MAX_FOOTER_LENGTH = 2048;

    /**
     * Filled in with the title of {@link NotificationBody}.
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/title.html">Example</a>
     */
    String title;

    /**
     * Includes the text of the notifier (e.g., {@link NotificationBody#getText()} is "Forsen has levelled Attack to 100")
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/description.html">Example</a>
     */
    String description;

    /**
     * Author of the embed object.
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/author.html">Example</a>
     */
    Author author;

    /**
     * Color trim of the rich embed
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/color.html">Example</a>
     */
    @JsonAdapter(ColorAdapter.class)
    Color color;

    /**
     * Embedded image.
     * <p>
     * Typically, this contains the screenshot of the notification, but can also be an item image.
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/image.html">Example</a>
     */
    UrlEmbed image;

    /**
     * Filled in with the notification's icon: {@link NotificationType#getThumbnail()}
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/thumbnail.html">Example</a>
     */
    UrlEmbed thumbnail;

    /**
     * Contains extra notifier-specific fields.
     * <p>
     * For example, the loot notifier contains a field with the total value of the loot
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/fields.html">Example</a>
     */
    List<Field> fields;

    /**
     * Embed footer.
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/footer.html">Example</a>
     */
    Footer footer;

    /**
     * Timestamp at the bottom of the embed object, serialized to long.
     *
     * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/timestamp.html">Example</a>
     */
    @JsonAdapter(InstantAdapter.class)
    Instant timestamp;

    /**
     * Helper function to construct a simple embed that only contains an image
     *
     * @param url the url of the image
     * @return embed object
     */
    public static Embed ofImage(String url) {
        return Embed.builder()
            .image(new UrlEmbed(url))
            .build();
    }

    @Value
    public static class UrlEmbed {
        String url;
    }
}
