package dinkplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.AmascutPurpleNotificationData;
import dinkplugin.notifiers.data.LoginNotificationData;
import dinkplugin.notifiers.data.Progress;
import dinkplugin.util.ConfigUtil;
import dinkplugin.util.SerializedPet;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
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

    private static final int[] TOA_CHEST_VARBS;

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
        // check if the oldState is any that can be considered "in game", and if the new state is "LOGIN_SCREEN"
        if ((oldState == GameState.LOGGED_IN || oldState == GameState.CONNECTION_LOST || oldState == GameState.HOPPING)
            && newState == GameState.LOGIN_SCREEN && isEnabled()) {
            notifyLogout();
        }
    }

    public void onTick() {
        if (loginTicks.getAndUpdate(i -> Math.max(-1, i - 1)) == 0 && isEnabled()) {
            clientThread.invokeLater(this::notifyLogin); // just 20ms later to be able to run client scripts cleanly
        }
    }

    public void onVarbit(VarbitChanged event) {
        if (event.getVarbitId() == VarbitID.TOA_VAULT_SARCOPHAGUS && event.getValue() % 2 == 1) {
            clientThread.invokeAtTickEnd(this::notifyPurpleAmascut);
        }
    }

    private void notifyPurpleAmascut() {
        // inspect multiloc to ensure local player is the recipient of the purple drop (s/o @rdutta)
        for (@Varbit int varbitId : TOA_CHEST_VARBS) {
            if (client.getVarbitValue(varbitId) == 2) {
                // someone else in the party received the purple drop
                return;
            }
        }

        // Gather relevant data
        var party = Utils.getAmascutTombsParty(client);
        int rewardPoints = client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSCORE);
        int raidLevels = client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL);

        // Calculate probability based on https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Uniques
        int x = Math.min(raidLevels, 400);
        int y = Math.max(Math.min(raidLevels, 550) - 400, 0);
        int partySize = Math.max(party.size(), 1);
        double probability = Math.min(0.01 * rewardPoints / (10_500 - 20 * (x + y / 3.0)), 0.55) / partySize;

        // Fire notification
        String playerName = Utils.getPlayerName(client);
        Template message = Template.builder()
            .replacementBoundary("%")
            .template("%USERNAME% rolled a purple (unique) drop from Tombs of Amascut!")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .build();
        var extra = new AmascutPurpleNotificationData(party, rewardPoints, raidLevels, probability);
        createMessage(false, NotificationBody.builder()
            .type(NotificationType.TOA_UNIQUE)
            .text(message)
            .extra(extra)
            .playerName(playerName)
            .build()
        );
    }

    private void notifyLogin() {
        // Gather data points
        int world = client.getWorld();

        int collectionCompleted = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
        int collectionTotal = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);

        int combatAchievementPoints = client.getVarbitValue(VarbitID.CA_POINTS);
        int combatAchievementPointsTotal = client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER);

        int diaryCompleted = AchievementDiary.DIARIES.keySet()
            .stream()
            .mapToInt(id -> DiaryNotifier.isComplete(id, client.getVarbitValue(id)) ? 1 : 0)
            .sum();
        int diaryTotal = AchievementDiary.DIARIES.size();
        client.runScript(DiaryNotifier.COMPLETED_TASKS_SCRIPT_ID);
        int diaryTaskCompleted = client.getIntStack()[0];
        client.runScript(DiaryNotifier.TOTAL_TASKS_SCRIPT_ID);
        int diaryTaskTotal = client.getIntStack()[0];

        int gambleCount = client.getVarbitValue(VarbitID.BARBASSAULT_GAMBLECOUNT);

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

        int questsCompleted = client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT);
        int questsTotal = client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT);
        int questPoints = client.getVarpValue(VarPlayerID.QP);
        int questPointsTotal = client.getVarbitValue(VarbitID.QP_MAX);

        int slayerPoints = client.getVarbitValue(VarbitID.SLAYER_POINTS);
        int slayerStreak = client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED);

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

    private void notifyLogout() {
        String playerName = Utils.getPlayerName(client);
        Template message = Template.builder()
            .replacementBoundary("%")
            .template("%USERNAME% logged out")
            .replacement("%USERNAME%", Replacements.ofText(playerName))
            .build();

        createMessage(false, NotificationBody.builder()
            .type(NotificationType.LOGOUT)
            .text(message)
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

    static {
        TOA_CHEST_VARBS = new int[] {
            VarbitID.TOA_VAULT_CHEST_0, VarbitID.TOA_VAULT_CHEST_1, VarbitID.TOA_VAULT_CHEST_2, VarbitID.TOA_VAULT_CHEST_3,
            VarbitID.TOA_VAULT_CHEST_4, VarbitID.TOA_VAULT_CHEST_5, VarbitID.TOA_VAULT_CHEST_6, VarbitID.TOA_VAULT_CHEST_7
        };
    }
}
