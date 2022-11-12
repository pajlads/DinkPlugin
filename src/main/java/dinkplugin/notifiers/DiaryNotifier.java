package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.domain.AchievementDiaries;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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
        if (previous != null && value > previous) {
            this.handle(diary);
        }
    }

    private void handle(AchievementDiaries.Diary diary) {
        log.debug("Diary completion detected: {}", diary);
    }
}
