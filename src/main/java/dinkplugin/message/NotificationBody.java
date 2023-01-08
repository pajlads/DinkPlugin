package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import dinkplugin.notifiers.data.NotificationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import net.runelite.api.vars.AccountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value
@With
@Builder
@AllArgsConstructor
public class NotificationBody<T extends NotificationData> {

    /*
     * Dink fields
     */
    @NotNull
    NotificationType type;
    String playerName;
    AccountType accountType;
    @Nullable
    T extra;
    @NotNull
    transient String text;

    /*
     * Discord fields
     */
    @Nullable
    @SerializedName("content")
    String computedDiscordContent; // this should be set by DiscordMessageHandler (when richDiscordEmbeds is disabled), not notifiers
    @Singular
    List<Embed> embeds;

}
