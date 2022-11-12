package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.domain.AchievementDiaries;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DiaryNotificationData;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class DiaryNotifier extends BaseNotifier {
    private static final Map<Integer, AchievementDiaries.Diary> DIARIES = AchievementDiaries.INSTANCE.getDiaries();
    private final Map<Integer, Integer> diaryCompletionById = new HashMap<>();

    @Inject
    public DiaryNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyAchievementDiary() && super.isEnabled();
    }

    public void reset() {
        this.diaryCompletionById.clear();
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
            this.reset();
    }

    public void onTick() {
        // init diary completions
        if (diaryCompletionById.isEmpty() && isEnabled()) {
            Client client = plugin.getClient();
            for (Integer id : DIARIES.keySet()) {
                int value = client.getVarbitValue(id);
                if (value >= 0) {
                    diaryCompletionById.put(id, value);
                }
            }
        }
    }

    public void onVarbitChanged(VarbitChanged event) {
        int id = event.getVarbitId();
        AchievementDiaries.Diary diary = DIARIES.get(id);
        if (diary == null) return;
        if (!isEnabled()) return;

        int value = event.getValue();
        Integer previous = diaryCompletionById.put(id, value);
        if (previous != null && value > previous && checkDifficulty(diary)) {
            this.handle(diary);
        }
    }

    private void handle(AchievementDiaries.Diary diary) {
        int total = getTotalCompleted();
        String player = Utils.getPlayerName(plugin.getClient());
        String message = StringUtils.replaceEach(
            plugin.getConfig().diaryNotifyMessage(),
            new String[] { "%USERNAME%", "%DIFFICULTY%", "%AREA%", "%TOTAL%" },
            new String[] { player, diary.getDifficulty().toString(), diary.getArea(), String.valueOf(total) }
        );

        createMessage(DinkPluginConfig::diarySendImage, NotificationBody.builder()
            .type(NotificationType.ACHIEVEMENT_DIARY)
            .content(message)
            .extra(new DiaryNotificationData(diary.getArea(), diary.getDifficulty(), total))
            .playerName(player)
            .build());
    }

    private boolean checkDifficulty(AchievementDiaries.Diary diary) {
        return diary.getDifficulty().ordinal() >= plugin.getConfig().minDiaryDifficulty().ordinal();
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
}
