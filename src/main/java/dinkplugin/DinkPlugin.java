package dinkplugin;

import com.google.inject.Provides;
import dinkplugin.notifiers.ChatNotifier;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import dinkplugin.notifiers.DeathNotifier;
import dinkplugin.notifiers.DiaryNotifier;
import dinkplugin.notifiers.GambleNotifier;
import dinkplugin.notifiers.GrandExchangeNotifier;
import dinkplugin.notifiers.GroupStorageNotifier;
import dinkplugin.notifiers.KillCountNotifier;
import dinkplugin.notifiers.LevelNotifier;
import dinkplugin.notifiers.LootNotifier;
import dinkplugin.notifiers.MetaNotifier;
import dinkplugin.notifiers.PetNotifier;
import dinkplugin.notifiers.PlayerKillNotifier;
import dinkplugin.notifiers.QuestNotifier;
import dinkplugin.notifiers.SlayerNotifier;
import dinkplugin.notifiers.SpeedrunNotifier;
import dinkplugin.notifiers.TradeNotifier;
import dinkplugin.util.KillCountService;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@PluginDescriptor(
    name = "Dink",
    description = "Discord-compatible webhook notifications for Loot, Death, Levels, CLog, KC, Diary, Quests, etc.",
    tags = { "loot", "logger", "collection", "pet", "death", "xp", "level", "notifications", "discord", "speedrun",
        "diary", "combat achievements", "combat task", "barbarian assault", "high level gambles" }
)
public class DinkPlugin extends Plugin {
    public static final String USER_AGENT = RuneLite.USER_AGENT + " (Dink/1.x)";

    private @Inject ChatMessageManager chatManager;

    private @Inject SettingsManager settingsManager;
    private @Inject VersionManager versionManager;

    private @Inject KillCountService killCountService;

    private @Inject CollectionNotifier collectionNotifier;
    private @Inject PetNotifier petNotifier;
    private @Inject LevelNotifier levelNotifier;
    private @Inject LootNotifier lootNotifier;
    private @Inject DeathNotifier deathNotifier;
    private @Inject SlayerNotifier slayerNotifier;
    private @Inject QuestNotifier questNotifier;
    private @Inject ClueNotifier clueNotifier;
    private @Inject SpeedrunNotifier speedrunNotifier;
    private @Inject KillCountNotifier killCountNotifier;
    private @Inject CombatTaskNotifier combatTaskNotifier;
    private @Inject DiaryNotifier diaryNotifier;
    private @Inject GambleNotifier gambleNotifier;
    private @Inject PlayerKillNotifier pkNotifier;
    private @Inject GroupStorageNotifier groupStorageNotifier;
    private @Inject GrandExchangeNotifier grandExchangeNotifier;
    private @Inject MetaNotifier metaNotifier;
    private @Inject TradeNotifier tradeNotifier;
    private @Inject ChatNotifier chatNotifier;

    private final AtomicReference<GameState> gameState = new AtomicReference<>();

    @Override
    protected void startUp() {
        log.debug("Started up Dink");
        settingsManager.init();
        versionManager.onStart();
        lootNotifier.init();
        deathNotifier.init();
        chatNotifier.init();
    }

    @Override
    protected void shutDown() {
        log.debug("Shutting down Dink");
        this.resetNotifiers();
        gameState.lazySet(null);
    }

    void resetNotifiers() {
        collectionNotifier.reset();
        petNotifier.reset();
        clueNotifier.reset();
        diaryNotifier.reset();
        levelNotifier.reset();
        deathNotifier.reset();
        slayerNotifier.reset();
        killCountNotifier.reset();
        groupStorageNotifier.reset();
        speedrunNotifier.reset();
        tradeNotifier.reset();
        chatNotifier.reset();
    }

    @Provides
    DinkPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DinkPluginConfig.class);
    }

    @Subscribe
    public void onAccountHashChanged(AccountHashChanged event) {
        grandExchangeNotifier.onAccountChange();
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event) {
        settingsManager.onCommand(event);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!SettingsManager.CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        settingsManager.onConfigChanged(event);
        lootNotifier.onConfigChanged(event.getKey(), event.getNewValue());
        deathNotifier.onConfigChanged(event.getKey(), event.getNewValue());
        chatNotifier.onConfig(event.getKey(), event.getNewValue());
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        levelNotifier.reset();
        killCountService.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState newState = gameStateChanged.getGameState();
        if (newState == GameState.LOADING) {
            // an intermediate state that is irrelevant for our notifiers; ignore
            return;
        }

        GameState previousState = gameState.getAndSet(newState);
        if (previousState == newState) {
            // no real change occurred (just momentarily went through LOADING); ignore
            return;
        }

        versionManager.onGameState(previousState, newState);
        settingsManager.onGameState(previousState, newState);
        collectionNotifier.onGameState(newState);
        levelNotifier.onGameStateChanged(gameStateChanged);
        diaryNotifier.onGameState(gameStateChanged);
        grandExchangeNotifier.onGameStateChange(gameStateChanged);
        metaNotifier.onGameState(previousState, newState);
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.onStatChanged(statChange);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        settingsManager.onTick();
        collectionNotifier.onTick();
        petNotifier.onTick();
        clueNotifier.onTick();
        slayerNotifier.onTick();
        levelNotifier.onTick();
        combatTaskNotifier.onTick();
        diaryNotifier.onTick();
        killCountNotifier.onTick();
        pkNotifier.onTick();
        grandExchangeNotifier.onTick();
        metaNotifier.onTick();
    }

    @Subscribe(priority = 1) // run before the base loot tracker plugin
    public void onChatMessage(ChatMessage message) {
        String chatMessage = Utils.sanitize(message.getMessage());
        String source = message.getName() != null && !message.getName().isEmpty() ? message.getName() : message.getSender();
        chatNotifier.onMessage(message.getType(), source, chatMessage);
        switch (message.getType()) {
            case GAMEMESSAGE:
                collectionNotifier.onChatMessage(chatMessage);
                petNotifier.onChatMessage(chatMessage);
                killCountService.onGameMessage(chatMessage);
                slayerNotifier.onChatMessage(chatMessage);
                clueNotifier.onChatMessage(chatMessage);
                killCountNotifier.onGameMessage(chatMessage);
                combatTaskNotifier.onGameMessage(chatMessage);
                deathNotifier.onGameMessage(chatMessage);
                speedrunNotifier.onGameMessage(chatMessage);
                break;

            case FRIENDSCHATNOTIFICATION:
                killCountNotifier.onFriendsChatNotification(chatMessage);
                // intentional fallthrough to clan notifications

            case CLAN_MESSAGE:
            case CLAN_GUEST_MESSAGE:
            case CLAN_GIM_MESSAGE:
                petNotifier.onClanNotification(chatMessage);
                break;

            case MESBOX:
                diaryNotifier.onMessageBox(chatMessage);
                gambleNotifier.onMesBoxNotification(chatMessage);
                break;

            case TRADE:
                tradeNotifier.onTradeMessage(chatMessage);
                break;

            default:
                // do nothing
                break;
        }
    }

    @Subscribe(priority = 1) // run before the base GE plugin
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event) {
        grandExchangeNotifier.onOfferChange(event.getSlot(), event.getOffer());
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        pkNotifier.onHitsplat(event);
    }

    @Subscribe
    public void onActorDeath(ActorDeath actor) {
        deathNotifier.onActorDeath(actor);
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        deathNotifier.onInteraction(event);
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        collectionNotifier.onScript(event.getScriptId());
        deathNotifier.onScript(event);
    }

    @Subscribe(priority = 1) // run before the base loot tracker plugin
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        killCountService.onNpcKill(npcLootReceived);
        lootNotifier.onNpcLootReceived(npcLootReceived);
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        killCountService.onPlayerKill(playerLootReceived);
        lootNotifier.onPlayerLootReceived(playerLootReceived);
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event) {
        versionManager.onProfileChange();
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        killCountService.onLoot(lootReceived);
        lootNotifier.onLootReceived(lootReceived);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        settingsManager.onVarbitChanged(event);
        collectionNotifier.onVarPlayer(event);
        diaryNotifier.onVarbitChanged(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        questNotifier.onWidgetLoaded(event);
        clueNotifier.onWidgetLoaded(event);
        speedrunNotifier.onWidgetLoaded(event);
        groupStorageNotifier.onWidgetLoad(event);
        killCountNotifier.onWidget(event);
        tradeNotifier.onWidgetLoad(event);
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        groupStorageNotifier.onWidgetClose(event);
        tradeNotifier.onWidgetClose(event);
    }

    public void addChatSuccess(String message) {
        addChatMessage("Success", Utils.GREEN, message);
    }

    public void addChatWarning(String message) {
        addChatMessage("Warning", Utils.RED, message);
    }

    void addChatMessage(String category, Color color, String message) {
        String formatted = String.format("[%s] %s: %s",
            ColorUtil.wrapWithColorTag(getName(), Utils.PINK),
            category,
            ColorUtil.wrapWithColorTag(message, color)
        );

        chatManager.queue(
            QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(formatted)
                .build()
        );
    }
}
