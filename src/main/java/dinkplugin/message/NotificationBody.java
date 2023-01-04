package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.With;
import net.runelite.api.vars.AccountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@With
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
    private AccountType accountType;
    @Nullable
    private T extra;
    @NotNull
    private transient String text;

    /*
     * Discord fields
     */
    @Nullable
    @SerializedName("content")
    private String computedDiscordContent; // this should be set by DiscordMessageHandler, not notifiers
    private @Singular List<Embed> embeds;

}
