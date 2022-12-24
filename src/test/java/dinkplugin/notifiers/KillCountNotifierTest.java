package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillCountNotifierTest extends MockedNotifierTest {

    @InjectMocks
    KillCountNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyKillCount()).thenReturn(true);
        when(config.killCountNotifyBestTime()).thenReturn(true);
        when(config.killCountSendImage()).thenReturn(true);
        when(config.killCountMessage()).thenReturn("%USERNAME% has defeated %BOSS% with a completion count of %COUNT%");
        when(config.killCountBestTimeMessage()).thenReturn("%USERNAME% has defeated %BOSS% with a new personal best time of %TIME% and a completion count of %COUNT%");
    }

    @Test
    void testNotifyInterval() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(70);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 420.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .content(PLAYER_NAME + " has defeated King Black Dragon with a completion count of 420")
                .extra(new BossNotificationData("King Black Dragon", 420, gameMessage, null, null))
                .playerName(PLAYER_NAME)
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
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .content(PLAYER_NAME + " has defeated King Black Dragon with a completion count of 1")
                .extra(new BossNotificationData("King Black Dragon", 1, gameMessage, null, null))
                .playerName(PLAYER_NAME)
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
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // more config
        when(config.notifyKillCount()).thenReturn(false);
        when(config.killCountNotifyInitial()).thenReturn(true);
        when(config.killCountInterval()).thenReturn(1);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:56.50 (new personal best).");
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .content(PLAYER_NAME + " has defeated Zulrah with a new personal best time of 00:00:56.500 and a completion count of 12")
                .extra(new BossNotificationData("Zulrah", 12, gameMessage, Duration.ofSeconds(56).plusMillis(500), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(6);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:59.30. Personal best: 0:56.50");
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .content(PLAYER_NAME + " has defeated Zulrah with a completion count of 12")
                .extra(new BossNotificationData("Zulrah", 12, gameMessage, Duration.ofSeconds(59).plusMillis(300), false))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testIgnoreNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:59.30. Personal best: 0:56.50");
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
