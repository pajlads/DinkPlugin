package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.GroupBankContentsNotificationData;
import dinkplugin.notifiers.data.LoginNotificationData;
import dinkplugin.notifiers.data.Progress;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.SerializedPet;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.RuneLiteConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetaNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    MetaNotifier notifier;

    int world = 420;
    int level = 50;
    long xp = Experience.getXpForLevel(level);
    int skillCount = Skill.values().length;
    String url = StringUtils.isNotBlank(PRIMARY_WEBHOOK_URL) ? PRIMARY_WEBHOOK_URL : "https://example.com";

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // update config mocks
        when(config.metadataWebhook()).thenReturn(url);

        // update client mocks
        when(client.getWorld()).thenReturn(world);

        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(1312);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(1477);

        when(client.getVarbitValue(VarbitID.CA_POINTS)).thenReturn(1984);
        when(client.getVarbitValue(VarbitID.CA_THRESHOLD_GRANDMASTER)).thenReturn(2005);

        when(client.getVarbitValue(VarbitID.FALADOR_DIARY_EASY_COMPLETE)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.VARROCK_DIARY_EASY_COMPLETE)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.WILDERNESS_DIARY_EASY_COMPLETE)).thenReturn(1);

        when(client.getVarbitValue(VarbitID.BARBASSAULT_GAMBLECOUNT)).thenReturn(666);

        when(client.getRealSkillLevel(any())).thenReturn(level);
        when(client.getSkillExperience(any())).thenReturn((int) xp);
        when(client.getTotalLevel()).thenReturn(skillCount * level);
        when(client.getOverallExperience()).thenReturn(skillCount * xp);

        when(client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT)).thenReturn(21);
        when(client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT)).thenReturn(158);
        when(client.getVarpValue(VarPlayerID.QP)).thenReturn(43);
        when(client.getVarbitValue(VarbitID.QP_MAX)).thenReturn(300);

        when(client.getVarbitValue(VarbitID.SLAYER_POINTS)).thenReturn(2484);
        when(client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED)).thenReturn(300);

        // too lazy to mock script results, just return zero so excluded from serialization in tests
        when(client.getIntStack()).thenReturn(new int[1]);
    }

    @Test
    void testNotify() {
        // fire event
        notifier.onGameState(GameState.LOGGING_IN, GameState.LOGGED_IN);
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // verify handled
        Map<String, Integer> levels = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> level));
        Map<String, Integer> exp = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> (int) xp));
        LoginNotificationData extra = new LoginNotificationData(world,
            Progress.of(1312, 1477),
            Progress.of(1984, 2005),
            Progress.of(3, 48),
            null,
            new LoginNotificationData.BarbarianAssault(666),
            new LoginNotificationData.SkillData(xp * skillCount, level * skillCount, levels, exp),
            Progress.of(21, 158), Progress.of(43, 300),
            new LoginNotificationData.SlayerData(2484, 300),
            null
        );
        verifyCreateMessage(
            url,
            false,
            NotificationBody.builder()
                .extra(extra)
                .text(buildTemplate(PLAYER_NAME + " logged into World " + world))
                .type(NotificationType.LOGIN)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyWithoutCollection() {
        // update client mock
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(0);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(0);

        // fire events
        notifier.onGameState(GameState.LOGGING_IN, GameState.LOGGED_IN);
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // verify handled
        Map<String, Integer> levels = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> level));
        Map<String, Integer> exp = Arrays.stream(Skill.values())
            .collect(Collectors.toMap(Skill::getName, s -> (int) xp));
        LoginNotificationData extra = new LoginNotificationData(world,
            null, // collection log data should not be present
            Progress.of(1984, 2005),
            Progress.of(3, 48),
            null,
            new LoginNotificationData.BarbarianAssault(666),
            new LoginNotificationData.SkillData(xp * skillCount, level * skillCount, levels, exp),
            Progress.of(21, 158), Progress.of(43, 300),
            new LoginNotificationData.SlayerData(2484, 300),
            null
        );
        verifyCreateMessage(
            url,
            false,
            NotificationBody.builder()
                .extra(extra)
                .text(buildTemplate(PLAYER_NAME + " logged into World " + world))
                .type(NotificationType.LOGIN)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testDisabled() {
        // update config mock
        when(config.metadataWebhook()).thenReturn("");

        // fire event
        notifier.onGameState(GameState.LOGGING_IN, GameState.LOGGED_IN);
        IntStream.rangeClosed(0, MetaNotifier.INIT_TICKS).forEach(i -> notifier.onTick());

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testPetDeserialization() {
        when(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, MetaNotifier.RL_CHAT_CMD_PLUGIN_NAME))
            .thenReturn(Boolean.TRUE.toString());

        when(configManager.getRSProfileConfiguration("chatcommands", "pets2"))
            .thenReturn(String.format("[%d, %d]", ItemID.HERBIBOARPET, ItemID.MOLEPET));

        mockItem(ItemID.HERBIBOARPET, 0, "Herbi");
        mockItem(ItemID.MOLEPET, 0, "Baby mole");

        List<SerializedPet> expected = List.of(
            new SerializedPet(ItemID.HERBIBOARPET, "Herbi"),
            new SerializedPet(ItemID.MOLEPET, "Baby mole")
        );
        Assertions.assertEquals(expected, notifier.getPets());
    }

    @Test
    void testLogoutNotify() {
        // Update config mock
        when(config.metadataWebhook()).thenReturn(url);

        // fire event
        notifier.onGameState(GameState.LOGGED_IN, GameState.LOGIN_SCREEN);
        // verify notifier
        verifyCreateMessage(
            url,
            false,
            NotificationBody.builder()
                .text(buildTemplate(PLAYER_NAME + " logged out"))
                .type(NotificationType.LOGOUT)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testLogoutDisabled() {
        // update config mock
        when(config.metadataWebhook()).thenReturn("");

        // fire event
        notifier.onGameState(GameState.LOGGED_IN, GameState.LOGIN_SCREEN);
        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyGroupStorage() {
        // mock shared bank widget
        Widget parent = mock(Widget.class);
        when(client.getWidget(InterfaceID.SharedBank.ITEMS)).thenReturn(parent);

        // mock item widgets
        Widget item1 = mock(Widget.class);
        when(item1.getItemId()).thenReturn(ItemID.RUNE_2H_SWORD);
        when(item1.getItemQuantity()).thenReturn(1);

        Widget item2 = mock(Widget.class);
        when(item2.getItemId()).thenReturn(ItemID.MITHRIL_BAR);
        when(item2.getItemQuantity()).thenReturn(10);

        Widget[] itemWidgets = new Widget[80];
        itemWidgets[0] = item1;
        itemWidgets[1] = item2;
        when(parent.getDynamicChildren()).thenReturn(itemWidgets);

        // mock item prices
        mockItem(ItemID.RUNE_2H_SWORD, 32000, "Rune 2h sword");
        mockItem(ItemID.MITHRIL_BAR, 300, "Mithril bar");

        // fire widget event
        WidgetLoaded event = new WidgetLoaded();
        event.setGroupId(InterfaceID.SHARED_BANK);
        notifier.onWidget(event);

        // verify notification
        List<SerializedItemStack> items = Arrays.asList(
            new SerializedItemStack(ItemID.RUNE_2H_SWORD, 1, 32000, "Rune 2h sword"),
            new SerializedItemStack(ItemID.MITHRIL_BAR, 10, 300, "Mithril bar")
        );
        verifyCreateMessage(
            url,
            false,
            NotificationBody.builder()
                .text(buildTemplate(PLAYER_NAME + " opened the GIM shared bank containing 2 items worth 35K with 80 slots unlocked"))
                .type(NotificationType.GROUP_BANK_CONTENTS)
                .extra(new GroupBankContentsNotificationData(items, 80))
                .playerName(PLAYER_NAME)
                .build()
        );
    }

}
