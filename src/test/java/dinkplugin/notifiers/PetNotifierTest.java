package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    PetNotifier notifier;

    @Bind
    @Mock
    ItemSearcher itemSearcher;

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
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null))
                .text(PLAYER_NAME + " got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyCollection() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("New item added to your collection log: " + petName);
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null))
                .text(PLAYER_NAME + " got a pet")
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeable() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null))
                .text(PLAYER_NAME + " got a pet")
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeableNotARealPet() {
        String petName = "Forsen";
        int itemId = ItemID.TZREKJAD;

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null))
                .text(PLAYER_NAME + " got a pet")
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyMultipleSameName() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        notifier.onClanNotification(
            String.format(
                "[ClanName] %s has a funny feeling like he would have been followed: %s at 50 killcount.",
                PLAYER_NAME,
                petName
            )
        );
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, "50 killcount"))
                .text(PLAYER_NAME + " got a pet")
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyClan() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onClanNotification(
            String.format(
                "[ClanName] %s has a funny feeling like he would have been followed: %s at 50 killcount.",
                PLAYER_NAME,
                petName
            )
        );
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, "50 killcount"))
                .text(PLAYER_NAME + " got a pet")
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
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
        notifier.onTick();

        // verify handled at override url
        verify(messageHandler).createMessage(
            "https://example.com",
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null))
                .text(PLAYER_NAME + " got a pet")
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send non-pet message
        notifier.onChatMessage("You feel Forsen's warmth behind you.");
        notifier.onTick();

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyPet()).thenReturn(false);

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        notifier.onTick();

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
        notifier.onTick();

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null))
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
        notifier.onTick();

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
