package dinkplugin;

import com.google.inject.Provides;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.DeathNotifier;
import dinkplugin.notifiers.LevelNotifier;
import dinkplugin.notifiers.LootNotifier;
import dinkplugin.notifiers.PetNotifier;
import dinkplugin.notifiers.QuestNotifier;
import dinkplugin.notifiers.SlayerNotifier;
import dinkplugin.notifiers.SpeedrunNotifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;

@Slf4j
@Getter
@PluginDescriptor(
    name = "Dink",
    description = "A notifier for sending webhooks to Discord or other custom destinations",
    tags = {"loot","logger","collection","pet","death","xp","level","notifications","discord","speedrun"}
)
public class DinkPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private OkHttpClient httpClient;

    @Inject
    private DrawManager drawManager;
    @Inject
    private DinkPluginConfig config;

    @Inject
    private ItemManager itemManager;

    private final DiscordMessageHandler messageHandler = new DiscordMessageHandler(this);
    private final CollectionNotifier collectionNotifier = new CollectionNotifier(this);
    private final PetNotifier petNotifier = new PetNotifier(this);
    private final LevelNotifier levelNotifier = new LevelNotifier(this);
    private final LootNotifier lootNotifier = new LootNotifier(this);
    private final DeathNotifier deathNotifier = new DeathNotifier(this);
    private final SlayerNotifier slayerNotifier = new SlayerNotifier(this);
    private final QuestNotifier questNotifier = new QuestNotifier(this);
    private final ClueNotifier clueNotifier = new ClueNotifier(this);
    private final SpeedrunNotifier speedrunNotifier = new SpeedrunNotifier(this);

    public static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>(.*))");

    @Inject
    private WorldService worldService;

    @Override
    protected void startUp() {
        Utils.setClient(client);
        log.info("Started up Dink");
    }

    @Override
    protected void shutDown() {
        log.info("Shutting down Dink");
    }

    @Provides
    DinkPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DinkPluginConfig.class);
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        levelNotifier.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            levelNotifier.reset();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.handleLevelUp(statChange.getSkill().getName(), statChange.getLevel(), statChange.getXp());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        levelNotifier.onTick();
    }

    @Subscribe
    public void onChatMessage(ChatMessage message) {
        ChatMessageType msgType = message.getType();
        String chatMessage = Text.removeTags(message.getMessage());

        if (msgType == ChatMessageType.GAMEMESSAGE) {
            Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
            if (config.notifyCollectionLog() && collectionMatcher.find()) {
                collectionNotifier.handleNotify(collectionMatcher.group("itemName"));
                return;
            }

            petNotifier.onChatMessage(chatMessage);
            slayerNotifier.onChatMessage(chatMessage);
            clueNotifier.onChatMessage(chatMessage);
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actor) {
        if (config.notifyDeath() && client.getLocalPlayer() == actor.getActor()) {
            deathNotifier.handleNotify();
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        if (!config.notifyLoot()) {
            return;
        }

        NPC npc = npcLootReceived.getNpc();
        Collection<ItemStack> items = npcLootReceived.getItems();

        lootNotifier.handleNotify(items, npc.getName());
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        Collection<ItemStack> items = playerLootReceived.getItems();
        lootNotifier.handleNotify(items, playerLootReceived.getPlayer().getName());
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        if (!config.notifyLoot()) return;

        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            lootNotifier.handleNotify(lootReceived.getItems(), lootReceived.getName());
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        int groupId = event.getGroupId();

        if (groupId == QUEST_COMPLETED_GROUP_ID) {
            if (config.notifyQuest()) {
                Widget quest = client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT);

                if (quest != null) {
                    String questWidget = quest.getText();
                    questNotifier.handleNotify(questWidget);
                }
            }
        }

        clueNotifier.onWidgetLoaded(event);

        final int SPEEDRUN_COMPLETED_GROUP_ID = 781;
        final int SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID = 4;
        final int SPEEDRUN_COMPLETED_DURATION_CHILD_ID = 10;
        final int SPEEDRUN_COMPLETED_PB_CHILD_ID = 12;
        if (config.notifySpeedrun() && groupId == SPEEDRUN_COMPLETED_GROUP_ID) {
            Widget questName = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID);
            Widget duration = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID);
            Widget personalBest = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_PB_CHILD_ID);
            if (questName == null || duration == null || personalBest == null) {
                log.error("Found speedrun finished widget (group id {}) but it is missing something, questName={}, duration={}, pb={}", SPEEDRUN_COMPLETED_GROUP_ID, questName, duration, personalBest);
            } else {
                this.speedrunNotifier.attemptNotify(Utils.parseQuestWidget(questName.getText()), duration.getText(), personalBest.getText());
            }
        }
    }

    public boolean isIgnoredWorld() {
        return Utils.isIgnoredWorld(client.getWorldType());
    }
}
