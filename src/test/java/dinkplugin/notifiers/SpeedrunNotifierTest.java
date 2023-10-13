package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.SpeedrunNotificationData;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.Duration;

import static dinkplugin.notifiers.SpeedrunNotifier.SPEEDRUN_COMPLETED_DURATION_CHILD_ID;
import static dinkplugin.notifiers.SpeedrunNotifier.SPEEDRUN_COMPLETED_GROUP_ID;
import static dinkplugin.notifiers.SpeedrunNotifier.SPEEDRUN_COMPLETED_PB_CHILD_ID;
import static dinkplugin.notifiers.SpeedrunNotifier.SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeedrunNotifierTest extends MockedNotifierTest {

    private static final String QUEST_NAME = "Cook's Assistant";
    private static final String PERSONAL_BEST = "1:30.25";

    @Bind
    @InjectMocks
    SpeedrunNotifier notifier;

    private static Template buildTemplate(String quest, String time) {
        return Template.builder()
            .template(String.format("%s has beat their PB of {{quest}} with a time of %s", PLAYER_NAME, time))
            .replacement("{{quest}}", Replacements.ofWiki(quest))
            .build();
    }

    private static Template buildNonPersonalBestTemplate(String quest, String time) {
        return Template.builder()
            .template(String.format("%s has just finished a speedrun of {{quest}} with a time of %s (their PB is %s)", PLAYER_NAME, time, PERSONAL_BEST))
            .replacement("{{quest}}", Replacements.ofWiki(quest))
            .build();
    }

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifySpeedrun()).thenReturn(true);
        when(config.speedrunPBOnly()).thenReturn(true);
        when(config.speedrunSendImage()).thenReturn(false);
        when(config.speedrunPBMessage()).thenReturn("%USERNAME% has beat their PB of %QUEST% with a time of %TIME%");
        when(config.speedrunMessage()).thenReturn("%USERNAME% has just finished a speedrun of %QUEST% with a time of %TIME% (their PB is %BEST%)");

        // init common widget mocks
        Widget quest = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID)).thenReturn(quest);
        when(quest.getText()).thenReturn("You have completed " + QUEST_NAME + "!");

        Widget pb = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_PB_CHILD_ID)).thenReturn(pb);
        when(pb.getText()).thenReturn(PERSONAL_BEST);
    }

    @Test
    void testNotify() {
        String latest = "1:15.30";

        // mock widget
        Widget time = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s (new personal best)", latest));
        plugin.onWidgetLoaded(event());

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(QUEST_NAME, latest))
                .extra(new SpeedrunNotificationData(QUEST_NAME, Duration.ofMinutes(1).plusSeconds(30).plusMillis(250).toString(), Duration.ofMinutes(1).plusSeconds(15).plusMillis(300).toString(), true))
                .type(NotificationType.SPEEDRUN)
                .build()
        );
    }

    @Test
    void testNotifyNotPersonalBest() {
        String latest = "1:40.30";

        // mock widget
        Widget time = mock(Widget.class);
        when(config.speedrunPBOnly()).thenReturn(false);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s", latest));
        plugin.onWidgetLoaded(event());

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildNonPersonalBestTemplate(QUEST_NAME, latest))
                .extra(new SpeedrunNotificationData(QUEST_NAME, Duration.ofMinutes(1).plusSeconds(30).plusMillis(250).toString(), Duration.ofMinutes(1).plusSeconds(40).plusMillis(300).toString(), false))
                .type(NotificationType.SPEEDRUN)
                .build()
        );
    }

    @Test
    void testIgnoreNonPersonalBest() {
        String latest = "1:45.30";

        // mock widget
        Widget time = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s", latest));
        plugin.onWidgetLoaded(event());

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifySpeedrun()).thenReturn(false);

        // mock widget
        String latest = "1:15.30";
        Widget time = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s (new personal best)", latest));
        plugin.onWidgetLoaded(event());

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyDenyList() {
        // configure irrelevant deny list
        when(config.filteredNames()).thenReturn("pajlads");
        settingsManager.init();

        // mock widget
        String latest = "1:15.30";
        Widget time = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s (new personal best)", latest));
        plugin.onWidgetLoaded(event());

        // check notification message
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(QUEST_NAME, latest))
                .extra(new SpeedrunNotificationData(QUEST_NAME, Duration.ofMinutes(1).plusSeconds(30).plusMillis(250).toString(), Duration.ofMinutes(1).plusSeconds(15).plusMillis(300).toString(), true))
                .type(NotificationType.SPEEDRUN)
                .build()
        );
    }

    @Test
    void testIgnoreDenyList() {
        String latest = "1:15.30";

        // configure deny list
        when(config.filteredNames()).thenReturn(PLAYER_NAME);
        settingsManager.init();

        // mock widget
        Widget time = mock(Widget.class);
        when(client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID)).thenReturn(time);
        when(time.getText()).thenReturn(latest);

        // fire fake event
        notifier.onGameMessage(String.format("Speedrun duration: %s (new personal best)", latest));
        plugin.onWidgetLoaded(event());

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private WidgetLoaded event() {
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(SPEEDRUN_COMPLETED_GROUP_ID);
        return event;
    }
}
