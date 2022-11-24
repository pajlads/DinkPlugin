package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.DeathNotificationData;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeathNotifierTest extends MockedNotifierTest {

    @InjectMocks
    DeathNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyDeath()).thenReturn(true);
        when(config.deathNotifPvpEnabled()).thenReturn(true);
        when(config.deathSendImage()).thenReturn(false);
        when(config.deathNotifyMessage()).thenReturn("%USERNAME% has died, losing %VALUELOST% gp");
        when(config.deathNotifPvpMessage()).thenReturn("%USERNAME% has just been PKed by %PKER% for %VALUELOST% gp...");

        // init client mocks
        when(client.getVarbitValue(Varbits.IN_WILDERNESS)).thenReturn(1);
        when(client.getPlayers()).thenReturn(Collections.emptyList());
        WorldPoint location = new WorldPoint(0, 0, 0);
        when(localPlayer.getWorldLocation()).thenReturn(location);
    }

    @Test
    void testNotifyEmpty() {
        // fire event
        notifier.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has died, losing %d gp", PLAYER_NAME, 0))
                .extra(new DeathNotificationData(0L, false, null, Collections.emptyList(), Collections.emptyList()))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // prepare mock
        Player other = mock(Player.class);

        // fire event
        notifier.onActorDeath(new ActorDeath(other));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyDeath()).thenReturn(false);

        // fire event
        notifier.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

}
