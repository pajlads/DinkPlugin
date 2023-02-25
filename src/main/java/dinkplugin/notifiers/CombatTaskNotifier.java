package dinkplugin.notifiers;

import dinkplugin.util.Utils;
import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CombatAchievementData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class CombatTaskNotifier extends BaseNotifier {
    private static final Pattern ACHIEVEMENT_PATTERN = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) combat task: (?<task>.+)\\.");
    public static final String REPEAT_WARNING = "Combat Task notifier will fire duplicates unless you disable the game setting: Combat Achievement Tasks - Repeat completion";

    @Varbit
    public static final int COMBAT_TASK_REPEAT_POPUP = 12456;

    private static final int TIER_COUNT = CombatAchievementTier.TIER_BY_LOWER_NAME.size();

    /**
     * ID for script procedure that calculates the number of total tasks in a given tier.
     * <p>
     * Argument: integer corresponding to 1 plus {@link CombatAchievementTier#ordinal()}
     *
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,ca_tasks_tier_total%5D.cs2">ClientScript Reference</a>
     */
    private static final int TIER_TOTAL_SCRIPT_ID = 4789;

    private static final int SCRIPT_INACTIVE = 0, SCRIPT_RUNNING = -1;

    private final AtomicInteger currentScriptArgument = new AtomicInteger(SCRIPT_INACTIVE);

    private final Map<CombatAchievementTier, Integer> totalByTier = Collections.synchronizedMap(new EnumMap<>(CombatAchievementTier.class));

    @Inject
    private ClientThread clientThread;

    @Override
    public boolean isEnabled() {
        return config.notifyCombatTask() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.combatTaskWebhook();
    }

    public void reset() {
        this.currentScriptArgument.set(SCRIPT_INACTIVE);
        this.totalByTier.clear();
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            this.reset();
    }

    public void onTick() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            this.reset();
        } else if (currentScriptArgument.get() == SCRIPT_INACTIVE && totalByTier.size() < TIER_COUNT && currentScriptArgument.compareAndSet(SCRIPT_INACTIVE, SCRIPT_RUNNING)) {
            for (int i = 1; i <= TIER_COUNT; i++) {
                client.runScript(TIER_TOTAL_SCRIPT_ID, i);
            }
        }
    }

    public void onPreScript(ScriptPreFired event) {
        if (event.getScriptId() != TIER_TOTAL_SCRIPT_ID || currentScriptArgument.get() != SCRIPT_RUNNING)
            return;

        Object[] args = event.getScriptEvent().getArguments();
        if (args.length != 2)
            return;

        Object arg = args[1]; // index 0 is script id
        if (arg instanceof Integer) {
            currentScriptArgument.compareAndSet(SCRIPT_RUNNING, (Integer) arg);
        }
    }

    public void onPostScript(ScriptPostFired event) {
        if (event.getScriptId() != TIER_TOTAL_SCRIPT_ID)
            return;

        int arg = currentScriptArgument.get();
        if (arg <= SCRIPT_INACTIVE || arg > TIER_COUNT || client.getIntStackSize() < 1)
            return;

        CombatAchievementTier tier = CombatAchievementTier.values()[arg - 1];
        totalByTier.put(tier, client.getIntStack()[0]);

        if (arg < TIER_COUNT) {
            currentScriptArgument.compareAndSet(arg, SCRIPT_RUNNING);
        } else {
            currentScriptArgument.compareAndSet(arg, SCRIPT_INACTIVE);
            log.debug("Finished initializing tier totals: {}", totalByTier);
        }
    }

    public void onGameMessage(String message) {
        if (isEnabled()) {
            parse(message).ifPresent(pair ->
                clientThread.invokeLater(() -> handle(pair.getLeft(), pair.getRight()))
            );
        }
    }

    private void handle(CombatAchievementTier tier, String task) {
        if (tier.ordinal() < config.minCombatAchievementTier().ordinal())
            return;

        String player = Utils.getPlayerName(client);
        String message = StringUtils.replaceEach(
            config.combatTaskMessage(),
            new String[] { "%USERNAME%", "%TIER%", "%TASK%" },
            new String[] { player, tier.toString(), task }
        );

        int totalTierTasks = totalByTier.getOrDefault(tier, -1);
        int tierTasksCompleted = Math.min(client.getVarbitValue(tier.getCompletedVarbitId()), totalTierTasks);
        boolean validCount = totalTierTasks > 0 && tierTasksCompleted > 0;

        CombatAchievementData extra = new CombatAchievementData(
            tier,
            task,
            validCount ? tierTasksCompleted : null,
            validCount ? totalTierTasks : null
        );

        createMessage(config.combatTaskSendImage(), NotificationBody.<CombatAchievementData>builder()
            .type(NotificationType.COMBAT_ACHIEVEMENT)
            .text(message)
            .playerName(player)
            .extra(extra)
            .build());
    }

    @VisibleForTesting
    static Optional<Pair<CombatAchievementTier, String>> parse(String message) {
        Matcher matcher = ACHIEVEMENT_PATTERN.matcher(message);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(matcher.group("tier"))
            .map(CombatAchievementTier.TIER_BY_LOWER_NAME::get)
            .map(tier -> Pair.of(tier, matcher.group("task")));
    }
}
