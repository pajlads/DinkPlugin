package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.LevelNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;

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
        int interval = config.levelInterval();
        return interval <= 1
            || level == 99
            || level % interval == 0;
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

    public void onStatChanged(StatChanged statChange) {
        this.handleLevelUp(statChange.getSkill().getName(), statChange.getLevel(), statChange.getXp());
    }

    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            this.reset();
        }
    }

    private void attemptNotify() {
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
        String fullNotification = config.levelNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%SKILL%", skillString);
        NotificationBody<LevelNotificationData> body = new NotificationBody<>();
        body.setContent(fullNotification);

        LevelNotificationData extra = new LevelNotificationData();
        extra.setAllSkills(currentLevels);
        extra.setLevelledSkills(lSkills);

        body.setExtra(extra);
        body.setType(NotificationType.LEVEL);
        messageHandler.createMessage(config.levelSendImage(), body);
    }

    public void handleLevelUp(String skill, int level, int xp) {
        if (plugin.isIgnoredWorld()) return;

        int virtualLevel = level < 99 ? level : Experience.getLevelForXp(xp); // avoid log(n) query when not needed
        Integer previousLevel = currentLevels.put(skill, virtualLevel);
        if (config.notifyLevel() && checkLevelInterval(virtualLevel) && previousLevel != null) {
            if (virtualLevel > previousLevel) {
                levelledSkills.add(skill);
                sendMessage = true;
            }
        }
    }
}
