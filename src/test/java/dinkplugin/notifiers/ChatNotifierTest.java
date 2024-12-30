package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.ChatNotificationType;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.ChatNotificationData;
import net.runelite.api.ChatMessageType;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.config.Notification;
import net.runelite.client.events.NotificationFired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.awt.TrayIcon;
import java.util.EnumSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    ChatNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // config mocks
        when(config.notifyChat()).thenReturn(true);
        when(config.chatMessageTypes()).thenReturn(EnumSet.of(ChatNotificationType.GAME, ChatNotificationType.COMMAND, ChatNotificationType.RUNELITE, ChatNotificationType.CLAN));
        when(config.chatNotifyMessage()).thenReturn("%USERNAME% received a chat message:\n\n```\n%MESSAGE%\n```");
        setPatterns("You will be logged out in approximately 10 minutes.*\n" +
            "You will be logged out in approximately 5 minutes.*\n" +
            "Dragon impling is in the area\n" +
            "::TriggerDink\n" +
            "* has joined.");
    }

    @Test
    void testNotify() {
        // fire event
        String message = "You will be logged out in approximately 10 minutes.";
        notifier.onMessage(ChatMessageType.GAMEMESSAGE, null, message);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " received a chat message:\n\n```\n" + message + "\n```")
                        .build()
                )
                .extra(new ChatNotificationData(ChatMessageType.GAMEMESSAGE, null, null, message))
                .type(NotificationType.CHAT)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyCommand() {
        // fire event
        String message = "::TriggerDink";
        notifier.onCommand(new CommandExecuted(message.substring(2), new String[0]));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " received a chat message:\n\n```\n" + message + "\n```")
                        .build()
                )
                .extra(new ChatNotificationData(ChatMessageType.UNKNOWN, "CommandExecuted", null, message))
                .type(NotificationType.CHAT)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyTray() {
        // fire event
        String message = "Dragon impling is in the area";
        notifier.onNotification(new NotificationFired(Notification.ON, message, TrayIcon.MessageType.INFO));

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " received a chat message:\n\n```\n" + message + "\n```")
                        .build()
                )
                .extra(new ChatNotificationData(ChatMessageType.UNKNOWN, "NotificationFired", null, message))
                .type(NotificationType.CHAT)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyClan() {
        // update mocks
        var channel = mock(ClanChannel.class);
        var settings = mock(ClanSettings.class);
        when(client.getClanChannel()).thenReturn(channel);
        when(client.getClanSettings()).thenReturn(settings);

        var rank = ClanRank.OWNER;
        var title = new ClanTitle(rank.getRank(), "Queen");
        when(settings.titleForRank(rank)).thenReturn(title);

        var member = mock(ClanChannelMember.class);
        when(channel.findMember("Poki")).thenReturn(member);
        when(member.getRank()).thenReturn(rank);

        // fire event
        String message = "Poki has joined.";
        notifier.onMessage(ChatMessageType.CLAN_MESSAGE, "", message);

        // verify notification message
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " received a chat message:\n\n```\n" + message + "\n```")
                        .build()
                )
                .extra(new ChatNotificationData(ChatMessageType.CLAN_MESSAGE, "", title, message))
                .type(NotificationType.CHAT)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // fire event
        notifier.onMessage(ChatMessageType.GAMEMESSAGE, null, "You will be logged out in approximately 30 minutes.");
        notifier.onMessage(ChatMessageType.PUBLICCHAT, null, "You will be logged out in approximately 10 minutes.");
        notifier.onMessage(ChatMessageType.TRADE, null, "You will be logged out in approximately 10 minutes.");
        notifier.onMessage(ChatMessageType.CLAN_MESSAGE, null, "You will be logged out in approximately 10 minutes.");
        notifier.onCommand(new CommandExecuted("You", "will be logged out in approximately 10 minutes.".split(" ")));
        notifier.onCommand(new CommandExecuted("DontTriggerDink", new String[0]));
        notifier.onNotification(new NotificationFired(Notification.ON, "TriggerDink", TrayIcon.MessageType.INFO));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // update config mock
        when(config.notifyChat()).thenReturn(false);

        // fire event
        notifier.onMessage(ChatMessageType.GAMEMESSAGE, null, "You will be logged out in approximately 10 minutes.");
        notifier.onCommand(new CommandExecuted("TriggerDink", new String[0]));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private void setPatterns(String configValue) {
        when(config.chatPatterns()).thenReturn(configValue);
        notifier.onConfig(ChatNotifier.PATTERNS_CONFIG_KEY, configValue);
    }

}
