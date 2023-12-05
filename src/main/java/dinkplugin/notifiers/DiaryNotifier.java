package dinkplugin.notifiers;

import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.DiaryNotificationData;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dinkplugin.domain.AchievementDiary.DIARIES;

@Slf4j
@Singleton
public class DiaryNotifier extends BaseNotifier {

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,script3971%5D.cs2">CS2 Reference</a>
     */
    static final int COMPLETED_TASKS_SCRIPT_ID = 3971;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,script3980%5D.cs2">CS2 Reference</a>
     */
    static final int TOTAL_TASKS_SCRIPT_ID = 3980;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,summary_diary_completed%5D.cs2">CS2 Reference</a>
     */
    private static final int COMPLETED_AREA_TASKS_SCRIPT_ID = 4072;

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,summary_diary_total%5D.cs2">CS2 Reference</a>
     */
    private static final int TOTAL_AREA_TASKS_SCRIPT_ID = 4073;

    private static final Pattern COMPLETION_REGEX = Pattern.compile("Congratulations! You have completed all of the (?<difficulty>.+) tasks in the (?<area>.+) area");
    private final Map<Integer, Integer> diaryCompletionById = new ConcurrentHashMap<>();
    private final AtomicInteger initDelayTicks = new AtomicInteger();
    private final AtomicInteger cooldownTicks = new AtomicInteger();

    @Inject
    private ClientThread clientThread;

    @Override
    public boolean isEnabled() {
        return config.notifyAchievementDiary() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.diaryWebhook();
    }

    public void reset() {
        this.diaryCompletionById.clear();
        this.initDelayTicks.set(0);
        this.cooldownTicks.set(0);
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            this.reset();
    }

    public void onTick() {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;

        cooldownTicks.getAndUpdate(i -> Math.max(i - 1, 0));
        int ticks = initDelayTicks.getAndUpdate(i -> Math.max(i - 1, 0));
        if (ticks > 0) {
            if (ticks == 1) {
                this.initCompleted();
            }
        } else if (diaryCompletionById.size() < DIARIES.size() && super.isEnabled()) {
            // mark diary completions to be initialized later
            this.initDelayTicks.set(4);
        }
    }

    public void onMessageBox(String message) {
        if (!isEnabled()) return;

        Matcher matcher = COMPLETION_REGEX.matcher(message);
        if (matcher.find()) {
            String difficultyStr = matcher.group("difficulty");
            AchievementDiary.Difficulty difficulty;
            try {
                difficulty = AchievementDiary.Difficulty.valueOf(difficultyStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Failed to match diary difficulty: {}", difficultyStr);
                return;
            }

            String area = matcher.group("area").trim();
            Optional<AchievementDiary> found = DIARIES.values().stream()
                .filter(e -> e.getDifficulty() == difficulty && Utils.containsEither(e.getArea(), area))
                .findAny();
            if (found.isPresent()) {
                AchievementDiary diary = found.get();
                int varbitId = diary.getId();
                if (isComplete(varbitId, 1)) {
                    diaryCompletionById.put(varbitId, 1);
                } else {
                    diaryCompletionById.put(varbitId, 2);
                }
                if (!checkDifficulty(difficulty)) return;

                clientThread.invokeLater(() -> handle(diary)); // 20ms delay to run scripts cleanly
            } else {
                log.warn("Failed to match diary area: {}", area);
            }
        }
    }

    public void onVarbitChanged(VarbitChanged event) {
        int id = event.getVarbitId();
        if (id < 0) return;
        AchievementDiary diary = DIARIES.get(id);
        if (diary == null) return;
        if (!super.isEnabled()) return;
        if (diaryCompletionById.isEmpty()) {
            if (client.getGameState() == GameState.LOGGED_IN && isComplete(id, event.getValue())) {
                // this log only occurs in exceptional circumstances (i.e., completion within seconds of logging in or enabling the plugin)
                log.info("Skipping {} {} diary completion that occurred before map initialization", diary.getDifficulty(), diary.getArea());
            }
            return;
        }

        int value = event.getValue();
        Integer previous = diaryCompletionById.get(id);
        if (previous == null) {
            // this log should not occur, barring a jagex oddity
            log.warn("Resetting since {} {} diary was not initialized with a valid value; received new value of {}", diary.getDifficulty(), diary.getArea(), value);
            reset();
        } else if (value < previous) {
            // this log should not occur, barring a jagex/runelite oddity
            log.info("Resetting since it appears {} {} diary has lost progress from {}; received new value of {}", diary.getDifficulty(), diary.getArea(), previous, value);
            reset();
        } else if (value > previous) {
            diaryCompletionById.put(id, value);

            if (isComplete(id, value)) {
                if (checkDifficulty(diary.getDifficulty())) {
                    clientThread.invokeLater(() -> handle(diary)); // 20ms delay to run scripts cleanly
                } else {
                    log.debug("Skipping {} {} diary due to low difficulty", diary.getDifficulty(), diary.getArea());
                }
            } else {
                // Karamja special case
                log.info("Skipping {} {} diary start (not a completion with value {})", diary.getDifficulty(), diary.getArea(), value);
            }
        }
    }

    private void handle(AchievementDiary diary) {
        if (cooldownTicks.getAndSet(2) > 0) {
            log.debug("Skipping diary completion during cooldown: {} {}", diary.getDifficulty(), diary.getArea());
            return;
        }

        client.runScript(DiaryNotifier.COMPLETED_TASKS_SCRIPT_ID);
        int completedTasks = client.getIntStack()[0];
        client.runScript(DiaryNotifier.TOTAL_TASKS_SCRIPT_ID);
        int totalTasks = client.getIntStack()[0];

        client.runScript(COMPLETED_AREA_TASKS_SCRIPT_ID, diary.getAreaId());
        int completedAreaTasks = client.getIntStack()[0];
        client.runScript(TOTAL_AREA_TASKS_SCRIPT_ID, diary.getAreaId());
        int totalAreaTasks = client.getIntStack()[0];

        int completedDiaries = getTotalCompleted();
        String player = Utils.getPlayerName(client);
        Template message = Template.builder()
            .template(config.diaryNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(player))
            .replacement("%DIFFICULTY%", Replacements.ofText(diary.getDifficulty().toString()))
            .replacement("%AREA%", Replacements.ofWiki(diary.getArea(), diary.getArea() + " Diary"))
            .replacement("%TOTAL%", Replacements.ofText(String.valueOf(completedDiaries)))
            .replacement("%TASKS_COMPLETE%", Replacements.ofText(String.valueOf(completedTasks)))
            .replacement("%TASKS_TOTAL%", Replacements.ofText(String.valueOf(totalTasks)))
            .replacement("%AREA_TASKS_COMPLETE%", Replacements.ofText(String.valueOf(completedAreaTasks)))
            .replacement("%AREA_TASKS_TOTAL%", Replacements.ofText(String.valueOf(totalAreaTasks)))
            .build();

        createMessage(config.diarySendImage(), NotificationBody.builder()
            .type(NotificationType.ACHIEVEMENT_DIARY)
            .text(message)
            .extra(new DiaryNotificationData(diary.getArea(), diary.getDifficulty(), completedDiaries, completedTasks, totalTasks, completedAreaTasks, totalAreaTasks))
            .playerName(player)
            .build());
    }

    private boolean checkDifficulty(AchievementDiary.Difficulty difficulty) {
        return config.notifyAchievementDiary() && difficulty.ordinal() >= config.minDiaryDifficulty().ordinal();
    }

    private int getTotalCompleted() {
        int n = 0;

        for (Map.Entry<Integer, Integer> entry : diaryCompletionById.entrySet()) {
            int id = entry.getKey();
            int value = entry.getValue();
            if (isComplete(id, value)) {
                n++;
            }
        }

        return n;
    }

    private void initCompleted() {
        if (!super.isEnabled()) return;
        for (Integer id : DIARIES.keySet()) {
            int value = client.getVarbitValue(id);
            if (value >= 0) {
                diaryCompletionById.put(id, value);
            }
        }
        log.debug("Finished initializing current diary completions: {} out of {}", getTotalCompleted(), diaryCompletionById.size());
    }

    public static boolean isComplete(int id, int value) {
        if (id == 3578 || id == 3599 || id == 3611) {
            // Karamja special case (except Elite): 0 = not started, 1 = started, 2 = completed tasks
            return value > 1;
        } else {
            // otherwise: 0 = not started, 1 = completed
            return value > 0;
        }
    }
}
