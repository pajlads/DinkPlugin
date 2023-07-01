package dinkplugin.notifiers;

import com.google.common.collect.ImmutableSet;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.message.templating.impl.JoiningReplacement;
import dinkplugin.notifiers.data.LevelNotificationData;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static net.runelite.api.Experience.MAX_REAL_LEVEL;

@Slf4j
@Singleton
public class LevelNotifier extends BaseNotifier {
    private static final int INIT_CLIENT_TICKS = 50; // 1000ms
    private static final String COMBAT_NAME = "Combat";
    private static final Set<String> COMBAT_COMPONENTS;
    private final BlockingQueue<String> levelledSkills = new ArrayBlockingQueue<>(Skill.values().length);
    private final Map<String, Integer> currentLevels = new HashMap<>();
    private final AtomicInteger ticksWaited = new AtomicInteger();
    private final AtomicInteger initTicks = new AtomicInteger();

    @Inject
    private ClientThread clientThread;

    @Override
    protected String getWebhookUrl() {
        return config.levelWebhook();
    }

    public void initLevels() {
        if (initTicks.getAndSet(INIT_CLIENT_TICKS) > 0)
            return; // init task is already queued

        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGGED_IN)
                return false;

            if (initTicks.updateAndGet(i -> i - 1) > 0)
                return false; // still need to wait more ticks

            for (Skill skill : Skill.values()) {
                if (skill != Skill.OVERALL) {
                    // uses log(n) operation to support virtual levels
                    currentLevels.put(skill.getName(), Experience.getLevelForXp(client.getSkillExperience(skill)));
                }
            }
            currentLevels.put(COMBAT_NAME, calculateCombatLevel());
            log.debug("Initialized current skill levels: {}", currentLevels);
            return true;
        });
    }

    public void reset() {
        clientThread.invoke(() -> {
            currentLevels.clear();
            levelledSkills.clear();
            ticksWaited.set(0);
        });
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

        int virtualLevel = level < MAX_REAL_LEVEL ? level : Experience.getLevelForXp(xp); // avoid log(n) query when not needed
        Integer previousLevel = currentLevels.put(skill, virtualLevel);

        if (previousLevel == null) {
            initLevels();
            return;
        }

        if (virtualLevel < previousLevel) {
            // base skill level should never regress; reset notifier state
            reset();
            return;
        }

        // Check normal skill level up
        checkLevelUp(config.notifyLevel(), skill, previousLevel, virtualLevel);

        // Skip combat level checking if no level up has occurred
        if (virtualLevel <= previousLevel) {
            // only return if we don't need to initialize combat level for the first time
            if (currentLevels.containsKey(COMBAT_NAME))
                return;
        }

        // Check for combat level increase
        if (COMBAT_COMPONENTS.contains(skill)) {
            int combatLevel = calculateCombatLevel();
            Integer previousCombatLevel = currentLevels.put(COMBAT_NAME, combatLevel);
            checkLevelUp(config.notifyLevel() && config.levelNotifyCombat(), COMBAT_NAME, previousCombatLevel, combatLevel);
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

        if (!checkLevelInterval(previousLevel, currentLevel, COMBAT_NAME.equals(skill))) {
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
        // Prepare level state
        List<String> levelled = new ArrayList<>(levelledSkills.size());
        levelledSkills.drainTo(levelled);
        int count = levelled.size();
        Map<String, Integer> lSkills = new HashMap<>(count);
        Map<String, Integer> currentLevels = new HashMap<>(this.currentLevels);

        // Build skillMessage and populate lSkills
        JoiningReplacement.JoiningReplacementBuilder skillMessage = JoiningReplacement.builder();
        for (int index = 0; index < count; index++) {
            String skill = levelled.get(index);
            if (index > 0) {
                if (count > 2) {
                    skillMessage.component(Replacements.ofText(","));
                }
                skillMessage.component(Replacements.ofText(" "));
                if (index + 1 == count) {
                    skillMessage.component(Replacements.ofText("and "));
                }
            }
            Integer level = currentLevels.get(skill);
            skillMessage
                .component(Replacements.ofWiki(skill, COMBAT_NAME.equals(skill) ? "Combat level" : skill))
                .component(Replacements.ofText(" to " + level));
            lSkills.put(skill, level);
        }

        // Separately check for combat level increase for extra data
        Boolean combatLevelUp = lSkills.remove(COMBAT_NAME) != null; // remove Combat from levelledSkills
        Integer combatLevel = currentLevels.remove(COMBAT_NAME); // remove Combat from allSkills
        if (combatLevel == null) {
            combatLevelUp = null; // combat level was not populated, so it is unclear whether combat level increased
            combatLevel = calculateCombatLevel(); // populate combat level for extra data
        } else if (!config.levelNotifyCombat()) {
            combatLevelUp = null; // if levelNotifyCombat is disabled, it is unclear whether combat level increased
        }
        LevelNotificationData.CombatLevel combatData = new LevelNotificationData.CombatLevel(combatLevel, combatLevelUp);

        // Select relevant thumbnail url
        String thumbnail;
        if (count == 1) {
            // Use skill icon if only one skill was levelled up
            thumbnail = getSkillIcon(levelled.get(0));
        } else if (combatLevelUp != null && combatLevelUp && count == 2) {
            // Upon a combat level increase, use icon of the other combat-related skill that was levelled up
            String skill = levelled.get(0);
            if (COMBAT_NAME.equals(skill))
                skill = levelled.get(1);
            thumbnail = getSkillIcon(skill);
        } else {
            // Fall back to NotificationType#getThumbnail
            thumbnail = null;
        }

        // Populate message template
        Template fullNotification = Template.builder()
            .template(config.levelNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%SKILL%", skillMessage.build())
            .build();

        // Fire notification
        createMessage(config.levelSendImage(), NotificationBody.builder()
            .text(fullNotification)
            .extra(new LevelNotificationData(lSkills, currentLevels, combatData))
            .type(NotificationType.LEVEL)
            .thumbnailUrl(thumbnail)
            .build());
    }

    private boolean checkLevelInterval(int previous, int level, boolean skipVirtualCheck) {
        if (level < config.levelMinValue())
            return false;

        if (!skipVirtualCheck && level > MAX_REAL_LEVEL && !config.levelNotifyVirtual())
            return false;

        int interval = config.levelInterval();
        if (interval <= 1 || level == MAX_REAL_LEVEL)
            return true;

        int intervalOverride = config.levelIntervalOverride();
        if (intervalOverride > 0 && level >= intervalOverride) {
            return true;
        }
        // Check levels in (previous, current] for divisibility by interval
        // Allows for firing notification if jumping over a level that would've notified
        int remainder = level % interval;
        return remainder == 0 || (level - remainder) > previous;
    }

    private int calculateCombatLevel() {
        return Experience.getCombatLevel(
            getRealLevel(Skill.ATTACK),
            getRealLevel(Skill.STRENGTH),
            getRealLevel(Skill.DEFENCE),
            getRealLevel(Skill.HITPOINTS),
            getRealLevel(Skill.MAGIC),
            getRealLevel(Skill.RANGED),
            getRealLevel(Skill.PRAYER)
        );
    }

    private int getRealLevel(Skill skill) {
        Integer cachedLevel = currentLevels.get(skill.getName());
        return cachedLevel != null
            ? Math.min(cachedLevel, MAX_REAL_LEVEL)
            : client.getRealSkillLevel(skill);
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
