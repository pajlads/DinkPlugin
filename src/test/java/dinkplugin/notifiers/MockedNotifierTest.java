package dinkplugin.notifiers;

import com.google.gson.Gson;
import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.DinkPluginConfig;
import dinkplugin.MockedTestBase;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.util.TestImageUtil;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.vars.AccountType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.DrawManager;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.OkHttpClient;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.awt.Image;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class MockedNotifierTest extends MockedTestBase {

    protected static final String PLAYER_NAME = "dank";
    protected static final String PRIMARY_WEBHOOK_URL = System.getenv("TEST_WEBHOOK_URL");

    @Bind
    protected DinkPluginConfig config = Mockito.mock(DinkPluginConfig.class);

    @Bind
    protected Client client = Mockito.mock(Client.class);

    @Mock
    protected Player localPlayer;

    @Bind
    protected DrawManager drawManager = Mockito.mock(DrawManager.class);

    @Bind
    protected Gson gson = RuneLiteAPI.GSON;

    @Bind
    protected OkHttpClient httpClient = new OkHttpClient();

    @Bind
    protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Bind
    protected DiscordMessageHandler messageHandler = Mockito.spy(new DiscordMessageHandler(gson, client, drawManager, httpClient, config, executor));

    @Override
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getAccountType()).thenReturn(AccountType.GROUP_IRONMAN);
        when(client.isPrayerActive(any())).thenReturn(false);
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getName()).thenReturn(PLAYER_NAME);

        doAnswer(invocation -> {
            Consumer<Image> callback = invocation.getArgument(0);
            callback.accept(TestImageUtil.getExample());
            return null;
        }).when(drawManager).requestNextFrameListener(any());

        // init config mocks
        when(config.primaryWebhook()).thenReturn(PRIMARY_WEBHOOK_URL);
        when(config.maxRetries()).thenReturn(0);
        when(config.baseRetryDelay()).thenReturn(2000);
        when(config.imageWriteTimeout()).thenReturn(30_000);

        // make okhttp send message blocking
        when(messageHandler.isAsync()).thenReturn(false);
    }

    protected void mockItem(ItemManager manager, int id, int price, String name) {
        when(manager.getItemPrice(id)).thenReturn(price);
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        when(manager.getItemComposition(id)).thenReturn(item);
    }

}
