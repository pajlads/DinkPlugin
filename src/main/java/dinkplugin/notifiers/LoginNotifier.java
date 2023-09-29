package dinkplugin.notifiers;

import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LoginNotificationData;
import dinkplugin.notifiers.data.Progress;
import dinkplugin.util.Utils;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LoginNotifier extends BaseNotifier {
    private static final int INIT_TICKS = 10; // 6 seconds after login
    private final AtomicInteger ticks = new AtomicInteger(-1);

    @Override
    public boolean isEnabled() {
        return config.notifyLogin() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return null;
    }

    public void onGameState(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            ticks.set(INIT_TICKS);
        }
    }

    public void onTick() {
        if (ticks.getAndUpdate(i -> Math.max(-1, i - 1)) == 0 && isEnabled()) {
            this.handleNotify();
        }
    }

    private void handleNotify() {
        // Gather data points
        int world = client.getWorld();

        int collectionCompleted = client.getVarpValue(CollectionNotifier.COMPLETED_VARP);
        int collectionTotal = client.getVarpValue(CollectionNotifier.TOTAL_VARP);

        int combatAchievementPoints = client.getVarbitValue(CombatTaskNotifier.TOTAL_POINTS_ID);
        int combatAchievementPointsTotal = client.getVarbitValue(CombatTaskNotifier.GRANDMASTER_TOTAL_POINTS_ID);

        int diaryCompleted = AchievementDiary.DIARIES.keySet()
            .stream()
            .mapToInt(id -> DiaryNotifier.isComplete(id, client.getVarbitValue(id)) ? 1 : 0)
            .sum();
        int diaryTotal = AchievementDiary.DIARIES.size();

        int gambleCount = client.getVarbitValue(Varbits.BA_GC);

        long experienceTotal = client.getOverallExperience();
        int levelTotal = client.getTotalLevel();
        Map<String, Integer> skillLevels = Arrays.stream(Skill.values()).collect(Collectors.toMap(Skill::getName, skill -> {
            int lvl = client.getRealSkillLevel(skill);
            return lvl < 99 ? lvl : Experience.getLevelForXp(client.getSkillExperience(skill));
        }));

        int questsCompleted = client.getVarbitValue(QuestNotifier.COMPLETED_ID);
        int questsTotal = client.getVarbitValue(QuestNotifier.TOTAL_ID);
        int questPoints = client.getVarpValue(VarPlayer.QUEST_POINTS);
        int questPointsTotal = client.getVarbitValue(QuestNotifier.QP_TOTAL_ID);

        int slayerPoints = client.getVarbitValue(Varbits.SLAYER_POINTS);
        int slayerStreak = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

        // Fire notification
        String playerName = Utils.getPlayerName(client);
        Template message = Template.builder()
            .replacementBoundary("%")
            .template("%USERNAME% logged into World %WORLD%")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .replacement("%WORLD%", Replacements.ofText(String.valueOf(world)))
            .build();
        LoginNotificationData extra = new LoginNotificationData(
            world,
            Progress.of(collectionCompleted, collectionTotal),
            Progress.of(combatAchievementPoints, combatAchievementPointsTotal),
            Progress.of(diaryCompleted, diaryTotal),
            new LoginNotificationData.BarbarianAssault(gambleCount),
            new LoginNotificationData.SkillData(experienceTotal, levelTotal, skillLevels),
            Progress.of(questsCompleted, questsTotal),
            Progress.of(questPoints, questPointsTotal),
            new LoginNotificationData.SlayerData(slayerPoints, slayerStreak)
        );
        createMessage(false, NotificationBody.builder()
            .type(NotificationType.LOGIN)
            .text(message)
            .extra(extra)
            .playerName(playerName)
            .build()
        );
    }

}
