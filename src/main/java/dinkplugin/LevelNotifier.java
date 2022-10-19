package dinkplugin;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Slf4j
public class LevelNotifier extends BaseNotifier {

    private static final NavigableMap<Integer, Integer> LEVEL_BY_EXP_THRESHOLD;
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
        if (plugin.isSpeedrunWorld()) return;

        int virtualLevel = level < 99 ? level : getLevelForExperience(xp);
        Integer previousLevel = currentLevels.put(skill, virtualLevel);
        if (plugin.config.notifyLevel() && checkLevelInterval(virtualLevel) && previousLevel != null) {
            if (virtualLevel > previousLevel) {
                levelledSkills.add(skill);
                sendMessage = true;
            }
        }
    }

    private static Integer getLevelForExperience(int xp) {
        return xp > 0 ? LEVEL_BY_EXP_THRESHOLD.floorEntry(xp).getValue() : 1;
    }

    static {
        final int minLevel = 1, maxLevel = 126;

        LEVEL_BY_EXP_THRESHOLD = new TreeMap<>();
        LEVEL_BY_EXP_THRESHOLD.put(0, minLevel); // 0 xp = level 1

        int prev = 0;
        for (int level = minLevel + 1; level <= maxLevel; level++) {
            // see https://oldschool.runescape.wiki/w/Experience#Formula
            int x = prev + (int) Math.floor(level - 1 + 300 * Math.pow(2.0, (level - 1) / 7.0));
            int xp = (int) Math.floor(x / 4.0);
            LEVEL_BY_EXP_THRESHOLD.put(xp, level);
            prev = x;
        }
    }
}
