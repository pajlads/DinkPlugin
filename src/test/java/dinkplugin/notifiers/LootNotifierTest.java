package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    @Mock
    ItemManager itemManager;

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
        when(config.lootNotifyMessage()).thenReturn("%USERNAME% has looted: %LOOT% from %SOURCE% for %TOTAL_VALUE% gp");

        // init item mocks
        when(itemManager.getItemPrice(ItemID.RUBY)).thenReturn(RUBY_PRICE);
        ItemComposition ruby = mock(ItemComposition.class);
        when(ruby.getName()).thenReturn("Ruby");
        when(itemManager.getItemComposition(ItemID.RUBY)).thenReturn(ruby);

        when(itemManager.getItemPrice(ItemID.OPAL)).thenReturn(OPAL_PRICE);
        ItemComposition opal = mock(ItemComposition.class);
        when(opal.getName()).thenReturn("Opal");
        when(itemManager.getItemComposition(ItemID.OPAL)).thenReturn(opal);

        when(itemManager.getItemPrice(ItemID.TUNA)).thenReturn(TUNA_PRICE);
        ItemComposition tuna = mock(ItemComposition.class);
        when(tuna.getName()).thenReturn("Tuna");
        when(itemManager.getItemComposition(ItemID.TUNA)).thenReturn(tuna);
    }

    @Test
    void testNotifyNpc() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        String name = "Rasmus";
        when(npc.getName()).thenReturn(name);

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)));
        notifier.onNpcLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has looted: %s from %s for %d gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", name, RUBY_PRICE))
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), name))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnoreNpc() {
        // prepare mocks
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(LOOTED_NAME);

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)));
        notifier.onNpcLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testNotifyPickpocket() {
        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), RUBY_PRICE);
        notifier.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has looted: %s from %s for %d gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", LOOTED_NAME, RUBY_PRICE))
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")), LOOTED_NAME))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnorePickpocket() {
        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.TUNA, 1, null)), TUNA_PRICE);
        notifier.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testNotifyPlayer() {
        // prepare mocks
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        notifier.onPlayerLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", LOOTED_NAME, QuantityFormatter.quantityToStackSize(RUBY_PRICE + TUNA_PRICE)))
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME))
                .type(NotificationType.LOOT)
                .build()
        );
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
            total
        );
        notifier.onLootReceived(event);

        // verify notification message
        String loot = String.format("1 x Ruby (%d)\n1 x Opal (%d)", RUBY_PRICE, OPAL_PRICE);
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, loot, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME))
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
            total
        );
        notifier.onLootReceived(event);

        // verify notification message
        String loot = String.format("5 x Tuna (%d)", 5 * TUNA_PRICE);
        verify(messageHandler).createMessage(
            false,
            NotificationBody.builder()
                .content(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, loot, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 5, TUNA_PRICE, "Tuna")), LOOTED_NAME))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnoreRepeated() {
        // fire event
        int total = TUNA_PRICE * 4;
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
            total
        );
        notifier.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyLoot()).thenReturn(false);

        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), RUBY_PRICE);
        notifier.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(anyBoolean(), any());
    }

}
