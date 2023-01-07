package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sends along a small customizable snippet & icon at the bottom of the embed
 *
 * @see <a href="https://birdie0.github.io/discord-webhooks-guide/structure/embed/footer.html">Example</a>
 */
@Value
@Builder
public class Footer {
    @NotNull
    String text;

    @Nullable
    @SerializedName("icon_url")
    String iconUrl;
}
