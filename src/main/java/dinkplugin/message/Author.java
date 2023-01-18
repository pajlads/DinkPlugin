package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The top block of our rich Discord embed, describing the RS user
 *
 * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/author.html">Example</a>
 */
@Value
@Builder
public class Author {
    /**
     * Name of the user the webhook is sent for
     */
    @NotNull
    String name;

    /**
     * Icon of the user, auto filled with your Runelite chat badge (e.g. Ironman badge or Hardcore Ironman badge)
     */
    @Nullable
    @SerializedName("icon_url")
    String iconUrl;

    /**
     * Makes name a href and opens a browser with the url when clicked
     */
    @Nullable
    String url;
}
