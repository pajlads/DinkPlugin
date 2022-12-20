package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.SlayerNotificationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlayerNotifierTest extends MockedNotifierTest {

    @InjectMocks
    SlayerNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifySlayer()).thenReturn(true);
        when(config.slayerSendImage()).thenReturn(false);
        when(config.slayerPointThreshold()).thenReturn(1);
        when(config.slayerNotifyMessage()).thenReturn("%USERNAME% has completed: %TASK%, getting %POINTS% points for a total %TASKCOUNT% tasks completed");
    }

    @Test
    void testNotify() {
        // fire chat messages
        notifier.onChatMessage("You have completed your task! You killed 1 TzTok-Jad. You gained 69,420 xp.");
        notifier.onChatMessage("You've completed 100 tasks and received 10 points, giving you a total of 200; return to a Slayer master.");

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed: %s, getting %d points for a total %d tasks completed", PLAYER_NAME, "1 TzTok-Jad", 10, 100))
                .extra(new SlayerNotificationData("1 TzTok-Jad", "100", "10"))
                .type(NotificationType.SLAYER)
                .build()
        );
    }

    @Test
    void testNotifyBoss() {
        // fire chat messages
        notifier.onChatMessage("You are granted an extra reward of 5k Slayer XP for completing your boss task against the Cave Kraken boss.");
        notifier.onChatMessage("You have completed your task! You killed 50 Bosses. You gained 14,200 xp.");
        notifier.onChatMessage("You've completed 150 tasks and received 10 points, giving you a total of 200; return to a Slayer master.");

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed: %s, getting %d points for a total %d tasks completed", PLAYER_NAME, "50 Cave Kraken", 10, 150))
                .extra(new SlayerNotificationData("50 Cave Kraken", "150", "10"))
                .type(NotificationType.SLAYER)
                .build()
        );
    }

    @Test
    void testNotifyBarrows() {
        // fire chat messages
        notifier.onChatMessage("You are granted an extra reward of 5k Slayer XP for completing your boss task against Barrows brothers.");
        notifier.onChatMessage("You have completed your task! You killed 36 Bosses. You gained 8,943 xp.");
        notifier.onChatMessage("You've completed 881 tasks and received 20 points, giving you a total of 689; return to a Slayer master.");

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed: %s, getting %d points for a total %d tasks completed", PLAYER_NAME, "36 Barrows brothers", 20, 881))
                .extra(new SlayerNotificationData("36 Barrows brothers", "881", "20"))
                .type(NotificationType.SLAYER)
                .build()
        );
    }

    @Test
    void testNotifyChaos() {
        // fire chat messages
        notifier.onChatMessage("You are granted an extra reward of 5k Slayer XP for completing your boss task against the Chaos Elemental.");
        notifier.onChatMessage("You have completed your task! You killed 11 Bosses. You gained 7,954 xp.");
        notifier.onChatMessage("You've completed 242 tasks and received 15 points, giving you a total of 3,254; return to a Slayer master.");

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed: %s, getting %d points for a total %d tasks completed", PLAYER_NAME, "11 Chaos Elemental", 15, 242))
                .extra(new SlayerNotificationData("11 Chaos Elemental", "242", "15"))
                .type(NotificationType.SLAYER)
                .build()
        );
    }

    @Test
    void testIgnorePoints() {
        // fire chat messages
        notifier.onChatMessage("You have completed your task! You killed 1 TzTok-Jad. You gained 69,420 xp.");
        notifier.onChatMessage("You've completed 101 tasks and received 0 points, giving you a total of 200; return to a Slayer master.");

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifySlayer()).thenReturn(false);

        // fire chat messages
        notifier.onChatMessage("You have completed your task! You killed 1 TzTok-Jad. You gained 69,420 xp.");
        notifier.onChatMessage("You've completed 100 tasks and received 10 points, giving you a total of 200; return to a Slayer master.");

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
