package dinkplugin;

import com.google.inject.Provides;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.notifiers.ClueNotifier;
import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.DeathNotifier;
import dinkplugin.notifiers.KillCountNotifier;
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
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.UsernameChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "Dink",
    description = "A notifier for sending webhooks to Discord or other custom destinations",
    tags = { "loot", "logger", "collection", "pet", "death", "xp", "level", "notifications", "discord", "speedrun" }
)
public class DinkPlugin extends Plugin {
    @Inject
    @Getter
    private Client client;
    @Inject
    @Getter
    private OkHttpClient httpClient;
    @Inject
    @Getter
    private DinkPluginConfig config;
    @Inject
    @Getter
    private DrawManager drawManager;
    @Inject
    @Getter
    private ItemManager itemManager;

    @Getter
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
    private final KillCountNotifier killCountNotifier = new KillCountNotifier(this);

    @Override
    protected void startUp() {
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
        levelNotifier.onGameStateChanged(gameStateChanged);
    }

    @Subscribe
    public void onStatChanged(StatChanged statChange) {
        levelNotifier.onStatChanged(statChange);
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
            collectionNotifier.onChatMessage(chatMessage);
            petNotifier.onChatMessage(chatMessage);
            slayerNotifier.onChatMessage(chatMessage);
            clueNotifier.onChatMessage(chatMessage);
            killCountNotifier.onGameMessage(chatMessage);
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actor) {
        deathNotifier.onActorDeath(actor);
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
    public void onWidgetLoaded(WidgetLoaded event) {
        questNotifier.onWidgetLoaded(event);
        clueNotifier.onWidgetLoaded(event);
        speedrunNotifier.onWidgetLoaded(event);
    }
}
