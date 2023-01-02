package dinkplugin.notifiers.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class QuestNotificationData extends NotificationData {
    final String questName;
}
