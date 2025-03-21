package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.CollectionLogRanks;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.CollectionNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.KillCountService;
import net.runelite.api.*;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.BiFunction;

import static dinkplugin.notifiers.CollectionNotifier.TOTAL_ENTRIES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class CollectionNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    CollectionNotifier notifier;

    @Bind
    @Mock
    ItemSearcher itemSearcher;

    @Bind
    @InjectMocks
    KillCountService killCountService;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init client mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(1);
        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_POSSIBLE_LOGS_VARP)).thenReturn(TOTAL_ENTRIES);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);

        final String[] CLOG_RANK = {"None", "Bronze", "Iron", "Steel", "Black", "Mithril", "Adamant", "Rune", "Dragon", "Gilded"};
        final int[] CLOG_THRESHOLDS = {0, 100, 300, 500, 700, 900, 1000, 1100, 1200, 1300, 1400};
        int i = 0;
        for (CollectionLogRanks rank : CollectionLogRanks.values()) {
            StructComposition s = mock(StructComposition.class);
            when(s.getStringValue(CollectionLogRanks.RANK_VARP)).thenReturn(CLOG_RANK[i]);
            when(s.getIntValue(CollectionLogRanks.RANK_CLOGS_VARP)).thenReturn(CLOG_THRESHOLDS[i]);
            IterableHashTable params = mock(IterableHashTable.class);
            when(s.getParams()).thenReturn(params);
            when(client.getStructComposition(rank.getStructId())).thenReturn(s);
            i++;
        }
        notifier.onTick();
        notifier.init();;

        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(CollectionNotifier.COMPLETED_LOGS_VARP);
        initCompleted.setValue(0);
        notifier.onVarPlayer(initCompleted);

        // init config mocks
        when(config.notifyCollectionLog()).thenReturn(true);
        when(config.collectionSendImage()).thenReturn(false);
        when(config.collectionNotifyMessage()).thenReturn("%USERNAME% has added %ITEM% to their collection");
    }

    @Test
    void testNotify() {
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        mockItem(ItemID.SEERCULL, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGANNOTH_SUPREME);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.SEERCULL, 1))));

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES, "None", 99, "Bronze", source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testRankUp() {
        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(299);
        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(CollectionNotifier.COMPLETED_LOGS_VARP);
        initCompleted.setValue(299);
        notifier.onVarPlayer(initCompleted);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        mockItem(ItemID.SEERCULL, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGANNOTH_SUPREME);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.SEERCULL, 1))));

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 300, TOTAL_ENTRIES, "Iron", 200, "Steel", source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testGildedRank() {
        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(1399);
        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(CollectionNotifier.COMPLETED_LOGS_VARP);
        initCompleted.setValue(1399);
        notifier.onVarPlayer(initCompleted);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        mockItem(ItemID.SEERCULL, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGANNOTH_SUPREME);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.SEERCULL, 1))));

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1400, TOTAL_ENTRIES, "Gilded", null, null, source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNoRank() {
        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(0);
        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(CollectionNotifier.COMPLETED_LOGS_VARP);
        initCompleted.setValue(0);
        notifier.onVarPlayer(initCompleted);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        mockItem(ItemID.SEERCULL, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGANNOTH_SUPREME);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.SEERCULL, 1))));

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES, "None", 99, "Bronze", source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyPopup() {
        // prepare item
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // update mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(3);
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT)).thenReturn("Collection log");
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT)).thenReturn("New item:<br>" + item);

        // send chat event (to be ignored)
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification yet
        Mockito.verifyNoInteractions(messageHandler);

        // send script events
        notifier.onScript(ScriptID.NOTIFICATION_START);
        notifier.onScript(ScriptID.NOTIFICATION_DELAY);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES, "None", 99, "Bronze", null, null, null, null))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyFresh() {
        notifier.reset();

        /*
         * first notification: no varbit data
         */
        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_POSSIBLE_LOGS_VARP)).thenReturn(0);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, null, null, "None", 1, "None", null, null, null, null))
                .type(NotificationType.COLLECTION)
                .build()
        );

        /*
         * jagex sends varbit data shortly after the notification
         */
        BiFunction<Integer, Integer, VarbitChanged> varpEvent = (id, value) -> {
            VarbitChanged e = new VarbitChanged();
            e.setVarpId(id);
            e.setValue(value);
            return e;
        };

        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(1);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.COMPLETED_LOGS_VARP, 1));

        when(client.getVarpValue(CollectionNotifier.TOTAL_POSSIBLE_LOGS_VARP)).thenReturn(TOTAL_ENTRIES);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.TOTAL_POSSIBLE_LOGS_VARP, TOTAL_ENTRIES));

        when(client.getVarpValue(CollectionNotifier.COMPLETED_LOGS_VARP)).thenReturn(100);
        notifier.onVarPlayer(varpEvent.apply(CollectionNotifier.COMPLETED_LOGS_VARP, 100));

        notifier.onTick();

        /*
         * a later notification occurs
         */
        String item2 = "Seers ring";
        int price2 = 420_000;
        when(itemSearcher.findItemId(item2)).thenReturn(ItemID.SEERS_RING);
        when(itemManager.getItemPrice(ItemID.SEERS_RING)).thenReturn(price2);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item2);

        // verify handled
        verifyCreateMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item2))
                        .build()
                )
                .extra(new CollectionNotificationData(item2, ItemID.SEERS_RING, (long) price2, 101, TOTAL_ENTRIES, "Bronze", 199, "Iron", null, null, null, null))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send unrelated message
        notifier.onChatMessage("New item added to your backpack: weed");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyCollectionLog()).thenReturn(false);

        // send fake message
        String item = "Seercull";
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
