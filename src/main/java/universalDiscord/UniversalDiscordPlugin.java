package universalDiscord;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
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
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.OkHttpClient;

import java.util.Collection;
import java.util.regex.Pattern;


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

    private static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log:.*");
    private static final Pattern PET_REGEX = Pattern.compile("You have a funny feeling like you.*");

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
        String chatMessage = message.getMessage().replaceAll("<.*?>", "");

        if (msgType.equals(ChatMessageType.GAMEMESSAGE)) {
            if(config.notifyCollectionLog() && COLLECTION_LOG_REGEX.matcher(chatMessage).matches()) {
                collectionNotifier.handleNotify(message.getMessage());
                return;
            }

            if(config.notifyPet() && PET_REGEX.matcher(chatMessage).matches()) {
                petNotifier.handleNotify(message.getMessage());
                return;
            }
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        NPC npc = npcLootReceived.getNpc();
        Collection<ItemStack> items = npcLootReceived.getItems();

        lootNotifier.handleLootDrop(items, npc.getName());
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        Collection<ItemStack> items = playerLootReceived.getItems();
        lootNotifier.handleLootDrop(items, playerLootReceived.getPlayer().getName());
    }

    @Subscribe
    public void onLootReceived(LootReceived lootReceived) {
        if (lootReceived.getType() != LootRecordType.EVENT && lootReceived.getType() != LootRecordType.PICKPOCKET) {
            return;
        }

        lootNotifier.handleLootDrop(lootReceived.getItems(), lootReceived.getName());
    }
}
