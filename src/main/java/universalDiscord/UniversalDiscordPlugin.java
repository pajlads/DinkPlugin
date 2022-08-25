package universalDiscord;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.events.NotificationFired;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.OkHttpClient;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;


@Slf4j
@PluginDescriptor(
    name = "Universal Discord"
)
public class UniversalDiscordPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    public OkHttpClient httpClient;

    @Inject
    public DrawManager drawManager;
    @Inject
    public UniversalDiscordConfig config;

    @Inject
    public ItemManager itemManager;

    public final DiscordMessageHandler messageHandler = new DiscordMessageHandler(this);
    private final CollectionNotifier collectionNotifier = new CollectionNotifier(this);
    private final PetNotifier petNotifier = new PetNotifier(this);
    private final LevelNotifier levelNotifier = new LevelNotifier(this);
    private final LootNotifier lootNotifier = new LootNotifier(this);
    private final DeathNotifier deathNotifier = new DeathNotifier(this);
    private final SlayerNotifier slayerNotifier = new SlayerNotifier(this);
    private final QuestNotifier questNotifier = new QuestNotifier(this);
    private final ClueNotifier clueNotifier = new ClueNotifier(this);

    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (?<scrollCount>\\d+) (?<scrollType>\\w+) Treasure Trails\\.");
    private static final Pattern SLAYER_TASK_REGEX = Pattern.compile("You have completed your task! You killed (?<task>[\\d,]+ [\\w,]+)\\..*");
    private static final Pattern SLAYER_COMPLETE_REGEX = Pattern.compile("You've completed (?:at least )?(?<taskCount>[\\d,]+) (?:Wilderness )?tasks?(?: and received \\d+ points, giving you a total of (?<points>[\\d,]+)|\\.You'll be eligible to earn reward points if you complete tasks from a more advanced Slayer Master\\.| and reached the maximum amount of Slayer points \\((?<points2>[\\d,]+)\\))?");

    private static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>[\\w,\\s-.]+)");
    private static final Pattern PET_REGEX = Pattern.compile("You have a funny feeling like you.*");

    private String slayerTask = "";
    private String slayerTasksCompleted = "";
    private String slayerPoints = "";

    private boolean questCompleted = false;
    private boolean clueCompleted = false;
    private String clueCount = "";
    private String clueType = "";

    @Override
    protected void startUp() throws Exception {
        Utils.client = client;
        log.info("Started up Universal Discord");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Shutting down Universal Discord");
    }

    @Provides
    UniversalDiscordConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(UniversalDiscordConfig.class);
    }

    @Subscribe
    public void onNotificationFired(NotificationFired notif) {

    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        levelNotifier.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN)) {
            levelNotifier.reset();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.handleLevelUp(statChange.getSkill().getName(), statChange.getLevel());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        levelNotifier.onTick();
    }

    @Subscribe
    public void onChatMessage(ChatMessage message) {
        ChatMessageType msgType = message.getType();
        String chatMessage = Text.removeTags(message.getMessage());

        if (msgType.equals(ChatMessageType.GAMEMESSAGE)) {
            Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
            if(config.notifyCollectionLog() && collectionMatcher.find()) {
                collectionNotifier.handleNotify(collectionMatcher.group("itemName"));
                return;
            }

            if(config.notifyPet() && PET_REGEX.matcher(chatMessage).matches()) {
                petNotifier.handleNotify();
                return;
            }

            if(config.notifySlayer()
                && (chatMessage.contains("Slayer master")
                    || chatMessage.contains("Slayer Master")
                    || chatMessage.contains("completed your task!")
                )) {
                Matcher taskMatcher = SLAYER_TASK_REGEX.matcher(chatMessage);
                Matcher pointsMatcher = SLAYER_COMPLETE_REGEX.matcher(chatMessage);

                if(taskMatcher.find()) {
                    slayerTask = taskMatcher.group("task");
                    slayerNotifier.slayerTask = slayerTask;
                    slayerNotifier.handleNotify();
                }

                if(pointsMatcher.find()) {
                    slayerPoints = pointsMatcher.group("points");
                    slayerTasksCompleted = pointsMatcher.group("taskCount");

                    if(slayerPoints == null) {
                        slayerPoints = pointsMatcher.group("points2");
                    }

                    // 3 different cases of seeing points, so in our worst case it's 0
                    if(slayerPoints == null) {
                        slayerPoints = "0";
                    }
                    slayerNotifier.slayerPoints = slayerPoints;
                    slayerNotifier.slayerCompleted = slayerTasksCompleted;

                    slayerNotifier.handleNotify();
                }
            }

            if(config.notifyClue()) {
                Matcher clueMatcher = CLUE_SCROLL_REGEX.matcher(chatMessage);
                if(clueMatcher.find()) {
                    String numberCompleted = clueMatcher.group("scrollCount");
                    String scrollType = clueMatcher.group("scrollType");

                    if(clueCompleted) {
                        clueNotifier.handleNotify(numberCompleted, scrollType);
                        clueCompleted = false;
                    } else {
                        clueType = scrollType;
                        clueCount = numberCompleted;
                        clueCompleted = true;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actor) {
        if(config.notifyDeath() && Objects.equals(actor.getActor().getName(), Utils.getPlayerName())) {
            deathNotifier.handleNotify();
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        if(!config.notifyLoot()) {
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
        if (lootReceived.getType() != LootRecordType.EVENT && lootReceived.getType() != LootRecordType.PICKPOCKET) {
            return;
        }

        lootNotifier.handleNotify(lootReceived.getItems(), lootReceived.getName());
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        int groupId = event.getGroupId();

        if (groupId == QUEST_COMPLETED_GROUP_ID) {

            if(config.notifyQuest()) {
                Widget quest = client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT);

                if(quest != null) {
                    String questWidget = quest.getText();
                    questNotifier.handleNotify(questWidget);
                }
            }
        }

        if (groupId == WidgetID.CLUE_SCROLL_REWARD_GROUP_ID) {
            Widget clue = client.getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER);
            if(clue != null) {
                clueNotifier.clueItems.clear();
                Widget[] children = clue.getChildren();

                if(children == null) {
                    return;
                }

                for (Widget child : children) {
                    if(child == null) {
                        continue;
                    }

                    int quantity = child.getItemQuantity();
                    int itemId = child.getItemId();

                    if (itemId > -1 && quantity > 0) {
                        clueNotifier.clueItems.put(itemId, quantity);
                    }
                }

                if(clueCompleted) {
                    clueNotifier.handleNotify(clueCount, clueType);
                    clueCompleted = false;
                } else {
                    clueCompleted = true;
                }
            }
        }
    }
}
