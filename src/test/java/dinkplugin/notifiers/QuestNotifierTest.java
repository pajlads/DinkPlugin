package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;
import static net.runelite.api.widgets.WidgetInfo.QUEST_COMPLETED_NAME_TEXT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    QuestNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyQuest()).thenReturn(true);
        when(config.questSendImage()).thenReturn(false);
        when(config.questNotifyMessage()).thenReturn("%USERNAME% has completed: %QUEST%");

    }

    @Test
    void testNotify() {
        // init client mocks
        when(client.getVarbitValue(QuestNotifier.COMPLETED_ID)).thenReturn(22);
        when(client.getVarbitValue(QuestNotifier.TOTAL_ID)).thenReturn(156);
        when(client.getVarpValue(VarPlayer.QUEST_POINTS)).thenReturn(44);
        when(client.getVarbitValue(QuestNotifier.QP_TOTAL_ID)).thenReturn(293);

        // mock widget
        Widget questWidget = mock(Widget.class);
        when(client.getWidget(QUEST_COMPLETED_NAME_TEXT)).thenReturn(questWidget);
        when(questWidget.getText()).thenReturn("You have completed the Dragon Slayer I quest!");

        // send event
        plugin.onWidgetLoaded(event(QUEST_COMPLETED_GROUP_ID));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(PLAYER_NAME + " has completed: Dragon Slayer I")
                .extra(new QuestNotificationData("Dragon Slayer I", 22, 156, 44, 293))
                .type(NotificationType.QUEST)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send unrelated event
        plugin.onWidgetLoaded(event(-1));

        // verify no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyQuest()).thenReturn(false);

        // mock widget
        Widget questWidget = mock(Widget.class);
        when(client.getWidget(QUEST_COMPLETED_NAME_TEXT)).thenReturn(questWidget);
        when(questWidget.getText()).thenReturn("You have completed the Dragon Slayer quest!");

        // send event
        plugin.onWidgetLoaded(event(QUEST_COMPLETED_GROUP_ID));

        // verify no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static WidgetLoaded event(int id) {
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(id);
        return event;
    }

}
