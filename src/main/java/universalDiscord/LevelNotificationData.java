package universalDiscord;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class LevelNotificationData {
    private Map<String, Integer> levelledSkills = new HashMap<>();
    private Map<String, Integer> allSkills = new HashMap<>();
}
