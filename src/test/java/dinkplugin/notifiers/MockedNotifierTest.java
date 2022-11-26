package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.DinkPluginConfig;
import dinkplugin.MockedTestBase;
import dinkplugin.message.DiscordMessageHandler;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.vars.AccountType;
import net.runelite.client.game.ItemManager;
import org.mockito.Mock;

import java.util.EnumSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockedNotifierTest extends MockedTestBase {

    protected static final String PLAYER_NAME = "dank";
    protected static final String PRIMARY_WEBHOOK_URL = "";

    @Bind
    @Mock
    protected DinkPluginConfig config;

    @Bind
    @Mock
    protected Client client;

    @Mock
    protected Player localPlayer;

    @Bind
    @Mock
    protected DiscordMessageHandler messageHandler;

    @Override
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getAccountType()).thenReturn(AccountType.NORMAL);
        when(client.isPrayerActive(any())).thenReturn(false);
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getName()).thenReturn(PLAYER_NAME);
        when(config.primaryWebhook()).thenReturn(PRIMARY_WEBHOOK_URL);
    }

    protected void mockItem(ItemManager manager, int id, int price, String name) {
        when(manager.getItemPrice(id)).thenReturn(price);
        ItemComposition item = mock(ItemComposition.class);
        when(item.getName()).thenReturn(name);
        when(manager.getItemComposition(id)).thenReturn(item);
    }

}
