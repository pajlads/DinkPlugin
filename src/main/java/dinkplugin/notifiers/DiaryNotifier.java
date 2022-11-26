package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.domain.AchievementDiaries;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DiaryNotificationData;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DiaryNotifier extends BaseNotifier {
    private static final Map<Integer, AchievementDiaries.Diary> DIARIES = AchievementDiaries.INSTANCE.getDiaries();
    private final Map<Integer, Integer> diaryCompletionById = new HashMap<>();
    private int initDelayTicks = 0;

    @Override
    public boolean isEnabled() {
        return config.notifyAchievementDiary() && super.isEnabled();
    }

    public void reset() {
        this.diaryCompletionById.clear();
        this.initDelayTicks = 0;
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN)
            this.reset();
    }

    public void onTick() {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;

        if (initDelayTicks > 0) {
            initDelayTicks--;

            if (initDelayTicks <= 0)
                this.initCompleted();
        } else if (diaryCompletionById.isEmpty() && isEnabled()) {
            // mark diary completions to be initialized later
            this.initDelayTicks = 8;
        }
    }

    public void onVarbitChanged(VarbitChanged event) {
        int id = event.getVarbitId();
        if (id < 0) return;
        AchievementDiaries.Diary diary = DIARIES.get(id);
        if (diary == null) return;
        if (diaryCompletionById.isEmpty()) return;
        if (!super.isEnabled()) return;

        int value = event.getValue();
        Integer previous = diaryCompletionById.get(id);
        if (previous == null || value < previous) {
            reset();
        } else if (value > previous) {
            diaryCompletionById.put(id, value);

            if (value < 2 && (id == 3578 || id == 3599 || id == 3611)) {
                // Karamja special case: 0 = not started, 1 = started, 2 = completed tasks
                return;
            }

            if (checkDifficulty(diary))
                handle(diary);
        }
    }

    private void handle(AchievementDiaries.Diary diary) {
        int total = getTotalCompleted();
        String player = Utils.getPlayerName(client);
        String message = StringUtils.replaceEach(
            config.diaryNotifyMessage(),
            new String[] { "%USERNAME%", "%DIFFICULTY%", "%AREA%", "%TOTAL%" },
            new String[] { player, diary.getDifficulty().toString(), diary.getArea(), String.valueOf(total) }
        );

        createMessage(DinkPluginConfig::diarySendImage, NotificationBody.builder()
            .type(NotificationType.ACHIEVEMENT_DIARY)
            .content(message)
            .extra(new DiaryNotificationData(diary.getArea(), diary.getDifficulty(), total))
            .playerName(player)
            .screenshotFile("achievementDiaryImage.png")
            .build());
    }

    private boolean checkDifficulty(AchievementDiaries.Diary diary) {
        return config.notifyAchievementDiary() && diary.getDifficulty().ordinal() >= config.minDiaryDifficulty().ordinal();
    }

    private int getTotalCompleted() {
        int n = 0;

        for (Map.Entry<Integer, Integer> entry : diaryCompletionById.entrySet()) {
            int id = entry.getKey();
            int value = entry.getValue();
            if (value <= 0) continue;

            if (id != 3578 && id != 3599 && id != 3611) {
                n++;
            } else if (value > 1) {
                n++; // Karamja special case
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
    }
}
