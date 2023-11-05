package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.FilterMode;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import net.runelite.api.ItemID;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.EnumSet;
import java.util.stream.IntStream;

import static dinkplugin.notifiers.PetNotifier.MAX_TICKS_WAIT;
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
        // update mocks
        when(config.petNotifyMessage()).thenReturn("%USERNAME% %GAME_MESSAGE%");

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " feels something weird sneaking into their backpack"))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyDuplicate() {
        // update mocks
        when(config.petNotifyMessage()).thenReturn("%USERNAME% %GAME_MESSAGE%");

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you would have been followed...");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, true, true))
                .text(buildTemplate(PLAYER_NAME + " has a funny feeling like they would have been followed..."))
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
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(1);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("New item added to your collection log: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, false))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyLostExistingCollection() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(1);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, true))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
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
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeableDuplicate() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.TZREKJAD;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you would have been followed...");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, true, true))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
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
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
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
        notifier.onTick();

        notifier.onChatMessage("random unrelated chat message");
        notifier.onClanNotification("random unrelated clan message");
        notifier.onTick();

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
                .extra(new PetNotificationData(petName, "50 killcount", false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
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
        notifier.onTick();

        notifier.onChatMessage("random unrelated chat message");
        notifier.onClanNotification("random unrelated clan message");
        notifier.onTick();

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
                .extra(new PetNotificationData(petName, "50 killcount", false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyOverride() {
        // update mocks
        when(config.petNotifyMessage()).thenReturn("%USERNAME% %GAME_MESSAGE%");

        // define url override
        when(config.petWebhook()).thenReturn("https://example.com");

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled at override url
        verify(messageHandler).createMessage(
            "https://example.com",
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " has a funny feeling like they're being followed"))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send non-pet message
        notifier.onChatMessage("You feel Forsen's warmth behind you.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyPet()).thenReturn(false);

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifySeasonal() {
        // update mocks
        when(config.ignoreSeasonal()).thenReturn(false);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnoreSeasonal() {
        // update mocks
        when(config.ignoreSeasonal()).thenReturn(true);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyIrrelevantNameIgnore() {
        // ignore notifications for an irrelevant player
        when(config.filteredNames()).thenReturn("xqc");
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyNameAllowList() {
        // only allow notifications for "dank dank"
        when(config.nameFilterMode()).thenReturn(FilterMode.ALLOW);
        when(config.filteredNames()).thenReturn(PLAYER_NAME);
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .type(NotificationType.PET)
                .build());
    }

    @Test
    void testIgnoreNameAllowList() {
        // only allow notifications for a different player
        when(config.nameFilterMode()).thenReturn(FilterMode.ALLOW);
        when(config.filteredNames()).thenReturn("xqc");
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoredPlayerName() {
        // ignore notifications for our player name
        when(config.filteredNames()).thenReturn(PLAYER_NAME);
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
