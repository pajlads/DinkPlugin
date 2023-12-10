package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static dinkplugin.util.QuestUtils.parseQuestWidget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        when(client.getWidget(ComponentID.QUEST_COMPLETED_NAME_TEXT)).thenReturn(questWidget);
        when(questWidget.getText()).thenReturn("You have completed the Dragon Slayer I quest!");

        // send event
        plugin.onWidgetLoaded(event(InterfaceID.QUEST_COMPLETED));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has completed: {{quest}}")
                        .replacement("{{quest}}", Replacements.ofWiki("Dragon Slayer I"))
                        .build()
                )
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
        when(client.getWidget(ComponentID.QUEST_COMPLETED_NAME_TEXT)).thenReturn(questWidget);
        when(questWidget.getText()).thenReturn("You have completed the Dragon Slayer quest!");

        // send event
        plugin.onWidgetLoaded(event(InterfaceID.QUEST_COMPLETED));

        // verify no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void parseWidgets() {
        assertEquals("Recipe for Disaster - Another Cook's Quest", parseQuestWidget("You have completed Another Cook's Quest!"));
        assertEquals("Recipe for Disaster - Another Cook's Quest", parseQuestWidget("You have assisted the Lumbridge Cook... again!"));
        assertEquals("Recipe for Disaster - Goblin Generals", parseQuestWidget("You have freed the Goblin Generals!"));
        assertEquals("Recipe for Disaster - Sir Amik Varze", parseQuestWidget("You have freed Sir Amik Varze!"));
        assertEquals("Recipe for Disaster - Skrach Uglogwee", parseQuestWidget("You have freed Skrach 'Bone Crusher' Uglogwee!"));
        assertEquals("Recipe for Disaster", parseQuestWidget("You have completed Recipe for Disaster!"));

        assertEquals("Doric's Quest", parseQuestWidget("You have completed Doric's Quest!"));
        assertEquals("Heroes Quest", parseQuestWidget("You have completed the Heroes' Quest!"));

        assertEquals("Dragon Slayer II", parseQuestWidget("You have completed Dragon Slayer II!"));
        assertEquals("Rag and Bone Man II", parseQuestWidget("You have completed Rag and Bone Man II!"));
        assertEquals("Fairytale II - Cure a Queen", parseQuestWidget("You have completed Fairytale II - Cure a Queen!"));

        assertNull(parseQuestWidget("You have... kind of... completed Hazeel Cult!"));
        assertEquals("Hazeel Cult", parseQuestWidget("You have completed Hazeel Cult!"));
    }

    private static WidgetLoaded event(int id) {
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(id);
        return event;
    }

}
