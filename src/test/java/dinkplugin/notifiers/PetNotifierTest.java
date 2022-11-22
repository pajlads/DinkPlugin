package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.DinkPluginConfig;
import dinkplugin.MockedTestBase;
import dinkplugin.message.DiscordMessageHandler;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
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

class PetNotifierTest extends MockedTestBase {

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
    PetNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getName()).thenReturn("dank");

        // init config mocks
        when(config.notifyPet()).thenReturn(true);
        when(config.petSendImage()).thenReturn(false);
        when(config.petNotifyMessage()).thenReturn("%USERNAME% got a pet");
    }

    @Test
    void testNotify() {
        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");

        // verify handled
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content("dank got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send non-pet message
        notifier.onChatMessage("You feel Forsen's warmth behind you.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

}
