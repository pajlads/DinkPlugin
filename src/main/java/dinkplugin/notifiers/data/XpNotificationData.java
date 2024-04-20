package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class XpNotificationData extends NotificationData {
    Map<String, Integer> xpData;
    Collection<String> milestoneAchieved;
    int interval;
}
