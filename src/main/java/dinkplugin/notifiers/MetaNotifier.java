package dinkplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LoginNotificationData;
import dinkplugin.notifiers.data.Progress;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.SerializedPet;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.chatcommands.ChatCommandsPlugin;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
public class MetaNotifier extends BaseNotifier {
    static final @VisibleForTesting String RL_CHAT_CMD_PLUGIN_NAME = ChatCommandsPlugin.class.getSimpleName().toLowerCase();
    static final @VisibleForTesting int INIT_TICKS = 10; // 6 seconds after login

    private final AtomicInteger loginTicks = new AtomicInteger(-1);

    @Inject
    private ClientThread clientThread;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Override
    public boolean isEnabled() {
        return StringUtils.isNotBlank(config.metadataWebhook()) && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.metadataWebhook();
    }

    public void onGameState(GameState oldState, GameState newState) {
        // inspect oldState because we don't want a notification on each world hop
        if (oldState == GameState.LOGGING_IN && newState == GameState.LOGGED_IN) {
            loginTicks.set(INIT_TICKS);
        }
    }

    public void onTick() {
        if (loginTicks.getAndUpdate(i -> Math.max(-1, i - 1)) == 0 && isEnabled()) {
            clientThread.invokeLater(this::notifyLogin); // just 20ms later to be able to run client scripts cleanly
        }
    }

    private void notifyLogin() {
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
        client.runScript(DiaryNotifier.COMPLETED_TASKS_SCRIPT_ID);
        int diaryTaskCompleted = client.getIntStack()[0];
        client.runScript(DiaryNotifier.TOTAL_TASKS_SCRIPT_ID);
        int diaryTaskTotal = client.getIntStack()[0];

        int gambleCount = client.getVarbitValue(Varbits.BA_GC);

        long experienceTotal = client.getOverallExperience();
        int levelTotal = client.getTotalLevel();
        Map<String, Integer> skillLevels = new HashMap<>(32);
        Map<String, Integer> skillExperience = new HashMap<>(32);
        for (Skill skill : Skill.values()) {
            int xp = client.getSkillExperience(skill);
            int lvl = client.getRealSkillLevel(skill);
            int virtualLevel = lvl < 99 ? lvl : Experience.getLevelForXp(xp);
            skillExperience.put(skill.getName(), xp);
            skillLevels.put(skill.getName(), virtualLevel);
        }

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
            Progress.of(diaryTaskCompleted, diaryTaskTotal),
            new LoginNotificationData.BarbarianAssault(gambleCount),
            new LoginNotificationData.SkillData(experienceTotal, levelTotal, skillLevels, skillExperience),
            Progress.of(questsCompleted, questsTotal),
            Progress.of(questPoints, questPointsTotal),
            new LoginNotificationData.SlayerData(slayerPoints, slayerStreak),
            getPets()
        );
        createMessage(false, NotificationBody.builder()
            .type(NotificationType.LOGIN)
            .text(message)
            .extra(extra)
            .playerName(playerName)
            .build()
        );
    }

    @VisibleForTesting
    List<SerializedPet> getPets() {
        if (ConfigUtil.isPluginDisabled(configManager, RL_CHAT_CMD_PLUGIN_NAME))
            return null;

        String json = configManager.getRSProfileConfiguration("chatcommands", "pets2");
        if (json == null || json.isEmpty())
            return null;

        int[] petItemIds;
        try {
            petItemIds = gson.fromJson(json, int[].class);
        } catch (JsonSyntaxException e) {
            log.info("Failed to deserialize owned pet IDs", e);
            return null;
        }

        List<SerializedPet> pets = new ArrayList<>(petItemIds.length);
        for (int itemId : petItemIds) {
            pets.add(new SerializedPet(itemId, client.getItemDefinition(itemId).getMembersName()));
        }
        return pets;
    }

}
