package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.CollectionLogRank;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.CollectionNotificationData;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.KillCountService;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.ScriptID;
import net.runelite.api.StructComposition;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        Thread thread = Thread.currentThread();
        Mockito.doAnswer(invocation -> Thread.currentThread() == thread).when(client).isClientThread();

        when(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM)).thenReturn(1);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(TOTAL_ENTRIES);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        setCompleted(0);

        final int[] CLOG_THRESHOLDS = {0, 100, 300, 500, 700, 900, 1000, 1100, 1200, 1300, 1400};
        CollectionLogRank[] ranks = CollectionLogRank.values();
        for (int i = 0; i < ranks.length; i++) {
            CollectionLogRank rank = ranks[i];
            StructComposition s = mock(StructComposition.class);
            when(s.getIntValue(CollectionLogRank.THRESHOLD_PARAM)).thenReturn(CLOG_THRESHOLDS[i]);
            when(client.getStructComposition(rank.getStructId())).thenReturn(s);
        }
        notifier.onTick();

        // init config mocks
        when(config.notifyCollectionLog()).thenReturn(true);
        when(config.collectionSendImage()).thenReturn(false);
        when(config.collectionNotifyMessage()).thenReturn("%USERNAME% has added %ITEM% to their collection");
    }

    @Test
    void testNotify() {
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        mockItem(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGCAVE_RANGED_BOSS);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, 1))));

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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, 1, TOTAL_ENTRIES, CollectionLogRank.NONE, 1, 99, CollectionLogRank.BRONZE, null, source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testRankUp() {
        setCompleted(299);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        mockItem(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGCAVE_RANGED_BOSS);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, 1))));

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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, 300, TOTAL_ENTRIES, CollectionLogRank.IRON, 0, 200, CollectionLogRank.STEEL, CollectionLogRank.BRONZE, source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testGildedRank() {
        setCompleted(1424);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        mockItem(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGCAVE_RANGED_BOSS);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, 1))));

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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, 1425, TOTAL_ENTRIES, CollectionLogRank.GILDED, 0, null, null, CollectionLogRank.DRAGON, source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNoRank() {
        setCompleted(0);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        mockItem(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, price, item);

        // prepare kc
        int kc = 150;
        double rarity = 1.0 / 128;
        String source = "Dagannoth Supreme";
        NPC npc = mock(NPC.class);
        when(npc.getName()).thenReturn(source);
        when(npc.getId()).thenReturn(NpcID.DAGCAVE_RANGED_BOSS);
        when(configManager.getRSProfileConfiguration("killcount", source.toLowerCase(), int.class)).thenReturn(kc);
        killCountService.onNpcKill(new NpcLootReceived(npc, List.of(new ItemStack(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, 1))));

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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, 1, TOTAL_ENTRIES, CollectionLogRank.NONE, 1, 99, CollectionLogRank.BRONZE, null, source, LootRecordType.NPC, kc, rarity))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyPopup() {
        // prepare item
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        when(itemManager.getItemPrice(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW)).thenReturn(price);

        // update mocks
        when(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM)).thenReturn(3);
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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, 1, TOTAL_ENTRIES, CollectionLogRank.NONE, 1, 99, CollectionLogRank.BRONZE, null, null,null, null, null))
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
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(0);
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(0);

        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW);
        when(itemManager.getItemPrice(ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW)).thenReturn(price);

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
                .extra(new CollectionNotificationData(item, ItemID.DAGANOTH_CAVE_MAGIC_SHORTBOW, (long) price, null, null, null, null, null, null, null, null,null, null, null))
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

        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(1);
        notifier.onVarPlayer(varpEvent.apply(VarPlayerID.COLLECTION_COUNT, 1));

        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX)).thenReturn(TOTAL_ENTRIES);
        notifier.onVarPlayer(varpEvent.apply(VarPlayerID.COLLECTION_COUNT_MAX, TOTAL_ENTRIES));

        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(100);
        notifier.onVarPlayer(varpEvent.apply(VarPlayerID.COLLECTION_COUNT, 100));

        notifier.onTick();

        /*
         * a later notification occurs
         */
        String item2 = "Seers ring";
        int price2 = 420_000;
        when(itemSearcher.findItemId(item2)).thenReturn(ItemID.SEER_RING);
        when(itemManager.getItemPrice(ItemID.SEER_RING)).thenReturn(price2);

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
                .extra(new CollectionNotificationData(item2, ItemID.SEER_RING, (long) price2, 101, TOTAL_ENTRIES, CollectionLogRank.BRONZE, 1, 199, CollectionLogRank.IRON, null, null,null, null, null))
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

    private void setCompleted(int clogCount) {
        when(client.getVarpValue(VarPlayerID.COLLECTION_COUNT)).thenReturn(clogCount);
        when(config.notifyCollectionLog()).thenReturn(false);
        VarbitChanged initCompleted = new VarbitChanged();
        initCompleted.setVarpId(VarPlayerID.COLLECTION_COUNT);
        initCompleted.setValue(clogCount);
        notifier.onVarPlayer(initCompleted);
        when(config.notifyCollectionLog()).thenReturn(true);
    }

}
