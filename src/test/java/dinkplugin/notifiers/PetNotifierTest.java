package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.AccountType;
import dinkplugin.domain.FilterMode;
import dinkplugin.domain.SeasonalPolicy;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.PetNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import dinkplugin.util.MathUtils;
import net.runelite.api.NPC;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.events.NpcLootReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.EnumSet;
import java.util.stream.IntStream;

import static dinkplugin.notifiers.PetNotifier.MAX_TICKS_WAIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
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

    @Bind
    @InjectMocks
    KillCountService killCountService;

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
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
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
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, true, true, null, null, null))
                .text(buildTemplate(PLAYER_NAME + " has a funny feeling like they would have been followed..."))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyCollection() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);
        when(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM)).thenReturn(1);
        String npcName = "TzTok-Jad";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(npcName);
        when(npc.getId()).thenReturn(NpcID.TZHAAR_FIGHTCAVE_SWARM_BOSS);
        int kc = 100;
        double rarity = 1.0 / 200;
        double luck = MathUtils.cumulativeGeometric(rarity, kc);
        when(configManager.getRSProfileConfiguration("killcount", npcName.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, Collections.emptyList()));

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("New item added to your collection log: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, false, rarity, kc, luck))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyLostExistingCollection() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);
        when(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM)).thenReturn(1);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, true, 1.0 / 200, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeable() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, null, 1.0 / 200, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeableDuplicate() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

        // prepare mocks
        when(itemSearcher.findItemId("Tzrek-jad")).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you would have been followed...");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, true, true, 1.0 / 200, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyUntradeableNotARealPet() {
        String petName = "Forsen";
        int itemId = ItemID.JAD_PET;

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onChatMessage("Untradeable drop: " + petName);
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyMultipleSameName() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

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
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, "50 killcount", false, null, 1.0 / 200, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyClan() {
        String petName = "TzRek-Jad";
        int itemId = ItemID.JAD_PET;

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
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, "50 killcount", false, null, 1.0 / 200, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .thumbnailUrl(ItemUtils.getItemImageUrl(itemId))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testNotifyClanUnsired() {
        String petName = "Abyssal orphan";
        int itemId = ItemID.ABYSSALSIRE_PET;

        // prepare mocks
        when(itemSearcher.findItemId(petName)).thenReturn(itemId);

        // send fake message
        notifier.onChatMessage("You have a funny feeling like you're being followed.");
        notifier.onTick();

        notifier.onClanNotification(
            String.format(
                "[ClanName] %s feels like she's acquired something special: %s",
                PLAYER_NAME,
                petName
            )
        );
        IntStream.range(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(petName, null, false, null, 1 / 25.6 / 100, null, null))
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
        verifyCreateMessage(
            "https://example.com",
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
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
        when(config.seasonalPolicy()).thenReturn(SeasonalPolicy.ACCEPT);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .type(NotificationType.PET)
                .build()
        );
    }

    @Test
    void testIgnoreSeasonal() {
        // update mocks
        when(config.seasonalPolicy()).thenReturn(SeasonalPolicy.REJECT);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.SEASONAL));
        worldTracker.onWorldChange();

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
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
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
        when(config.deniedAccountTypes()).thenReturn(EnumSet.of(AccountType.GROUP_IRONMAN));
        settingsManager.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .extra(new PetNotificationData(null, null, false, null, null, null, null))
                .text(buildTemplate(PLAYER_NAME + " got a pet"))
                .type(NotificationType.PET)
                .build());
    }

    @Test
    void testIgnoreAccountType() {
        // prevent notifs from group ironmen
        when(config.nameFilterMode()).thenReturn(FilterMode.DENY);
        when(config.deniedAccountTypes()).thenReturn(EnumSet.of(AccountType.GROUP_IRONMAN));
        settingsManager.init();
        accountTracker.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreNameAllowList() {
        // only allow notifications for a different player
        when(config.nameFilterMode()).thenReturn(FilterMode.ALLOW);
        when(config.filteredNames()).thenReturn("xqc");
        settingsManager.init();
        accountTracker.init();

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
        accountTracker.init();

        // send fake message
        notifier.onChatMessage("You feel something weird sneaking into your backpack.");
        IntStream.rangeClosed(0, MAX_TICKS_WAIT).forEach(i -> notifier.onTick());

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
