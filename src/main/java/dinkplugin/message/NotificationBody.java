package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBody<T extends Fieldable> {

    /*
     * Dink fields
     */
    @NotNull
    private NotificationType type;
    private String playerName;
    @Nullable
    private T extra;

    private transient String content;

    /*
     * Discord fields
     */
    private String username;
    private boolean tts;
    private @SerializedName("avatar_url") String avatarUrl;
    private @Singular List<Embed> embeds;

}
