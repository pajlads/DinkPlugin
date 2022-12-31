package dinkplugin;

import com.google.inject.Provides;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import dinkplugin.notifiers.DeathNotifier;
import dinkplugin.notifiers.DiaryNotifier;
import dinkplugin.notifiers.KillCountNotifier;
import dinkplugin.notifiers.LevelNotifier;
import dinkplugin.notifiers.LootNotifier;
import dinkplugin.notifiers.PetNotifier;
import dinkplugin.notifiers.QuestNotifier;
import dinkplugin.notifiers.SlayerNotifier;
import dinkplugin.notifiers.SpeedrunNotifier;
import dinkplugin.util.Utils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "Dink",
    description = "A notifier for sending webhooks to Discord or other custom destinations",
    tags = { "loot", "logger", "collection", "pet", "death", "xp", "level", "notifications", "discord", "speedrun",
        "diary", "combat achievements", "combat task" }
)
public class DinkPlugin extends Plugin {
    public static final String USER_AGENT = RuneLite.USER_AGENT + " (Dink/1.x)";

    private @Inject ChatMessageManager chatManager;

    private @Inject SettingsValidator validator;

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

    @Override
    protected void startUp() {
        log.info("Started up Dink");
        levelNotifier.initLevels();
    }

    @Override
    protected void shutDown() {
        log.info("Shutting down Dink");
        clueNotifier.reset();
        diaryNotifier.reset();
        levelNotifier.reset();
        slayerNotifier.reset();
        killCountNotifier.reset();
    }

    @Provides
    DinkPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DinkPluginConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        validator.onConfigChanged(event);
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        levelNotifier.reset();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        levelNotifier.onGameStateChanged(gameStateChanged);
        diaryNotifier.onGameState(gameStateChanged);
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.onStatChanged(statChange);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        clueNotifier.onTick();
        slayerNotifier.onTick();
        levelNotifier.onTick();
        diaryNotifier.onTick();
        killCountNotifier.onTick();
    }

    @Subscribe
    public void onChatMessage(ChatMessage message) {
        ChatMessageType msgType = message.getType();
        String chatMessage = Text.removeTags(message.getMessage().replace("<br>", "\n")).replace('\u00A0', ' ').trim();

        if (msgType == ChatMessageType.GAMEMESSAGE) {
            collectionNotifier.onChatMessage(chatMessage);
            petNotifier.onChatMessage(chatMessage);
            slayerNotifier.onChatMessage(chatMessage);
            clueNotifier.onChatMessage(chatMessage);
            killCountNotifier.onGameMessage(chatMessage);
            combatTaskNotifier.onGameMessage(chatMessage);
        }

        if (msgType == ChatMessageType.FRIENDSCHATNOTIFICATION) {
            killCountNotifier.onFriendsChatNotification(chatMessage);
        }
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
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        lootNotifier.onNpcLootReceived(npcLootReceived);
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        lootNotifier.onPlayerLootReceived(playerLootReceived);
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        lootNotifier.onLootReceived(lootReceived);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        validator.onVarbitChanged(event);
        diaryNotifier.onVarbitChanged(event);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        questNotifier.onWidgetLoaded(event);
        clueNotifier.onWidgetLoaded(event);
        speedrunNotifier.onWidgetLoaded(event);
    }

    public void addChatWarning(String message) {
        String formatted = String.format("[%s] %s: %s",
            ColorUtil.wrapWithColorTag(getName(), Utils.PINK),
            "Warning",
            ColorUtil.wrapWithColorTag(message, Utils.RED)
        );

        chatManager.queue(
            QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(formatted)
                .build()
        );
    }
}
