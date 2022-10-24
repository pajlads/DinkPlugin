package dinkplugin.notifiers.data;

import lombok.Value;

import java.util.Map;

@Value
public class LevelNotificationData {
    Map<String, Integer> levelledSkills;
    Map<String, Integer> allSkills;
}
