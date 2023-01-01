package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBody<T> {

    /*
     * Dink fields
     */
    @Builder.Default
    @EqualsAndHashCode.Exclude
    private transient String screenshotFile = "image.png";
    private NotificationType type;
    private String playerName;
    private T extra;

    @SerializedName("text") // used by slack; avoids duplication in discord webhook
    private String content;

    /*
     * Discord fields
     */
    private String username;
    private boolean tts;
    private @SerializedName("avatar_url") String avatarUrl;
    private @Singular List<Embed> embeds;

}
