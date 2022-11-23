package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.DinkPluginConfig;
import dinkplugin.MockedTestBase;
import dinkplugin.message.DiscordMessageHandler;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import org.mockito.Mock;

import java.util.EnumSet;

import static org.mockito.Mockito.when;

class MockedNotifierTest extends MockedTestBase {

    protected static final String PLAYER_NAME = "dank";

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
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getName()).thenReturn(PLAYER_NAME);
    }

}
