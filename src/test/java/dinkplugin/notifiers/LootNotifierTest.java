package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerConfig;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LootNotifierTest extends MockedNotifierTest {

    private static final int RUBY_PRICE = 900;
    private static final int OPAL_PRICE = 600;
    private static final int TUNA_PRICE = 100;
    private static final String LOOTED_NAME = "Rasmus";

    @Bind
    @InjectMocks
    LootNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyLoot()).thenReturn(true);
        when(config.lootSendImage()).thenReturn(false);
        when(config.lootIcons()).thenReturn(false);
        when(config.minLootValue()).thenReturn(500);
        when(config.includePlayerLoot()).thenReturn(true);
        when(config.lootIncludeClueScrolls()).thenReturn(true);
        when(config.lootNotifyMessage()).thenReturn("%USERNAME% has looted: %LOOT% from %SOURCE% for %TOTAL_VALUE% gp");

        // init client mocks
        WorldPoint location = new WorldPoint(0, 0, 0);
        when(localPlayer.getWorldLocation()).thenReturn(location);

        // init item mocks
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.TUNA, TUNA_PRICE, "Tuna");
    }

    @Test
    void testNotifyNpc() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        String name = "Rasmus";
        when(npc.getName()).thenReturn(name);
        int kc = 69;
        when(configManager.getConfiguration(eq(LootTrackerConfig.GROUP), any(), eq("drops_NPC_" + name)))
            .thenReturn("{\"type\":\"NPC\",\"name\":\"Rasmus\",\"kills\":" + kc +
                ",\"first\":1667708688588,\"last\":1667708688588,\"drops\":[526,69,1603,1]}");

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)));
        plugin.onNpcLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %d gp", PLAYER_NAME, RUBY_PRICE, name, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), name, LootRecordType.NPC, kc + 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyAllowlist() {
        // prepare mocks
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        notifier.onConfigChanged("lootItemAllowlist", "salmon\nraw trout\ntuna\npike");
        PlayerLootReceived event = new PlayerLootReceived(player, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{tuna}} (%d) from %s for %d gp", PLAYER_NAME, TUNA_PRICE, LOOTED_NAME, TUNA_PRICE))
                        .replacement("{{tuna}}", Replacements.ofWiki("Tuna"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.PLAYER, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyAllowlistWildcard() {
        // prepare mocks
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        notifier.onConfigChanged("lootItemAllowlist", "salmon\nraw trout\ntun*\npike");
        PlayerLootReceived event = new PlayerLootReceived(player, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{tuna}} (%d) from %s for %d gp", PLAYER_NAME, TUNA_PRICE, LOOTED_NAME, TUNA_PRICE))
                        .replacement("{{tuna}}", Replacements.ofWiki("Tuna"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.PLAYER, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnoreDenylist() {
        // fire event
        notifier.onConfigChanged("lootItemDenylist", "Ruby");
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreDenylistWildcard() {
        // fire event
        notifier.onConfigChanged("lootItemDenylist", "Rub*");
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Ensure LootReceived event for The Whisperer fires a notification - https://github.com/pajlads/DinkPlugin/pull/286")
    void testNotifyWhisperer() {
        String name = "The Whisperer";

        // fire event
        LootReceived event = new LootReceived(name, 99, LootRecordType.NPC, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %d gp", PLAYER_NAME, RUBY_PRICE, name, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), name, LootRecordType.NPC, 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    @DisplayName("Ensure NpcLootReceived for The Whisperer doesn't fire a notification - https://github.com/pajlads/DinkPlugin/pull/286")
    void testIgnoreWhispererNpcLootReceived() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        String name = "The Whisperer";
        when(npc.getName()).thenReturn(name);
        when(npc.getId()).thenReturn(NpcID.THE_WHISPERER);

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)));
        plugin.onNpcLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreNpc() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(LOOTED_NAME);

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onNpcLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPickpocket() {
        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %d gp", PLAYER_NAME, RUBY_PRICE, LOOTED_NAME, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), LOOTED_NAME, LootRecordType.PICKPOCKET, 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnorePickpocket() {
        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyClue() {
        // fire event
        String source = "Clue Scroll (Medium)";
        LootReceived event = new LootReceived(source, -1, LootRecordType.EVENT, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %d gp", PLAYER_NAME, RUBY_PRICE, source, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), source, LootRecordType.EVENT, 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnoreClue() {
        // update config mock
        when(config.lootIncludeClueScrolls()).thenReturn(false);

        // fire event
        LootReceived event = new LootReceived("Clue Scroll (Medium)", -1, LootRecordType.EVENT, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPlayer() {
        // prepare mocks
        when(config.pkWebhook()).thenReturn("https://example.com/");
        when(config.lootRedirectPlayerKill()).thenReturn(false);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %s gp", PLAYER_NAME, RUBY_PRICE, LOOTED_NAME, QuantityFormatter.quantityToStackSize(RUBY_PRICE + TUNA_PRICE)))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.PLAYER, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyPlayerForwarded() {
        // prepare mocks
        String overrideUrl = "https://example.com/";
        when(config.pkWebhook()).thenReturn(overrideUrl);
        when(config.lootRedirectPlayerKill()).thenReturn(true);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            overrideUrl,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %s gp", PLAYER_NAME, RUBY_PRICE, LOOTED_NAME, QuantityFormatter.quantityToStackSize(RUBY_PRICE + TUNA_PRICE)))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.PLAYER, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyPlayerForwardBlank() {
        // prepare mocks
        when(config.pkWebhook()).thenReturn("");
        when(config.lootRedirectPlayerKill()).thenReturn(true);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d) from %s for %s gp", PLAYER_NAME, RUBY_PRICE, LOOTED_NAME, QuantityFormatter.quantityToStackSize(RUBY_PRICE + TUNA_PRICE)))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .build()
                )
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.PLAYER, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyPkChest() {
        // update mocks
        final int minValue = 750;
        when(config.minLootValue()).thenReturn(minValue);
        assert OPAL_PRICE < minValue && TUNA_PRICE < minValue;

        // fire event
        String source = "Loot Chest";
        List<ItemStack> items = List.of(new ItemStack(ItemID.OPAL, 1, null), new ItemStack(ItemID.TUNA, 2, null));
        int totalValue = OPAL_PRICE + 2 * TUNA_PRICE;
        LootReceived event = new LootReceived(source, -1, LootRecordType.EVENT, items, 1);
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: Various items including: 1 x {{opal}} (%d) from %s for %s gp", PLAYER_NAME, OPAL_PRICE, source, QuantityFormatter.quantityToStackSize(totalValue)))
                        .replacement("{{opal}}", Replacements.ofWiki("Opal"))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal"), new SerializedItemStack(ItemID.TUNA, 2, TUNA_PRICE, "Tuna")), source, LootRecordType.EVENT, 1))
                .type(NotificationType.LOOT)
                .thumbnailUrl(ItemUtils.getItemImageUrl(ItemID.TUNA))
                .build()
        );
    }

    @Test
    void testIgnorePkChest() {
        // update mocks
        final int minValue = 750;
        when(config.minLootValue()).thenReturn(minValue);
        assert OPAL_PRICE < minValue && TUNA_PRICE < minValue;

        // fire event
        String source = "Loot Chest";
        List<ItemStack> items = List.of(new ItemStack(ItemID.OPAL, 1, null), new ItemStack(ItemID.TUNA, 1, null));
        LootReceived event = new LootReceived(source, -1, LootRecordType.EVENT, items, 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnorePlayer() {
        // prepare mocks
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);
        when(config.includePlayerLoot()).thenReturn(false);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        plugin.onPlayerLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyMultiple() {
        // fire event
        int total = RUBY_PRICE + OPAL_PRICE + TUNA_PRICE;
        LootReceived event = new LootReceived(
            LOOTED_NAME,
            99,
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(ItemID.RUBY, 1, null),
                new ItemStack(ItemID.OPAL, 1, null),
                new ItemStack(ItemID.TUNA, 1, null)
            ),
            3
        );
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 1 x {{ruby}} (%d)\n1 x {{opal}} (%d) from %s for %s gp", PLAYER_NAME, RUBY_PRICE, OPAL_PRICE, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{opal}}", Replacements.ofWiki("Opal"))
                        .build()
                )
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.EVENT, 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyRepeated() {
        // fire event
        int total = TUNA_PRICE * 5;
        LootReceived event = new LootReceived(
            LOOTED_NAME,
            99,
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null)
            ),
            5
        );
        plugin.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted: 5 x {{tuna}} (%d) from %s for %s gp", PLAYER_NAME, 5 * TUNA_PRICE, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
                        .replacement("{{tuna}}", Replacements.ofWiki("Tuna"))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 5, TUNA_PRICE, "Tuna")), LOOTED_NAME, LootRecordType.EVENT, 1))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnoreRepeated() {
        // fire event
        LootReceived event = new LootReceived(
            LOOTED_NAME,
            99,
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null),
                new ItemStack(ItemID.TUNA, 1, null)
            ),
            4
        );
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyLoot()).thenReturn(false);

        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
