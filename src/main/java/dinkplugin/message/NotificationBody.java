package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import dinkplugin.notifiers.data.NotificationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import net.runelite.api.vars.AccountType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
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

    /**
     * Filled in with the text of the notifier (e.g., {@link #getText()} is "Forsen has levelled Attack to 100")
     * <p>
     * This is done by {@link DiscordMessageHandler#createMessage} if {@link dinkplugin.DinkPluginConfig#discordRichEmbeds()} is disabled.
     */
    @Nullable
    @SerializedName("content")
    String computedDiscordContent;

    @Builder.Default
    List<Embed> embeds = new LinkedList<>();

}
