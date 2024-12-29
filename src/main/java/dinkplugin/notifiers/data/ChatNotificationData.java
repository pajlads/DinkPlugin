package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * or {@link ChatMessageType#CLAN_GUEST_CHAT} or {@link ChatMessageType#CLAN_GIM_CHAT}.
     */
    @Nullable
    ClanTitle clanTitle;

    @NotNull
    String message;

}
