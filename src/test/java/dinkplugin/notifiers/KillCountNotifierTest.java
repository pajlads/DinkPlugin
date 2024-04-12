package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.Embed;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.TimeUtils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Varbits;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.time.Duration;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillCountNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    KillCountNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyKillCount()).thenReturn(true);
        when(config.killCountNotifyBestTime()).thenReturn(true);
        when(config.killCountSendImage()).thenReturn(true);
        when(config.killCountMessage()).thenReturn("%USERNAME% has defeated %BOSS% with a completion count of %COUNT%");
        when(config.killCountBestTimeMessage()).thenReturn("%USERNAME% has defeated %BOSS% with a new personal best time of %TIME% and a completion count of %COUNT%");

        // init client mocks
        when(client.getVarbitValue(TimeUtils.ENABLE_PRECISE_TIMING)).thenReturn(1);
        when(client.getCachedNPCs()).thenReturn(new NPC[0]);
    }

    @Test
    void testNotifyInterval() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(70);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 420.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        NotificationBody<BossNotificationData> body = NotificationBody.<BossNotificationData>builder()
            .text(buildTemplate("King Black Dragon", 420))
            .extra(new BossNotificationData("King Black Dragon", 420, gameMessage, null, null))
            .playerName(PLAYER_NAME)
            .type(NotificationType.KILL_COUNT)
            .build();

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            body
        );

        assertDoesNotThrow(() -> RuneLiteAPI.GSON.toJson(body));
    }

    @Test
    void testNotifyInitial() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(true);
        when(config.killCountInterval()).thenReturn(99);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate("King Black Dragon", 1))
                .extra(new BossNotificationData("King Black Dragon", 1, gameMessage, null, null))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(99);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // more config
        when(config.notifyKillCount()).thenReturn(false);
        when(config.killCountNotifyInitial()).thenReturn(true);
        when(config.killCountInterval()).thenReturn(1);

        // fire event
        String gameMessage = "Your King Black Dragon kill count is: 1.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:56.50 (new personal best).");
        notifier.onTick();

        // check notification
        NotificationBody<BossNotificationData> body = NotificationBody.<BossNotificationData>builder()
            .text(buildPbTemplate("Zulrah", "00:56.50", 12))
            .extra(new BossNotificationData("Zulrah", 12, gameMessage, Duration.ofSeconds(56).plusMillis(500), true))
            .playerName(PLAYER_NAME)
            .type(NotificationType.KILL_COUNT)
            .build();

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            body
        );

        assertDoesNotThrow(() -> RuneLiteAPI.GSON.toJson(body));
    }

    @Test
    void testNotifyPbDelayed() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Fight duration: 1:54.00 (new personal best)");
        IntStream.range(0, KillCountNotifier.MAX_BAD_TICKS - 1).forEach(i -> notifier.onTick());
        String gameMessage = "Your Grotesque Guardians kill count is: 79.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        NotificationBody<BossNotificationData> body = NotificationBody.<BossNotificationData>builder()
            .text(buildPbTemplate("Grotesque Guardians", "01:54.00", 79))
            .extra(new BossNotificationData("Grotesque Guardians", 79, gameMessage, Duration.ofMinutes(1).plusSeconds(54), true))
            .playerName(PLAYER_NAME)
            .type(NotificationType.KILL_COUNT)
            .build();

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            body
        );

        assertDoesNotThrow(() -> RuneLiteAPI.GSON.toJson(body));
    }

    @Test
    void testNotifyExtremeDelayMissingPb() {
        // more config
        when(config.killCountInterval()).thenReturn(1);

        // fire events
        notifier.onGameMessage("Fight duration: 1:54.00 (new personal best)");
        IntStream.range(0, KillCountNotifier.MAX_BAD_TICKS + 1).forEach(i -> notifier.onTick());
        String gameMessage = "Your Grotesque Guardians kill count is: 80.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        NotificationBody<BossNotificationData> body = NotificationBody.<BossNotificationData>builder()
            .text(buildTemplate("Grotesque Guardians", 80))
            .extra(new BossNotificationData("Grotesque Guardians", 80, gameMessage, null, null))
            .playerName(PLAYER_NAME)
            .type(NotificationType.KILL_COUNT)
            .build();

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            body
        );

        assertDoesNotThrow(() -> RuneLiteAPI.GSON.toJson(body));
    }

    @Test
    void testNotifyPbLong() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 1.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 1:00:56.50 (new personal best).");
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildPbTemplate("Zulrah", "01:00:56.50", 1))
                .extra(new BossNotificationData("Zulrah", 1, gameMessage, Duration.ofHours(1).plusSeconds(56).plusMillis(500), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyPbImprecise() {
        // more mocks
        when(config.killCountInterval()).thenReturn(99);
        when(client.getVarbitValue(TimeUtils.ENABLE_PRECISE_TIMING)).thenReturn(0);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 13.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:56 (new personal best).");
        notifier.onTick();

        // check notification
        NotificationBody<BossNotificationData> body = NotificationBody.<BossNotificationData>builder()
            .text(buildPbTemplate("Zulrah", "00:56", 13))
            .extra(new BossNotificationData("Zulrah", 13, gameMessage, Duration.ofSeconds(56), true))
            .playerName(PLAYER_NAME)
            .type(NotificationType.KILL_COUNT)
            .build();

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            body
        );

        assertDoesNotThrow(() -> RuneLiteAPI.GSON.toJson(body));
    }

    @Test
    void testNotifyChambersPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onFriendsChatNotification("Congratulations - your raid is complete!\nTeam size: 24+ players Duration: 36:04.20 (new personal best)");
        String gameMessage = "Your completed Chambers of Xeric count is: 125.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildPbTemplate("Chambers of Xeric", "36:04.20", 125))
                .extra(new BossNotificationData("Chambers of Xeric", 125, gameMessage, Duration.ofMinutes(36).plusSeconds(4).plusMillis(200), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyChambersInterval() {
        // more config
        when(config.killCountNotifyInitial()).thenReturn(false);
        when(config.killCountInterval()).thenReturn(25);

        // fire events
        notifier.onFriendsChatNotification("Congratulations - your raid is complete!\nTeam size: Solo Duration: 46:31.80 Personal best: 40:24.60");
        String gameMessage = "Your completed Chambers of Xeric count is: 150.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate("Chambers of Xeric", 150))
                .extra(new BossNotificationData("Chambers of Xeric", 150, gameMessage, Duration.ofMinutes(46).plusSeconds(31).plusMillis(800), false))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyGauntletPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Challenge duration: 10:25 (new personal best).");
        notifier.onGameMessage("Preparation time: 7:09.60. Hunllef kill time: 3:15.40.");
        String gameMessage = "Your Gauntlet completion count is: 10.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildPbTemplate("Crystalline Hunllef", "10:25.00", 10))
                .extra(new BossNotificationData("Crystalline Hunllef", 10, gameMessage, Duration.ofMinutes(10).plusSeconds(25), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyTemporossPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Subdued in 6:30 (new personal best).");
        String gameMessage = "Your Tempoross kill count is: 69.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildPbTemplate("Tempoross", "06:30.00", 69))
                .extra(new BossNotificationData("Tempoross", 69, gameMessage, Duration.ofMinutes(6).plusSeconds(30), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyTombsPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Tombs of Amascut: Expert Mode total completion time: 25:00 (new personal best)");
        String gameMessage = "Your completed Tombs of Amascut: Expert Mode count is: 8.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildPbTemplate("Tombs of Amascut: Expert Mode", "25:00.00", 8))
                .extra(new BossNotificationData("Tombs of Amascut: Expert Mode", 8, gameMessage, Duration.ofMinutes(25), true))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(6);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:59.30. Personal best: 0:56.50");
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate("Zulrah", 12))
                .extra(new BossNotificationData("Zulrah", 12, gameMessage, Duration.ofSeconds(59).plusMillis(300), false))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyPerilousMoons() {
        // update config mocks
        when(config.killCountInterval()).thenReturn(10);

        // fire events
        String gameMessage = "Your Lunar Chest count is: 30.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            true,
            NotificationBody.builder()
                .text(buildTemplate("Lunar Chest", 30))
                .extra(new BossNotificationData("Lunar Chest", 30, gameMessage, null, null))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .build()
        );
    }

    @Test
    void testNotifyBarbarianAssault() {
        // update mocks
        int count = 420;
        String boss = "Penance Queen";
        when(config.killCountPenanceQueen()).thenReturn(true);
        when(config.killCountSendImage()).thenReturn(false);
        when(client.getVarbitValue(Varbits.BA_GC)).thenReturn(count);
        Widget widget = mock(Widget.class);
        when(widget.getText()).thenReturn("Reward:<br>80 Healer points<br>5 Defender points<br>5 Collector points<br>5 Attacker points");
        when(client.getWidget(ComponentID.BA_REWARD_REWARD_TEXT)).thenReturn(widget);

        // fire event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(InterfaceID.BA_REWARD);
        notifier.onWidget(event);
        notifier.onTick();

        // check notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has defeated {{boss}} with a completion count of " + count + " high gambles")
                        .replacement("{{boss}}", Replacements.ofWiki(boss))
                        .build()
                )
                .extra(new BossNotificationData(boss, count, "The Queen is dead!", null, null))
                .playerName(PLAYER_NAME)
                .type(NotificationType.KILL_COUNT)
                .embeds(Collections.singletonList(Embed.ofImage(ItemUtils.getNpcImageUrl(NpcID.PENANCE_QUEEN))))
                .build()
        );
    }

    @Test
    void testIgnoreBarbarianAssault() {
        // update mocks
        when(config.killCountPenanceQueen()).thenReturn(false);
        when(client.getVarbitValue(Varbits.BA_GC)).thenReturn(420);
        Widget widget = mock(Widget.class);
        when(widget.getText()).thenReturn("Reward:<br>80 Healer points<br>5 Defender points<br>5 Collector points<br>5 Attacker points");
        when(client.getWidget(ComponentID.BA_REWARD_REWARD_TEXT)).thenReturn(widget);

        // fire event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(InterfaceID.BA_REWARD);
        notifier.onWidget(event);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        String gameMessage = "Your Zulrah kill count is: 12.";
        notifier.onGameMessage(gameMessage);
        notifier.onGameMessage("Fight duration: 0:59.30. Personal best: 0:56.50");
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreChambersNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onFriendsChatNotification("Congratulations - your raid is complete!\nTeam size: Solo Duration: 46:31.80 Personal best: 40:24.60");
        String gameMessage = "Your completed Chambers of Xeric count is: 147.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreTemporossNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Subdued in 6:13. Personal best: 5:57");
        String gameMessage = "Your Tempoross kill count is: 69.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreTombsNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Tombs of Amascut total completion time: 29:45.60. Personal best: 25:08.40");
        String gameMessage = "Your completed Tombs of Amascut count is: 40.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreTheatreNoPb() {
        // more config
        when(config.killCountInterval()).thenReturn(99);

        // fire events
        notifier.onGameMessage("Theatre of Blood total completion time: 23:42.60. Personal best: 20:47.00");
        String gameMessage = "Your completed Theatre of Blood count is: 17.";
        notifier.onGameMessage(gameMessage);
        notifier.onTick();

        // ensure no message
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static Template buildTemplate(String boss, int count) {
        return Template.builder()
            .template(PLAYER_NAME + " has defeated {{boss}} with a completion count of " + count)
            .replacement("{{boss}}", Replacements.ofWiki(boss))
            .build();
    }

    private static Template buildPbTemplate(String boss, String time, int count) {
        return Template.builder()
            .template(String.format("%s has defeated {{boss}} with a new personal best time of %s and a completion count of %d", PLAYER_NAME, time, count))
            .replacement("{{boss}}", Replacements.ofWiki(boss))
            .build();
    }
}
