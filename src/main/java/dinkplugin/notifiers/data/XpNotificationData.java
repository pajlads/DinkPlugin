package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.Skill;

import java.util.Collection;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class XpNotificationData extends NotificationData {
    Map<Skill, Integer> xpData;
    Collection<Skill> milestoneAchieved;
    int interval;
}
