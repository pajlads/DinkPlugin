package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.LevelNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class LevelNotifier extends BaseNotifier {

    private final List<String> levelledSkills = new ArrayList<>();
    private final Map<String, Integer> currentLevels = new HashMap<>();
    private boolean sendMessage = false;
    private int ticksWaited = 0;

    @Override
    protected String getWebhookUrl() {
        return config.levelWebhook();
    }

    public void initLevels() {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                currentLevels.put(skill.getName(), Experience.getLevelForXp(client.getSkillExperience(skill)));
            }
        }
    }

    public void reset() {
        currentLevels.clear();
        levelledSkills.clear();
    }

    public void onTick() {
        if (currentLevels.isEmpty()) {
            initLevels();
        }

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

    private void handleLevelUp(String skill, int level, int xp) {
        if (!isEnabled()) return;

        int virtualLevel = level < 99 ? level : Experience.getLevelForXp(xp); // avoid log(n) query when not needed
        Integer previousLevel = currentLevels.put(skill, virtualLevel);
        if (config.notifyLevel() && checkLevelInterval(virtualLevel) && previousLevel != null) {
            if (virtualLevel > previousLevel) {
                levelledSkills.add(skill);
                sendMessage = true;
            }
        }
    }

    private void attemptNotify() {
        sendMessage = false;
        StringBuilder skillMessage = new StringBuilder();
        int index = 0;
        Map<String, Integer> lSkills = new HashMap<>();

        for (String skill : levelledSkills) {
            if (index > 0) {
                if (levelledSkills.size() > 2) {
                    skillMessage.append(',');
                }
                skillMessage.append(' ');
                if (index + 1 == levelledSkills.size()) {
                    skillMessage.append("and ");
                }
            }
            skillMessage.append(String.format("%s to %s", skill, currentLevels.get(skill)));
            lSkills.put(skill, currentLevels.get(skill));
            index++;
        }

        levelledSkills.clear();
        String fullNotification = StringUtils.replaceEach(
            config.levelNotifyMessage(),
            new String[] { "%USERNAME%", "%SKILL%" },
            new String[] { Utils.getPlayerName(client), skillMessage.toString() }
        );

        createMessage(DinkPluginConfig::levelSendImage, NotificationBody.builder()
            .content(fullNotification)
            .extra(new LevelNotificationData(lSkills, currentLevels))
            .screenshotFile("levelImage.png")
            .type(NotificationType.LEVEL)
            .build());
    }

    private boolean checkLevelInterval(int level) {
        int interval = config.levelInterval();
        return interval <= 1
            || level == 99
            || level % interval == 0;
    }
}
