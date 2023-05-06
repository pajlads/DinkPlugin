package dinkplugin.util;

import lombok.Value;
import net.runelite.discord.DiscordUser;
import org.jetbrains.annotations.Nullable;

@Value
public class DiscordProfile {
    String id;
    String name;
    String avatarHash;

    public static DiscordProfile of(@Nullable DiscordUser user) {
        if (user == null || user.userId == null) return null;
        return new DiscordProfile(user.userId, user.username, user.avatar);
    }
}
