package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.ChatMessageType;
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
     */
    @Nullable
    String source;

    @NotNull
    String message;

}
