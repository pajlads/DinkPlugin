package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.LootNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

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

        // fire event
        NpcLootReceived event = new NpcLootReceived(npc, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)));
        notifier.onNpcLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %d gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", name, RUBY_PRICE))
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
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPickpocket() {
        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), RUBY_PRICE);
        notifier.onLootReceived(event);

        // verify notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %d gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", LOOTED_NAME, RUBY_PRICE))
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
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
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
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, "1 x Ruby (" + RUBY_PRICE + ")", LOOTED_NAME, QuantityFormatter.quantityToStackSize(RUBY_PRICE + TUNA_PRICE)))
                .extra(new LootNotificationData(Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")), LOOTED_NAME))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testIgnorePlayer() {
        // prepare mocks
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(LOOTED_NAME);
        when(config.includePlayerLoot()).thenReturn(false);

        // fire event
        PlayerLootReceived event = new PlayerLootReceived(player, Arrays.asList(new ItemStack(ItemID.RUBY, 1, null), new ItemStack(ItemID.TUNA, 1, null)));
        notifier.onPlayerLootReceived(event);

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
            total
        );
        notifier.onLootReceived(event);

        // verify notification message
        String loot = String.format("1 x Ruby (%d)\n1 x Opal (%d)", RUBY_PRICE, OPAL_PRICE);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, loot, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
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
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, loot, LOOTED_NAME, QuantityFormatter.quantityToStackSize(total)))
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 5, TUNA_PRICE, "Tuna")), LOOTED_NAME))
                .type(NotificationType.LOOT)
                .build()
        );
    }

    @Test
    void testNotifyUnsired() {
        // prepare mocks
        Widget spriteWidget = mock(Widget.class);
        when(spriteWidget.getItemId()).thenReturn(ItemID.ABYSSAL_WHIP);
        when(client.getWidget(WidgetInfo.DIALOG_SPRITE)).thenReturn(spriteWidget);
        final int whipPrice = 1_500_000;
        mockItem(ItemID.ABYSSAL_WHIP, whipPrice, "Abyssal Whip");

        Widget textWidget = mock(Widget.class);
        when(textWidget.getText()).thenReturn("The Font consumes the Unsired and returns you a reward.");
        when(client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT)).thenReturn(textWidget);

        // fire event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(WidgetID.DIALOG_SPRITE_GROUP_ID);
        notifier.onWidgetLoaded(event);

        // verify notification message
        String source = "The Font of Consumption";
        String formattedPrice = QuantityFormatter.quantityToStackSize(whipPrice);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has looted: %s from %s for %s gp", PLAYER_NAME, "1 x Abyssal Whip (" + formattedPrice + ")", source, formattedPrice))
                .extra(new LootNotificationData(Collections.singletonList(new SerializedItemStack(ItemID.ABYSSAL_WHIP, 1, whipPrice, "Abyssal Whip")), source))
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
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyLoot()).thenReturn(false);

        // fire event
        LootReceived event = new LootReceived(LOOTED_NAME, 99, LootRecordType.PICKPOCKET, Collections.singletonList(new ItemStack(ItemID.RUBY, 1, null)), RUBY_PRICE);
        notifier.onLootReceived(event);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
