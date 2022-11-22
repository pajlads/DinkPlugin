package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.DinkPluginConfig;
import dinkplugin.MockedTestBase;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.EnumSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillCountNotifierTest extends MockedTestBase {

    @Bind
    @Mock
    DinkPluginConfig config;

    @Bind
    @Mock
    Client client;

    @Mock
    Player localPlayer;

    @Bind
    @Mock
    DiscordMessageHandler messageHandler;

    @InjectMocks
    KillCountNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getName()).thenReturn("dank");

        // init config mocks
        when(config.notifyKillCount()).thenReturn(true);
        when(config.killCountSendImage()).thenReturn(true);
        when(config.killCountMessage()).thenReturn("%USERNAME% has defeated %BOSS% with a completion count of %COUNT%");
    }

    @Test
    void testNotifyInterval() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(70);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 420.";
        notifier.onGameMessage(gameMessage);

        // check notification
        verify(messageHandler).createMessage(
            true,
            NotificationBody.builder()
                .content("dank has defeated King Black Dragon with a completion count of 420")
                .extra(new BossNotificationData("King Black Dragon", 420, gameMessage))
                .playerName("dank")
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyInitial() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(true);
        when(config.killCountInterval()).thenReturn(99);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);

        // check notification
        verify(messageHandler).createMessage(
            true,
            NotificationBody.builder()
                .content("dank has defeated King Black Dragon with a completion count of 1")
                .extra(new BossNotificationData("King Black Dragon", 1, gameMessage))
                .playerName("dank")
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(99);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);

        // ensure no message
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

}
