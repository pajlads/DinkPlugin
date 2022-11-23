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
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed: %s, getting %d points for a total %d tasks completed", PLAYER_NAME, "1 TzTok-Jad", 10, 100))
                .extra(new SlayerNotificationData("1 TzTok-Jad", "100", "10"))
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
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

}
