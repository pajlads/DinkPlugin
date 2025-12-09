package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChatNotificationData extends NotificationData {

    @NotNull
    ChatMessageType type;

    /**
     * {@link ChatMessage#getName()} when available; otherwise {@link  ChatMessage#getSender()}.
     * When {@link #getType()} is {@link ChatMessageType#UNKNOWN}, this field corresponds to the originating runelite event.
     */
    @Nullable
    String source;

    /**
     * Clan title of the player that sent the message.
     * Only populated when {@link #getType()} is {@link ChatMessageType#CLAN_CHAT}
     * or {@link ChatMessageType#CLAN_GUEST_CHAT} or {@link ChatMessageType#CLAN_GIM_CHAT}
     * or sometimes {@link ChatMessageType#CLAN_MESSAGE} (for user joins).
     */
    @Nullable
    ClanTitle clanTitle;

    @NotNull
    String message;

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("type", type);
        m.put("message", message);
        if (source != null) m.put("source", source);
        if (clanTitle != null) m.put("clanTitle", clanTitle);
        return m;
    }
}
