package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetNotifierTest extends MockedNotifierTest {

    @InjectMocks
    PetNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

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
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyOverride() {
        // define url override
        when(config.petWebhook()).thenReturn("https://example.com");

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");

        // verify handled at override url
        verify(messageHandler).createMessage(
            "https://example.com",
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send non-pet message
        notifier.onChatMessage("You feel Forsen's warmth behind you.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyPet()).thenReturn(false);

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyIrrelevantNameIgnore() {
        // ignore notifications for an irrelevant player
        when(config.ignoredNames()).thenReturn("xqc");
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnoredPlayerName() {
        // ignore notifications for our player name
        when(config.ignoredNames()).thenReturn(PLAYER_NAME);
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
