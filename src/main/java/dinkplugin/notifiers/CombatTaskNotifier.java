package dinkplugin.notifiers;

import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CombatAchievementData;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.GameStateChanged;
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
        this.totalByTier.clear();
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            this.reset();
    }

    public void onTick() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            this.reset();
        } else if (totalByTier.size() < TIER_COUNT) {
            this.populateTotalByTier();
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

        // Jagex does the same min logic: https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,ca_overview_create_tiers%5D.cs2#L28-L32
        int totalTierTasks = totalByTier.getOrDefault(tier, -1);
        int completedTierTasks = Math.min(client.getVarbitValue(tier.getCompletedVarbitId()), totalTierTasks);
        boolean validCount = totalTierTasks > 0 && completedTierTasks > 0;

        CombatAchievementData extra = new CombatAchievementData(
            tier,
            task,
            validCount ? completedTierTasks : null,
            validCount ? totalTierTasks : null
        );

        createMessage(config.combatTaskSendImage(), NotificationBody.<CombatAchievementData>builder()
            .type(NotificationType.COMBAT_ACHIEVEMENT)
            .text(message)
            .playerName(player)
            .extra(extra)
            .build());
    }

    private void populateTotalByTier() {
        // note: must be called from client thread
        CombatAchievementTier[] tiers = CombatAchievementTier.values();
        for (int i = 0; i < TIER_COUNT; i++) {
            CombatAchievementTier tier = tiers[i];
            client.runScript(TIER_TOTAL_SCRIPT_ID, i + 1); // 1-indexed in cs2
            totalByTier.put(tier, client.getIntStack()[0]);
        }
        log.debug("Finished initializing tier totals: {}", totalByTier);
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
