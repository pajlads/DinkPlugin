package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.ChatMessageType;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChatNotificationData extends NotificationData {
    ChatMessageType type;
    String message;
}
