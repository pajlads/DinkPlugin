package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.ClueNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.ItemID;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClueNotifierTest extends MockedNotifierTest {

    private static final int RUBY_PRICE = 900;
    private static final int TUNA_PRICE = 100;

    @Bind
    @Mock
    ItemManager itemManager;

    @InjectMocks
    ClueNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyClue()).thenReturn(true);
        when(config.clueSendImage()).thenReturn(false);
        when(config.clueShowItems()).thenReturn(false);
        when(config.clueMinValue()).thenReturn(500);
        when(config.clueNotifyMessage()).thenReturn("%USERNAME% has completed a %CLUE% clue, for a total of %COUNT%. They obtained: %LOOT%");

        // init item mocks
        mockItem(itemManager, ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(itemManager, ItemID.TUNA, TUNA_PRICE, "Tuna");
    }

    @Test
    void testNotify() {
        // fire chat event
        notifier.onChatMessage("You have completed 1312 medium Treasure Trails.");

        // mock widgets
        Widget widget = mock(Widget.class);
        when(client.getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER)).thenReturn(widget);

        Widget child = mock(Widget.class);
        when(child.getItemQuantity()).thenReturn(1);
        when(child.getItemId()).thenReturn(ItemID.RUBY);

        Widget[] children = { child };
        when(widget.getChildren()).thenReturn(children);

        // fire widget event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(WidgetID.CLUE_SCROLL_REWARD_GROUP_ID);
        notifier.onWidgetLoaded(event);

        // verify notification message
        verify(messageHandler).createMessage(
            URL,
            false,
            NotificationBody.builder()
                .content(String.format("%s has completed a %s clue, for a total of %d. They obtained: %s", PLAYER_NAME, "medium", 1312, "1 x Ruby (" + RUBY_PRICE + ")"))
                .extra(new ClueNotificationData("medium", 1312, Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"))))
                .type(NotificationType.CLUE)
                .build()
        );
    }

    @Test
    void testIgnoreLoot() {
        // fire chat event
        notifier.onChatMessage("You have completed 1337 medium Treasure Trails.");

        // mock widgets
        Widget widget = mock(Widget.class);
        when(client.getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER)).thenReturn(widget);

        Widget child = mock(Widget.class);
        when(child.getItemQuantity()).thenReturn(1);
        when(child.getItemId()).thenReturn(ItemID.TUNA);

        Widget[] children = { child };
        when(widget.getChildren()).thenReturn(children);

        // fire widget event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(WidgetID.CLUE_SCROLL_REWARD_GROUP_ID);
        notifier.onWidgetLoaded(event);

        // ensure no notification was fired
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyClue()).thenReturn(false);

        // fire chat event
        notifier.onChatMessage("You have completed 1312 medium Treasure Trails.");

        // mock widgets
        Widget widget = mock(Widget.class);
        when(client.getWidget(WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER)).thenReturn(widget);

        Widget child = mock(Widget.class);
        when(child.getItemQuantity()).thenReturn(1);
        when(child.getItemId()).thenReturn(ItemID.RUBY);

        Widget[] children = { child };
        when(widget.getChildren()).thenReturn(children);

        // fire widget event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(WidgetID.CLUE_SCROLL_REWARD_GROUP_ID);
        notifier.onWidgetLoaded(event);

        // ensure no notification was fired
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
