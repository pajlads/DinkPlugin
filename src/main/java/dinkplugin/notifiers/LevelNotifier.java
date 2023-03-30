package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LevelNotificationData;
import dinkplugin.util.Utils;
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
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
public class LevelNotifier extends BaseNotifier {
    private static final String COMBAT_NAME = "Combat";
    private static final Set<String> COMBAT_COMPONENTS;
    private final BlockingQueue<String> levelledSkills = new ArrayBlockingQueue<>(Skill.values().length);
    private final Map<String, Integer> currentLevels = new ConcurrentHashMap<>();
    private final AtomicInteger ticksWaited = new AtomicInteger();

    @Override
    protected String getWebhookUrl() {
        return config.levelWebhook();
    }

    public void initLevels() {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        for (Skill skill : Skill.values()) {
            if (skill != Skill.OVERALL) {
                // uses log(n) operation to support virtual levels
                currentLevels.put(skill.getName(), Experience.getLevelForXp(client.getSkillExperience(skill)));
            }
        }
        currentLevels.put(COMBAT_NAME, getCombatLevel());
    }

    public void reset() {
        currentLevels.clear();
        levelledSkills.clear();
        ticksWaited.set(0);
    }

    public void onTick() {
        if (currentLevels.isEmpty()) {
            initLevels();
            return;
        }

        if (levelledSkills.isEmpty()) {
            return;
        }

        int ticks = ticksWaited.incrementAndGet();
        // We wait a couple extra ticks so we can ensure that we process all the levels of the previous tick
        if (ticks > 2) {
            ticksWaited.set(0);
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

        if (previousLevel != null && virtualLevel < previousLevel) {
            // base skill level should never regress; reset notifier state
            reset();
            return;
        }

        // Check normal skill level up
        checkLevelUp(config.notifyLevel(), skill, previousLevel, virtualLevel);

        // Skip combat level checking if no level up has occurred
        if (previousLevel == null || virtualLevel <= previousLevel) {
            if (currentLevels.containsKey(COMBAT_NAME))
                return;
        }

        // Check for combat level increase
        if (COMBAT_COMPONENTS.contains(skill)) {
            int combatLevel = getCombatLevel();
            Integer previousCombatLevel = currentLevels.put(COMBAT_NAME, combatLevel);
            checkLevelUp(config.levelNotifyCombat(), COMBAT_NAME, previousCombatLevel, combatLevel);
        }
    }

    private void checkLevelUp(boolean configEnabled, String skill, Integer previousLevel, int currentLevel) {
        if (previousLevel == null || currentLevel <= previousLevel) {
            log.trace("Ignoring non-level-up for {}: {}", skill, currentLevel);
            return;
        }

        if (!configEnabled) {
            log.trace("Ignoring level up of {} to {} due to disabled config setting", skill, currentLevel);
            return;
        }

        if (!checkLevelInterval(previousLevel, currentLevel)) {
            log.trace("Ignoring level up of {} from {} to {} that does not align with config interval", skill, previousLevel, currentLevel);
            return;
        }

        if (levelledSkills.offer(skill)) {
            log.debug("Observed level up for {} to {}", skill, currentLevel);

            // allow more accumulation of level ups into single notification
            ticksWaited.set(0);
        }
    }

    private void attemptNotify() {
        StringBuilder skillMessage = new StringBuilder();
        List<String> levelled = new ArrayList<>(levelledSkills.size());
        levelledSkills.drainTo(levelled);
        int count = levelled.size();
        Map<String, Integer> lSkills = new HashMap<>(count);
        Map<String, Integer> currentLevels = new HashMap<>(this.currentLevels);

        for (int index = 0; index < count; index++) {
            String skill = levelled.get(index);
            if (index > 0) {
                if (count > 2) {
                    skillMessage.append(',');
                }
                skillMessage.append(' ');
                if (index + 1 == count) {
                    skillMessage.append("and ");
                }
            }
            Integer level = currentLevels.get(skill);
            skillMessage.append(String.format("%s to %s", skill, level));
            lSkills.put(skill, level);
        }

        Boolean combatLevelUp = lSkills.remove(COMBAT_NAME) != null;
        Integer combatLevel = currentLevels.remove(COMBAT_NAME);
        if (combatLevel == null) {
            combatLevelUp = null;
            combatLevel = getCombatLevel();
        } else if (!config.levelNotifyCombat()) {
            combatLevelUp = null;
        }
        LevelNotificationData.CombatLevel combatData = new LevelNotificationData.CombatLevel(combatLevel, combatLevelUp);

        String thumbnail = count == 1 ? getSkillIcon(levelled.get(0)) : null;
        String fullNotification = StringUtils.replaceEach(
            config.levelNotifyMessage(),
            new String[] { "%USERNAME%", "%SKILL%" },
            new String[] { Utils.getPlayerName(client), skillMessage.toString() }
        );

        createMessage(config.levelSendImage(), NotificationBody.builder()
            .text(fullNotification)
            .extra(new LevelNotificationData(lSkills, currentLevels, combatData))
            .type(NotificationType.LEVEL)
            .thumbnailUrl(thumbnail)
            .build());
    }

    private boolean checkLevelInterval(int previous, int level) {
        if (level < config.levelMinValue())
            return false;

        if (level > 99 && !config.levelNotifyVirtual())
            return false;

        int interval = config.levelInterval();
        if (interval <= 1 || level == 99)
            return true;

        // Check levels in (previous, current] for divisibility by interval
        // Allows for firing notification if jumping over a level that would've notified
        int remainder = level % interval;
        return remainder == 0 || (level - remainder) > previous;
    }

    private int getCombatLevel() {
        return Experience.getCombatLevel(
            client.getRealSkillLevel(Skill.ATTACK),
            client.getRealSkillLevel(Skill.STRENGTH),
            client.getRealSkillLevel(Skill.DEFENCE),
            client.getRealSkillLevel(Skill.HITPOINTS),
            client.getRealSkillLevel(Skill.MAGIC),
            client.getRealSkillLevel(Skill.RANGED),
            client.getRealSkillLevel(Skill.PRAYER)
        );
    }

    private static String getSkillIcon(String skillName) {
        return Utils.WIKI_IMG_BASE_URL + skillName + "_icon.png";
    }

    static {
        COMBAT_COMPONENTS = ImmutableSet.of(
            Skill.ATTACK.getName(),
            Skill.STRENGTH.getName(),
            Skill.DEFENCE.getName(),
            Skill.HITPOINTS.getName(),
            Skill.MAGIC.getName(),
            Skill.RANGED.getName(),
            Skill.PRAYER.getName()
        );
    }
}
