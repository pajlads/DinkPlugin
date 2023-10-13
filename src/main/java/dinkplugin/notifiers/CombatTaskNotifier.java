package dinkplugin.notifiers;

import com.google.common.collect.ImmutableMap;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.Utils;
import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CombatAchievementData;
import net.runelite.api.annotations.Varbit;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CombatTaskNotifier extends BaseNotifier {
    private static final Pattern ACHIEVEMENT_PATTERN = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) combat task: (?<task>.+)\\.");
    private static final Pattern TASK_POINTS = Pattern.compile("\\s+\\(\\d+ points?\\)$");
    public static final String REPEAT_WARNING = "Combat Task notifier will fire duplicates unless you disable the game setting: Combat Achievement Tasks - Repeat completion";

    @Varbit
    public static final int COMBAT_TASK_REPEAT_POPUP = 12456;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,ca_tasks_progress_bar%5D.cs2#L58">CS2 Reference</a>
     */
    @Varbit
    public static final int TOTAL_POINTS_ID = 14815;

    @Varbit
    public static final int GRANDMASTER_TOTAL_POINTS_ID = 14814;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,ca_tasks_progress_bar%5D.cs2#L6-L11">CS2 Reference</a>
     */
    @VisibleForTesting
    public static final Map<CombatAchievementTier, Integer> CUM_POINTS_VARBIT_BY_TIER;

    /**
     * The cumulative points needed to unlock rewards for each tier, in a Red-Black tree.
     * <p>
     * This is populated by {@link #initThresholds()} based on {@link #CUM_POINTS_VARBIT_BY_TIER}.
     *
     * @see <a href="https://gachi.gay/01CAv">Rewards Thresholds at the launch of the points-based system</a>
     */
    private final NavigableMap<Integer, CombatAchievementTier> cumulativeUnlockPoints = new TreeMap<>();

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

    public void onTick() {
        if (cumulativeUnlockPoints.size() < CUM_POINTS_VARBIT_BY_TIER.size())
            initThresholds();
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(message).ifPresent(pair -> handle(pair.getLeft(), pair.getRight()));
    }

    private void handle(CombatAchievementTier tier, String task) {
        if (tier.ordinal() < config.minCombatAchievementTier().ordinal())
            return;

        // delay notification for varbits to be updated
        clientThread.invokeAtTickEnd(() -> {
            int taskPoints = tier.getPoints();
            int totalPoints = client.getVarbitValue(TOTAL_POINTS_ID);

            Integer nextUnlockPointsThreshold = cumulativeUnlockPoints.ceilingKey(totalPoints + 1);
            Map.Entry<Integer, CombatAchievementTier> prev = cumulativeUnlockPoints.floorEntry(totalPoints);
            int prevThreshold = prev != null ? prev.getKey() : 0;

            Integer tierProgress, tierTotalPoints;
            if (nextUnlockPointsThreshold != null) {
                tierProgress = totalPoints - prevThreshold;
                tierTotalPoints = nextUnlockPointsThreshold - prevThreshold;
            } else {
                tierProgress = tierTotalPoints = null;
            }

            boolean crossedThreshold = prevThreshold > 0 && totalPoints - taskPoints < prevThreshold;
            CombatAchievementTier completedTier = crossedThreshold ? prev.getValue() : null;
            String completedTierName = completedTier != null ? completedTier.getDisplayName() : "N/A";

            String player = Utils.getPlayerName(client);
            Template message = Template.builder()
                .template(crossedThreshold ? config.combatTaskUnlockMessage() : config.combatTaskMessage())
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText(player))
                .replacement("%TIER%", Replacements.ofText(tier.toString()))
                .replacement("%TASK%", Replacements.ofWiki(task))
                .replacement("%POINTS%", Replacements.ofText(String.valueOf(taskPoints)))
                .replacement("%TOTAL_POINTS%", Replacements.ofText(String.valueOf(totalPoints)))
                .replacement("%COMPLETED%", Replacements.ofText(completedTierName))
                .build();

            createMessage(config.combatTaskSendImage(), NotificationBody.<CombatAchievementData>builder()
                .type(NotificationType.COMBAT_ACHIEVEMENT)
                .text(message)
                .playerName(player)
                .extra(new CombatAchievementData(tier, task, taskPoints, totalPoints, tierProgress, tierTotalPoints, completedTier))
                .build());
        });
    }

    private void initThresholds() {
        CUM_POINTS_VARBIT_BY_TIER.forEach((tier, varbitId) -> {
            int cumulativePoints = client.getVarbitValue(varbitId);
            if (cumulativePoints > 0)
                cumulativeUnlockPoints.put(cumulativePoints, tier);
        });
    }

    @VisibleForTesting
    static Optional<Pair<CombatAchievementTier, String>> parse(String message) {
        Matcher matcher = ACHIEVEMENT_PATTERN.matcher(message);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(matcher.group("tier"))
            .map(CombatAchievementTier.TIER_BY_LOWER_NAME::get)
            .map(tier -> Pair.of(
                tier,
                TASK_POINTS.matcher(
                    matcher.group("task")
                ).replaceFirst("") // remove points suffix
            ));
    }

    static {
        // noinspection UnstableApiUsage (builderWithExpectedSize is no longer @Beta in snapshot guava)
        CUM_POINTS_VARBIT_BY_TIER = ImmutableMap.<CombatAchievementTier, Integer>builderWithExpectedSize(6)
            .put(CombatAchievementTier.EASY, 4132) // 33 = 33 * 1
            .put(CombatAchievementTier.MEDIUM, 10660) // 115 = 33 + 41 * 2
            .put(CombatAchievementTier.HARD, 10661) // 304 = 115 + 63 * 3
            .put(CombatAchievementTier.ELITE, 14812) // 820 = 304 + 129 * 4
            .put(CombatAchievementTier.MASTER, 14813) // 1465 = 820 + 129 * 5
            .put(CombatAchievementTier.GRANDMASTER, GRANDMASTER_TOTAL_POINTS_ID) // 2005 = 1465 + 90 * 6
            .build();
    }
}
