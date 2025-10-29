package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.LootCriteria;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.AnnotatedItemStack;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.RareItemStack;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
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
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BingoNotifierTest extends MockedNotifierTest {

    private static final int SHARD_PRICE = 10_000_000;
    private static final int SCEPTRE_PRICE = 5_000_000;
    private static final int LARRAN_PRICE = 150_000;
    private static final int RUBY_PRICE = 900;
    private static final int OPAL_PRICE = 600;
    private static final int TUNA_PRICE = 100;
    private static final String LOOTED_NAME = "Rasmus";

    @Bind
    @InjectMocks
    BingoNotifier notifier;

    @Bind
    @InjectMocks
    KillCountService killCountService;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyBingo()).thenReturn(true);
        when(config.bingoSendImage()).thenReturn(false);
        when(config.bingoNotifyMessage()).thenReturn("%USERNAME% has looted bingo item: %LOOT% from %SOURCE% for %TOTAL_VALUE% gp");

        // init client mocks
        WorldPoint location = new WorldPoint(0, 0, 0);
        when(localPlayer.getWorldLocation()).thenReturn(location);

        // init item mocks
        mockItem(ItemID.BLOOD_SHARD, SHARD_PRICE, "Blood shard");
        mockItem(ItemID.PHARAOHS_SCEPTRE, SCEPTRE_PRICE, "Pharaoh's sceptre");
        mockItem(ItemID.SLAYER_WILDERNESS_KEY, LARRAN_PRICE, "Larran's key");
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.TUNA, TUNA_PRICE, "Tuna");
    }

    private void mockWorldNpcs(NPC npc) {
        mockNpcs(new NPC[] { npc });
    }

    @Test
    void testNotifyNpc() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        String name = "Rasmus";
        when(npc.getName()).thenReturn(name);
        when(npc.getId()).thenReturn(9999);
        mockWorldNpcs(npc);
        int kc = 69;
        when(configManager.getConfiguration(eq(LootTrackerConfig.GROUP), any(), eq("drops_NPC_" + name)))
            .thenReturn("{\"type\":\"NPC\",\"name\":\"Rasmus\",\"kills\":" + kc +
                ",\"first\":1667708688588,\"last\":1667708688588,\"drops\":[526,69,1603,1]}");

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1)));
        plugin.onNpcLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{ruby}} (%d) from {{source}} for %d gp", PLAYER_NAME, RUBY_PRICE, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(name))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new AnnotatedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), name, LootRecordType.NPC, kc + 1, null, null, 9999))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyAllowlistWildcard() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        String name = "Rasmus";
        when(npc.getName()).thenReturn(name);
        when(npc.getId()).thenReturn(9999);
        mockWorldNpcs(npc);
        int kc = 69;
        when(configManager.getConfiguration(eq(LootTrackerConfig.GROUP), any(), eq("drops_NPC_" + name)))
            .thenReturn("{\"type\":\"NPC\",\"name\":\"Rasmus\",\"kills\":" + kc +
                ",\"first\":1667708688588,\"last\":1667708688588,\"drops\":[526,69,1603,1]}");

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "rub*");
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1)));
        plugin.onNpcLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{ruby}} (%d) from {{source}} for %d gp", PLAYER_NAME, RUBY_PRICE, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(name))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new AnnotatedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), name, LootRecordType.NPC, kc + 1, null, null, 9999))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    @DisplayName("Ensure LootReceived event for The Whisperer fires a notification - https://github.com/pajlads/DinkPlugin/pull/286")
    void testNotifyWhisperer() {
        String name = "The Whisperer";
        NPC npc = Mockito.mock(NPC.class);
        when(npc.getName()).thenReturn(name);
        when(npc.getId()).thenReturn(NpcID.WHISPERER);
        mockWorldNpcs(npc);

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        LootReceived event = new LootReceived(name, 99, LootRecordType.NPC, Collections.singletonList(new ItemStack(ItemID.RUBY, 1)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{ruby}} (%d) from {{source}} for %d gp", PLAYER_NAME, RUBY_PRICE, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(name))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new AnnotatedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), name, LootRecordType.NPC, 1, null, null, NpcID.WHISPERER))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyPickpocket() {
        String name = "Remus Kaninus";
        NPC npc = Mockito.mock(NPC.class);
        when(npc.getName()).thenReturn(name);
        when(npc.getId()).thenReturn(NpcID.REMUS_KANINUS);
        mockWorldNpcs(npc);

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "blood shard");
        LootReceived event = new LootReceived(name, -1, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.BLOOD_SHARD, 1)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        double rarity = 1.0 / 5000;
        String price = QuantityFormatter.quantityToStackSize(SHARD_PRICE);
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{shard}} (%s) from {{source}} for %s gp", PLAYER_NAME, price, price))
                        .replacement("{{shard}}", Replacements.ofWiki("Blood shard"))
                        .replacement("{{source}}", Replacements.ofWiki(name))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new RareItemStack(ItemID.BLOOD_SHARD, 1, SHARD_PRICE, "Blood shard", EnumSet.of(LootCriteria.ALLOWLIST), rarity)), name, LootRecordType.PICKPOCKET, 1, rarity, null, NpcID.REMUS_KANINUS))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyClue() {
        // prepare completion count
        killCountService.onGameMessage("You have completed 42 medium Treasure Trails.");

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        String source = "Clue Scroll (Medium)";
        LootReceived event = new LootReceived(source, -1, LootRecordType.EVENT, Collections.singletonList(new ItemStack(ItemID.RUBY, 1)), 1);
        plugin.onLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{ruby}} (%d) from {{source}} for %d gp", PLAYER_NAME, RUBY_PRICE, RUBY_PRICE))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(source))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new AnnotatedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), source, LootRecordType.EVENT, 42, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnorePkChest() {
        // update mocks
        final int minValue = 750;
        when(config.minLootValue()).thenReturn(minValue);
        notifier.onConfigChanged("bingoItemAllowlist", "opal");

        // fire event
        String source = "Loot Chest";
        List<ItemStack> items = List.of(new ItemStack(ItemID.OPAL, 1), new ItemStack(ItemID.TUNA, 1));
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
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1), new ItemStack(ItemID.TUNA, 1)));
        plugin.onPlayerLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyMultiple() {
        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "ruby\nopal");
        int total = RUBY_PRICE + OPAL_PRICE + TUNA_PRICE;
        LootReceived event = new LootReceived(
            LOOTED_NAME,
            99,
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(ItemID.RUBY, 1),
                new ItemStack(ItemID.OPAL, 1),
                new ItemStack(ItemID.TUNA, 1)
            ),
            3
        );
        plugin.onLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{ruby}} (%d)\n1 x {{opal}} (%d) from {{source}} for %s gp", PLAYER_NAME, RUBY_PRICE, OPAL_PRICE, QuantityFormatter.quantityToStackSize(total)))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{opal}}", Replacements.ofWiki("Opal"))
                        .replacement("{{source}}", Replacements.ofWiki(LOOTED_NAME))
                        .build()
                )
                .extra(new LootNotificationData(Arrays.asList(new AnnotatedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST)), new AnnotatedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal", EnumSet.of(LootCriteria.ALLOWLIST)), new AnnotatedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna", EnumSet.noneOf(LootCriteria.class))), LOOTED_NAME, LootRecordType.EVENT, 1, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyRepeated() {
        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "tuna");
        int total = TUNA_PRICE * 5;
        LootReceived event = new LootReceived(
            LOOTED_NAME,
            99,
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(ItemID.TUNA, 1),
                new ItemStack(ItemID.TUNA, 1),
                new ItemStack(ItemID.TUNA, 1),
                new ItemStack(ItemID.TUNA, 1),
                new ItemStack(ItemID.TUNA, 1)
            ),
            5
        );
        plugin.onLootReceived(event);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 5 x {{tuna}} (%d) from {{source}} for %s gp", PLAYER_NAME, 5 * TUNA_PRICE, QuantityFormatter.quantityToStackSize(total)))
                        .replacement("{{tuna}}", Replacements.ofWiki("Tuna"))
                        .replacement("{{source}}", Replacements.ofWiki(LOOTED_NAME))
                        .build()
                )
                .extra(new LootNotificationData(Collections.singletonList(new AnnotatedItemStack(ItemID.TUNA, 5, TUNA_PRICE, "Tuna", EnumSet.of(LootCriteria.ALLOWLIST))), LOOTED_NAME, LootRecordType.EVENT, 1, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyGauntlet() {
        // prepare data
        int kc = 123;
        int quantity = 24;
        String total = QuantityFormatter.quantityToStackSize(quantity * RUBY_PRICE);
        String source = "Crystalline Hunllef";
        String realSource = "The Gauntlet";
        List<ItemStack> items = List.of(new ItemStack(ItemID.RUBY, quantity));
        mockNpcs(new NPC[0]);

        // fire events
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        killCountService.onGameMessage(String.format("Your Gauntlet completion count is: %d.", kc));
        plugin.onLootReceived(new LootReceived(source, 674, LootRecordType.NPC, items, 1));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: %d x {{ruby}} (%s) from {{source}} for %s gp", PLAYER_NAME, quantity, total, total))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(realSource))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new AnnotatedItemStack(ItemID.RUBY, quantity, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), realSource, LootRecordType.EVENT, kc, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyCorruptedGauntlet() {
        // prepare data
        int kc = 123;
        int quantity = 24;
        String total = QuantityFormatter.quantityToStackSize(quantity * RUBY_PRICE);
        String source = "Corrupted Hunllef";
        String realSource = "Corrupted Gauntlet";
        List<ItemStack> items = List.of(new ItemStack(ItemID.RUBY, quantity));
        mockNpcs(new NPC[0]);

        // fire events
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        killCountService.onGameMessage(String.format("Your Corrupted Gauntlet completion count is: %d.", kc));
        plugin.onLootReceived(new LootReceived(source, 894, LootRecordType.NPC, items, 1));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: %d x {{ruby}} (%s) from {{source}} for %s gp", PLAYER_NAME, quantity, total, total))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(realSource))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new AnnotatedItemStack(ItemID.RUBY, quantity, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), realSource, LootRecordType.EVENT, kc, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyAmascut() {
        // prepare data
        int kc = 123;
        int quantity = 24;
        String total = QuantityFormatter.quantityToStackSize(quantity * RUBY_PRICE);
        String source = "Tombs of Amascut";
        List<ItemStack> items = List.of(new ItemStack(ItemID.RUBY, quantity));

        // fire events
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        killCountService.onGameMessage(String.format("Your completed %s count is: %d.", source, kc));
        plugin.onLootReceived(new LootReceived("Tombs of Amascut", -1, LootRecordType.EVENT, items, 1));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: %d x {{ruby}} (%s) from {{source}} for %s gp", PLAYER_NAME, quantity, total, total))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(source))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new AnnotatedItemStack(ItemID.RUBY, quantity, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), source, LootRecordType.EVENT, kc, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyAmascutExpert() {
        // prepare data
        int kc = 123;
        int quantity = 24;
        String total = QuantityFormatter.quantityToStackSize(quantity * RUBY_PRICE);
        String source = "Tombs of Amascut: Expert Mode";
        List<ItemStack> items = List.of(new ItemStack(ItemID.RUBY, quantity));

        // fire events
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        killCountService.onGameMessage(String.format("Your completed %s count is: %d.", source, kc));
        plugin.onLootReceived(new LootReceived("Tombs of Amascut", -1, LootRecordType.EVENT, items, 1));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: %d x {{ruby}} (%s) from {{source}} for %s gp", PLAYER_NAME, quantity, total, total))
                        .replacement("{{ruby}}", Replacements.ofWiki("Ruby"))
                        .replacement("{{source}}", Replacements.ofWiki(source))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new AnnotatedItemStack(ItemID.RUBY, quantity, RUBY_PRICE, "Ruby", EnumSet.of(LootCriteria.ALLOWLIST))), source, LootRecordType.EVENT, kc, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyPharaohSceptre() {
        // prepare data
        String total = QuantityFormatter.quantityToStackSize(SCEPTRE_PRICE);
        String source = "Pyramid Plunder";

        // fire chat event
        notifier.onConfigChanged("bingoItemAllowlist", "pharaoh's sceptre");
        notifier.onGameMessage("You have found a Pharaoh's sceptre! It fell on the floor.");

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has looted bingo item: 1 x {{sceptre}} (%s) from {{source}} for %s gp", PLAYER_NAME, total, total))
                        .replacement("{{sceptre}}", Replacements.ofWiki("Pharaoh's sceptre"))
                        .replacement("{{source}}", Replacements.ofWiki(source))
                        .build()
                )
                .extra(new LootNotificationData(List.of(new AnnotatedItemStack(ItemID.PHARAOHS_SCEPTRE, 1, SCEPTRE_PRICE, "Pharaoh's sceptre", EnumSet.of(LootCriteria.ALLOWLIST))), source, LootRecordType.EVENT, null, null, null, null))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyBingo()).thenReturn(false);

        // fire event
        notifier.onConfigChanged("bingoItemAllowlist", "ruby");
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1)), 1);
        plugin.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
