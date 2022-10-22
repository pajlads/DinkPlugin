package dinkplugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LevelNotifier extends BaseNotifier {

    private final List<String> levelledSkills = new ArrayList<>();
    private final Map<String, Integer> currentLevels = new HashMap<>();
    private boolean sendMessage = false;
    private int ticksWaited = 0;

    @Inject
    public LevelNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void reset() {
        currentLevels.clear();
        levelledSkills.clear();
    }

    private boolean checkLevelInterval(int level) {
        return plugin.config.levelInterval() <= 1
            || level == 99
            || level % plugin.config.levelInterval() == 0;
    }

    public void onTick() {
        if (!sendMessage) {
            return;
        }

        ticksWaited++;
        // We wait a couple extra ticks so we can ensure that we process all the levels of the previous tick
        if (ticksWaited > 2) {
            ticksWaited = 0;
            attemptNotify();
        }
    }

    public void attemptNotify() {
        sendMessage = false;
        StringBuilder skillMessage = new StringBuilder();
        int index = 0;
        Map<String, Integer> lSkills = new HashMap<>();

        for (String skill : levelledSkills) {
            if (index == levelledSkills.size()) {
                skillMessage.append(" and ");
            } else if (index > 0) {
                skillMessage.append(", ");
            }
            skillMessage.append(String.format("%s to %s", skill, currentLevels.get(skill)));
            lSkills.put(skill, currentLevels.get(skill));
            index++;
        }

        String skillString = skillMessage.toString();
        levelledSkills.clear();
        String fullNotification = plugin.config.levelNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%SKILL%", skillString);
        NotificationBody<LevelNotificationData> body = new NotificationBody<>();
        body.setContent(fullNotification);

        LevelNotificationData extra = new LevelNotificationData();
        extra.setAllSkills(currentLevels);
        extra.setLevelledSkills(lSkills);

        body.setExtra(extra);
        body.setType(NotificationType.LEVEL);
        plugin.messageHandler.createMessage(plugin.config.levelSendImage(), body);
    }

    public void handleLevelUp(String skill, int level, int xp) {
        if (plugin.isIgnoredWorld()) return;

        int virtualLevel = level < 99 ? level : Experience.getLevelForXp(xp); // avoid log(n) query when not needed
        Integer previousLevel = currentLevels.put(skill, virtualLevel);
        if (plugin.config.notifyLevel() && checkLevelInterval(virtualLevel) && previousLevel != null) {
            if (virtualLevel > previousLevel) {
                levelledSkills.add(skill);
                sendMessage = true;
            }
        }
    }
}
