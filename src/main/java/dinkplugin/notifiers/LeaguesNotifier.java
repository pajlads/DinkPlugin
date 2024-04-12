package dinkplugin.notifiers;

import dinkplugin.domain.LeagueRelicTier;
import dinkplugin.domain.LeagueTaskDifficulty;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LeaguesAreaNotificationData;
import dinkplugin.notifiers.data.LeaguesRelicNotificationData;
import dinkplugin.notifiers.data.LeaguesTaskNotificationData;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.annotations.Varp;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LeaguesNotifier extends BaseNotifier {
    private static final String AREA_UNLOCK_PREFIX = "Congratulations, you've unlocked a new area: ";
    private static final String RELIC_UNLOCK_PREFIX = "Congratulations, you've unlocked a new Relic: ";
    private static final Pattern TASK_REGEX = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) task: (?<task>.+)\\.");

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/fa31b06ec5a9f6636bf9b9d5cbffbb71df022d06/scripts/%5Bproc%2Cleague_areas_progress_bar%5D.cs2#L177">CS2 Reference</a>
     */
    @VisibleForTesting
    static final @Varbit int TASKS_COMPLETED_ID = 10046;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/fa31b06ec5a9f6636bf9b9d5cbffbb71df022d06/scripts/%5Bproc%2Cscript730%5D.cs2#L86">CS2 Reference</a>
     */
    @VisibleForTesting
    static final @Varp int POINTS_EARNED_ID = 2614;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/fa31b06ec5a9f6636bf9b9d5cbffbb71df022d06/scripts/%5Bproc%2Cleague_areas_draw_interface%5D.cs2#L28-L55">CS2 Reference</a>
     */
    @VisibleForTesting
    static final @Varbit int FIVE_AREAS = 10666, FOUR_AREAS = 10665, THREE_AREAS = 10664, TWO_AREAS = 10663;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/fa31b06ec5a9f6636bf9b9d5cbffbb71df022d06/scripts/[proc%2Cscript2451].cs2#L3-L6">CS2 Reference</a>
     * @see <a href="https://abextm.github.io/cache2/#/viewer/enum/2670">Enum Reference</a>
     * @see <a href="https://abextm.github.io/cache2/#/viewer/struct/4699">Struct Reference</a>
     */
    @VisibleForTesting
    static final @Varbit int LEAGUES_VERSION = 10032; // 4 for Leagues IV

    /**
     * Trophy name by the required points, in a binary search tree.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League#Trophies">Wiki Reference</a>
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/fa31b06ec5a9f6636bf9b9d5cbffbb71df022d06/scripts/%5Bproc%2Cscript731%5D.cs2#L3">CS2 Reference</a>
     */
    private static final NavigableMap<Integer, String> TROPHY_BY_POINTS;

    /**
     * Mapping of each relic name to the tier (1-8).
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Relics">Wiki Reference</a>
     */
    private static final Map<String, LeagueRelicTier> TIER_BY_RELIC;

    /**
     * Mapping of the number of tasks required to unlock an area to the area index (0-3).
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Areas">Wiki reference</a>
     */
    private static final NavigableMap<Integer, Integer> AREA_BY_TASKS;

    @Override
    public boolean isEnabled() {
        return config.notifyLeagues() &&
            client.getVarbitValue(LEAGUES_VERSION) == 4 &&
            client.getWorldType().contains(WorldType.SEASONAL) &&
            settingsManager.isNamePermitted(client.getLocalPlayer().getName());
    }

    @Override
    protected String getWebhookUrl() {
        return config.leaguesWebhook();
    }

    public void onGameMessage(String message) {
        if (!isEnabled()) {
            return;
        }
        if (message.startsWith(AREA_UNLOCK_PREFIX)) {
            if (config.leaguesAreaUnlock()) {
                String area = message.substring(AREA_UNLOCK_PREFIX.length(), message.length() - 1);
                notifyAreaUnlock(area);
            }
        } else if (message.startsWith(RELIC_UNLOCK_PREFIX)) {
            if (config.leaguesRelicUnlock()) {
                String relic = message.substring(RELIC_UNLOCK_PREFIX.length(), message.length() - 1);
                notifyRelicUnlock(relic);
            }
        } else if (config.leaguesTaskCompletion()) {
            Matcher matcher = TASK_REGEX.matcher(message);
            if (matcher.find()) {
                LeagueTaskDifficulty tier = LeagueTaskDifficulty.TIER_BY_LOWER_NAME.get(matcher.group("tier"));
                if (tier != null && tier.ordinal() >= config.leaguesTaskMinTier().ordinal()) {
                    notifyTaskCompletion(tier, matcher.group("task"));
                }
            }
        }
    }

    private void notifyAreaUnlock(String area) {
        Map.Entry<Integer, String> unlocked = numAreasUnlocked();

        int tasksCompleted = client.getVarbitValue(TASKS_COMPLETED_ID);
        Integer tasksForNextArea = AREA_BY_TASKS.ceilingKey(tasksCompleted + 1);
        Integer tasksUntilNextArea = tasksForNextArea != null ? tasksForNextArea - tasksCompleted : null;

        if (unlocked == null) {
            int i = AREA_BY_TASKS.floorEntry(Math.max(tasksCompleted, 0)).getValue();
            unlocked = Map.entry(i, ith(i));
        }

        String playerName = Utils.getPlayerName(client);
        Template text = Template.builder()
            .template("%USERNAME% selected their %I_TH% region: %AREA%.")
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%I_TH%", Replacements.ofText(unlocked.getValue()))
            .replacement("%AREA%", Replacements.ofWiki(area, "Trailblazer Reloaded League/Areas/" + area))
            .build();
        createMessage(config.leaguesSendImage(), NotificationBody.builder()
            .type(NotificationType.LEAGUES_AREA)
            .text(text)
            .extra(new LeaguesAreaNotificationData(area, unlocked.getKey(), tasksCompleted, tasksUntilNextArea))
            .playerName(playerName)
            .seasonalWorld(true)
            .build());
    }

    private void notifyRelicUnlock(String relic) {
        int points = client.getVarpValue(POINTS_EARNED_ID);
        Integer pointsOfNextTier = LeagueRelicTier.TIER_BY_POINTS.ceilingKey(points + 1);
        Integer pointsUntilNextTier = pointsOfNextTier != null ? pointsOfNextTier - points : null;

        LeagueRelicTier relicTier = TIER_BY_RELIC.get(relic);
        if (relicTier == null) {
            // shouldn't happen, but just to be safe
            log.warn("Unknown relic encountered: {}", relic);
            relicTier = LeagueRelicTier.TIER_BY_POINTS.floorEntry(Math.max(points, 0)).getValue();
        }
        int tier = relicTier.ordinal() + 1;
        int requiredPoints = relicTier.getPoints();

        String playerName = Utils.getPlayerName(client);
        Template text = Template.builder()
            .template("%USERNAME% unlocked a Tier %TIER% Relic: %RELIC%.")
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%TIER%", Replacements.ofText(String.valueOf(tier)))
            .replacement("%RELIC%", Replacements.ofWiki(relic))
            .build();
        createMessage(config.leaguesSendImage(), NotificationBody.builder()
            .type(NotificationType.LEAGUES_RELIC)
            .text(text)
            .extra(new LeaguesRelicNotificationData(relic, tier, requiredPoints, points, pointsUntilNextTier))
            .playerName(playerName)
            .seasonalWorld(true)
            .build());
    }

    private void notifyTaskCompletion(LeagueTaskDifficulty tier, String task) {
        int taskPoints = tier.getPoints();
        int totalPoints = client.getVarpValue(POINTS_EARNED_ID);
        int tasksCompleted = client.getVarbitValue(TASKS_COMPLETED_ID);
        String playerName = Utils.getPlayerName(client);

        Integer nextAreaTasks = AREA_BY_TASKS.ceilingKey(tasksCompleted + 1);
        Integer tasksUntilNextArea = nextAreaTasks != null ? nextAreaTasks - tasksCompleted : null;

        Map.Entry<Integer, String> trophy = TROPHY_BY_POINTS.floorEntry(totalPoints);
        Integer prevTrophyPoints;
        if (trophy != null) {
            prevTrophyPoints = TROPHY_BY_POINTS.floorKey(totalPoints - taskPoints);
        } else {
            prevTrophyPoints = null;
        }
        boolean newTrophy = trophy != null && (prevTrophyPoints == null || trophy.getKey() > prevTrophyPoints);
        String justEarnedTrophy = newTrophy ? trophy.getValue() : null;
        Integer nextTrophyPoints = TROPHY_BY_POINTS.ceilingKey(totalPoints + 1);
        Integer pointsUntilNextTrophy = nextTrophyPoints != null ? nextTrophyPoints - totalPoints : null;

        Integer nextRelicPoints = LeagueRelicTier.TIER_BY_POINTS.ceilingKey(totalPoints + 1);
        Integer pointsUntilNextRelic = nextRelicPoints != null ? nextRelicPoints - totalPoints : null;

        Template text = Template.builder()
            .template(newTrophy
                ? "%USERNAME% completed a %TIER% task, %TASK%, unlocking the %TROPHY% trophy!"
                : "%USERNAME% completed a %TIER% task: %TASK%.")
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%TIER%", Replacements.ofText(tier.getDisplayName()))
            .replacement("%TASK%", Replacements.ofWiki(task, "Trailblazer_Reloaded_League/Tasks"))
            .replacement("%TROPHY%", newTrophy
                ? Replacements.ofWiki(trophy.getValue(), String.format("Trailblazer reloaded %s trophy", trophy.getValue().toLowerCase()))
                : Replacements.ofText("?"))
            .build();
        createMessage(config.leaguesSendImage(), NotificationBody.builder()
            .type(NotificationType.LEAGUES_TASK)
            .text(text)
            .extra(new LeaguesTaskNotificationData(task, tier, taskPoints, totalPoints, tasksCompleted, tasksUntilNextArea, pointsUntilNextRelic, pointsUntilNextTrophy, justEarnedTrophy))
            .playerName(playerName)
            .seasonalWorld(true)
            .build());
    }

    /**
     * @return the number of areas that have been unlocked as integer and human name
     */
    private Map.Entry<Integer, String> numAreasUnlocked() {
        // While Jagex's code has 5 areas (2 default, 3 discretionary),
        // most players think just in terms of the 3 discretionary areas,
        // so we disregard Misthalin and consider Karamja as the zeroth area.
        // Thus, the number of unlocked areas is bounded by 3 (instead of 5).
        if (client.getVarbitValue(FIVE_AREAS) > 0) {
            return Map.entry(3, ith(3));
        }
        if (client.getVarbitValue(FOUR_AREAS) > 0) {
            return Map.entry(2, ith(2));
        }
        if (client.getVarbitValue(THREE_AREAS) > 0) {
            return Map.entry(1, ith(1));
        }
        if (client.getVarbitValue(TWO_AREAS) > 0) {
            return Map.entry(0, ith(0)); // Karamja
        }
        return null;
    }

    private static String ith(int i) {
        if (i == 0) return "zeroth";
        if (i == 1) return "first";
        if (i == 2) return "second";
        if (i == 3) return "third";
        if (i == 4) return "fourth";
        if (i == 5) return "fifth";
        return String.valueOf(i);
    }

    static {
        AREA_BY_TASKS = Collections.unmodifiableNavigableMap(
            new TreeMap<>(Map.of(0, 0, 60, 1, 140, 2, 300, 3))
        );

        NavigableMap<Integer, String> thresholds = new TreeMap<>();
        thresholds.put(2_500, "Bronze");
        thresholds.put(5_000, "Iron");
        thresholds.put(10_000, "Steel");
        thresholds.put(18_000, "Mithril");
        thresholds.put(28_000, "Adamant");
        thresholds.put(42_000, "Rune");
        thresholds.put(56_000, "Dragon");
        TROPHY_BY_POINTS = Collections.unmodifiableNavigableMap(thresholds);

        TIER_BY_RELIC = Map.ofEntries(
            Map.entry("Endless Harvest", LeagueRelicTier.ONE),
            Map.entry("Production Prodigy", LeagueRelicTier.ONE),
            Map.entry("Trickster", LeagueRelicTier.ONE),
            Map.entry("Fairy's Flight", LeagueRelicTier.TWO),
            Map.entry("Globetrotter", LeagueRelicTier.TWO),
            Map.entry("Banker's Note", LeagueRelicTier.THREE),
            Map.entry("Fire Sale", LeagueRelicTier.THREE),
            Map.entry("Archer's Embrace", LeagueRelicTier.FOUR),
            Map.entry("Brawler's Resolve", LeagueRelicTier.FOUR),
            Map.entry("Superior Sorcerer", LeagueRelicTier.FOUR),
            Map.entry("Bloodthirsty", LeagueRelicTier.FIVE),
            Map.entry("Infernal Gathering", LeagueRelicTier.FIVE),
            Map.entry("Treasure Seeker", LeagueRelicTier.FIVE),
            Map.entry("Equilibrium", LeagueRelicTier.SIX),
            Map.entry("Farmer's Fortune", LeagueRelicTier.SIX),
            Map.entry("Ruinous Powers", LeagueRelicTier.SIX),
            Map.entry("Berserker", LeagueRelicTier.SEVEN),
            Map.entry("Soul Stealer", LeagueRelicTier.SEVEN),
            Map.entry("Weapon Master", LeagueRelicTier.SEVEN),
            Map.entry("Guardian", LeagueRelicTier.EIGHT),
            Map.entry("Executioner", LeagueRelicTier.EIGHT),
            Map.entry("Undying Retribution", LeagueRelicTier.EIGHT)
        );
    }
}
