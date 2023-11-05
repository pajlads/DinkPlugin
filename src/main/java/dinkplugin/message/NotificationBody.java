package dinkplugin.message;

import com.google.gson.annotations.SerializedName;
import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.AccountType;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.NotificationData;
import dinkplugin.util.DiscordProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

@Value
@With
@Builder(toBuilder = true)
@AllArgsConstructor
public class NotificationBody<T extends NotificationData> {

    static final int MAX_THREAD_NAME_LENGTH = 100; // not explicitly documented by Discord

    /*
     * Dink fields
     */
    @NotNull
    NotificationType type;
    String playerName;
    AccountType accountType;
    String dinkAccountHash;
    @Nullable
    String clanName;
    @Nullable
    String groupIronClanName;
    boolean seasonalWorld;
    @Nullable
    T extra;
    @NotNull
    @EqualsAndHashCode.Include
    transient Template text;

    /*
     * Discord fields
     */

    /**
     * Information about the current discord user, acquired via RPC (handled by base RuneLite).
     * <p>
     * This is only sent if {@link DinkPluginConfig#sendDiscordUser()} is enabled.
     * While this field is not used by Discord, it can be useful for custom webhook handlers that forward to Discord.
     */
    @Nullable
    DiscordProfile discordUser;

    /**
     * Filled in with the text of the notifier (e.g., {@link #getText()} is "Forsen has levelled Attack to 100")
     * <p>
     * This is done by {@link DiscordMessageHandler#createMessage} if {@link dinkplugin.DinkPluginConfig#discordRichEmbeds()} is disabled.
     */
    @Nullable
    @SerializedName("content")
    String computedDiscordContent;

    /**
     * An optional thumbnail to override that of {@link NotificationType#getThumbnail}
     * within the embed constructed by {@link DiscordMessageHandler#createMessage}
     */
    @Nullable
    @EqualsAndHashCode.Exclude
    transient String thumbnailUrl;

    @Builder.Default
    List<Embed> embeds = new LinkedList<>();

    /**
     * The thread name; must only be specified for forum channels when thread_id is not present
     *
     * @see #MAX_THREAD_NAME_LENGTH
     */
    @Nullable
    @SerializedName("thread_name")
    String threadName;

}
