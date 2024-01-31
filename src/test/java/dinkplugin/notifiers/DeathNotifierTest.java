package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.AccountType;
import dinkplugin.domain.ExceptionalDeath;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.DeathNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Region;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcID;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeathNotifierTest extends MockedNotifierTest {

    private static final int RUBY_PRICE = 900;
    private static final int SHARK_PRICE = 700;
    private static final int OPAL_PRICE = 600;
    private static final int COAL_PRICE = 200;
    private static final int TUNA_PRICE = 100;

    @Bind
    @InjectMocks
    DeathNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyDeath()).thenReturn(true);
        when(config.deathNotifPvpEnabled()).thenReturn(true);
        when(config.deathSendImage()).thenReturn(false);
        when(config.deathEmbedKeptItems()).thenReturn(true);
        when(config.deathIgnoreSafe()).thenReturn(true);
        when(config.deathNotifyMessage()).thenReturn("%USERNAME% has died, losing %VALUELOST% gp");
        when(config.deathNotifPvpMessage()).thenReturn("%USERNAME% has been PKed by %PKER% for %VALUELOST% gp");

        // init client mocks
        when(client.getVarbitValue(Varbits.IN_WILDERNESS)).thenReturn(1);
        when(client.getCachedPlayers()).thenReturn(new Player[0]);
        when(client.getCachedNPCs()).thenReturn(new NPC[0]);
        WorldPoint location = new WorldPoint(0, 0, 0);
        when(localPlayer.getWorldLocation()).thenReturn(location);
        when(localPlayer.getLocalLocation()).thenReturn(new LocalPoint(0, 0));

        // init item mocks
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.SHARK, SHARK_PRICE, "Shark");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.COAL, COAL_PRICE, "Coal");
        mockItem(ItemID.TUNA, TUNA_PRICE, "Tuna");

        // init npc mocks
        when(npcManager.getHealth(anyInt())).thenReturn(50);
    }

    @Test
    void testNotifyEmpty() {
        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, Collections.emptyList(), Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotify() {
        // prepare mocks
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.asList(
            new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"),
            new SerializedItemStack(ItemID.SHARK, 1, SHARK_PRICE, "Shark"),
            new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal"),
            new SerializedItemStack(ItemID.COAL, 1, COAL_PRICE, "Coal")
        );
        List<SerializedItemStack> lost = Collections.singletonList(
            new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")
        );
        List<Embed> embeds = Arrays.asList(
            Embed.ofImage("https://static.runelite.net/cache/item/icon/" + ItemID.RUBY + ".png"),
            Embed.ofImage("https://static.runelite.net/cache/item/icon/" + ItemID.SHARK + ".png"),
            Embed.ofImage("https://static.runelite.net/cache/item/icon/" + ItemID.OPAL + ".png"),
            Embed.ofImage("https://static.runelite.net/cache/item/icon/" + ItemID.COAL + ".png")
        );
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, TUNA_PRICE)))
                .extra(new DeathNotificationData(TUNA_PRICE, false, null, null, null, kept, lost, Region.of(client)))
                .type(NotificationType.DEATH)
                .embeds(embeds)
                .build()
        );
    }

    @Test
    void testNotifyPk() {
        // prepare mocks
        String pker = "Rasmus";
        Player other = mock(Player.class);
        when(other.getName()).thenReturn(pker);
        when(other.getInteracting()).thenReturn(localPlayer);
        Player[] candidates = { mock(Player.class), mock(Player.class), other, mock(Player.class) };
        when(client.getCachedPlayers()).thenReturn(candidates);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has been PKed by %s for %d gp", PLAYER_NAME, pker, 0)))
                .extra(new DeathNotificationData(0L, true, pker, pker, null, Collections.emptyList(), Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyPkInteraction() {
        // prepare mocks
        String pker = "Rasmus";
        Player other = mock(Player.class);
        when(other.getName()).thenReturn(pker);
        when(other.getInteracting()).thenReturn(localPlayer);
        when(other.getCombatLevel()).thenReturn(50);
        when(localPlayer.getCombatLevel()).thenReturn(50);

        // fire events
        plugin.onInteractingChanged(new InteractingChanged(other, localPlayer));
        plugin.onInteractingChanged(new InteractingChanged(localPlayer, other));
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has been PKed by %s for %d gp", PLAYER_NAME, pker, 0)))
                .extra(new DeathNotificationData(0L, true, pker, pker, null, Collections.emptyList(), Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyNotPk() {
        // prepare mocks: look like PK, but not in wilderness
        Player other = mock(Player.class);
        when(other.getName()).thenReturn("Rasmus");
        when(other.getInteracting()).thenReturn(localPlayer);
        when(client.getCachedPlayers()).thenReturn(new Player[] { other });
        when(client.getVarbitValue(Varbits.IN_WILDERNESS)).thenReturn(0);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify non-PK notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, Collections.emptyList(), Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyNpc() {
        // init mocks
        String name = "Guard";
        NPC other = mock(NPC.class);
        when(other.getName()).thenReturn(name);
        when(other.getId()).thenReturn(NpcID.GUARD);
        when(other.isDead()).thenReturn(false);
        when(other.getCombatLevel()).thenReturn(21);
        when(other.getInteracting()).thenReturn(localPlayer);
        when(other.getLocalLocation()).thenReturn(new LocalPoint(1, 1));

        NPCComposition comp = mock(NPCComposition.class);
        when(other.getTransformedComposition()).thenReturn(comp);
        when(comp.isInteractible()).thenReturn(true);
        when(comp.isFollower()).thenReturn(false);
        when(comp.getSize()).thenReturn(1);
        when(comp.isMinimapVisible()).thenReturn(true);
        when(comp.getId()).thenReturn(NpcID.GUARD);
        when(comp.getName()).thenReturn(name);
        when(comp.getStringValue(ParamID.NPC_HP_NAME)).thenReturn(name);
        when(comp.getCombatLevel()).thenReturn(21);
        when(comp.getActions()).thenReturn(new String[] { "Pickpocket", "Attack", "Examine" });

        when(npcManager.getHealth(NpcID.GUARD)).thenReturn(22);
        when(client.getCachedNPCs()).thenReturn(new NPC[] { other });
        when(config.deathNotifyMessage()).thenReturn("%USERNAME% has died to %NPC%");

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has died to {{npc}}")
                        .replacement("{{npc}}", Replacements.ofWiki(name))
                        .build()
                )
                .extra(new DeathNotificationData(0L, false, null, name, NpcID.GUARD, Collections.emptyList(), Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyValue() {
        // prepare mocks
        when(config.deathMinValue()).thenReturn(TUNA_PRICE - 1);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.asList(
            ItemUtils.stackFromItem(itemManager, items[0]),
            ItemUtils.stackFromItem(itemManager, items[3]),
            ItemUtils.stackFromItem(itemManager, items[4]),
            ItemUtils.stackFromItem(itemManager, items[2])
        );
        List<SerializedItemStack> lost = Collections.singletonList(
            ItemUtils.stackFromItem(itemManager, items[1])
        );
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, TUNA_PRICE)))
                .extra(new DeathNotificationData(TUNA_PRICE, false, null, null, null, kept, lost, Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifySafe() {
        // update config mock
        when(config.deathIgnoreSafe()).thenReturn(false);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(config.deathMinValue()).thenReturn(TUNA_PRICE + 1);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // mock castle wars
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(2400, 3100, 0));

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.stream(items)
            .map(i -> ItemUtils.stackFromItem(itemManager, i))
            .sorted(Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed())
            .collect(Collectors.toList());
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, kept, Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyAmascut() {
        // update mocks
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3520, 5120, 0));
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        notifier.onGameMessage("You failed to survive the Tombs of Amascut");

        // verify notification
        List<SerializedItemStack> kept = Arrays.asList(
            ItemUtils.stackFromItem(itemManager, items[0]),
            ItemUtils.stackFromItem(itemManager, items[3]),
            ItemUtils.stackFromItem(itemManager, items[4]),
            ItemUtils.stackFromItem(itemManager, items[2])
        );
        List<SerializedItemStack> lost = Collections.singletonList(
            ItemUtils.stackFromItem(itemManager, items[1])
        );
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, TUNA_PRICE)))
                .extra(new DeathNotificationData(TUNA_PRICE, false, null, null, null, kept, lost, Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyAmascutSafe() {
        // update mocks
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3520, 5120, 0));
        when(config.deathIgnoreSafe()).thenReturn(false);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.stream(items)
            .map(i -> ItemUtils.stackFromItem(itemManager, i))
            .sorted(Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed())
            .collect(Collectors.toList());
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, kept, Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyAmascutExceptional() {
        // update mocks
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3520, 5120, 0));
        when(config.deathIgnoreSafe()).thenReturn(true);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(config.deathSafeExceptions()).thenReturn(EnumSet.of(ExceptionalDeath.TOA));
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.stream(items)
            .map(i -> ItemUtils.stackFromItem(itemManager, i))
            .sorted(Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed())
            .collect(Collectors.toList());
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, kept, Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );

        // fire event
        notifier.onGameMessage("You failed to survive the Tombs of Amascut");

        // ensure toa death message is ignored in exceptional mode (since a notification already fired)
        Mockito.verifyNoMoreInteractions(messageHandler);
    }

    @Test
    void testNotifyAmascutHardcoreIron() {
        // update mocks
        when(client.getVarbitValue(Varbits.ACCOUNT_TYPE)).thenReturn(AccountType.HARDCORE_IRONMAN.ordinal());
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3520, 5120, 0));
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.asList(
            ItemUtils.stackFromItem(itemManager, items[0]),
            ItemUtils.stackFromItem(itemManager, items[3]),
            ItemUtils.stackFromItem(itemManager, items[4]),
            ItemUtils.stackFromItem(itemManager, items[2])
        );
        List<SerializedItemStack> lost = Collections.singletonList(
            ItemUtils.stackFromItem(itemManager, items[1])
        );
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, TUNA_PRICE)))
                .extra(new DeathNotificationData(TUNA_PRICE, false, null, null, null, kept, lost, Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );

        // ensure TOA death chat message is ignored for HCIM
        notifier.onGameMessage("You failed to survive the Tombs of Amascut");
        Mockito.verifyNoMoreInteractions(messageHandler);
    }

    @Test
    void testIgnoreAmascut() {
        // update mocks
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3520, 5120, 0));
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyGauntletSafe() {
        // update mocks
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(1950, 5650, 0));
        when(config.deathIgnoreSafe()).thenReturn(false);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> kept = Arrays.stream(items)
            .map(i -> ItemUtils.stackFromItem(itemManager, i))
            .sorted(Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed())
            .collect(Collectors.toList());
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, 0)))
                .extra(new DeathNotificationData(0L, false, null, null, null, kept, Collections.emptyList(), Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testNotifyGauntletHardcore() {
        // update mocks
        when(client.getVarbitValue(Varbits.ACCOUNT_TYPE)).thenReturn(3);
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(1950, 5650, 0));
        when(config.deathIgnoreSafe()).thenReturn(true);
        when(config.deathEmbedKeptItems()).thenReturn(false);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // verify notification
        List<SerializedItemStack> lost = Collections.singletonList(ItemUtils.stackFromItem(itemManager, items[0]));
        List<SerializedItemStack> kept = Arrays.stream(items)
            .skip(1) // tuna is lost (least valuable)
            .map(i -> ItemUtils.stackFromItem(itemManager, i))
            .sorted(Comparator.comparingLong(SerializedItemStack::getTotalPrice).reversed())
            .collect(Collectors.toList());
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has died, losing %d gp", PLAYER_NAME, TUNA_PRICE)))
                .extra(new DeathNotificationData(TUNA_PRICE, false, null, null, null, kept, lost, Region.of(client)))
                .type(NotificationType.DEATH)
                .build()
        );
    }

    @Test
    void testIgnoreGauntlet() {
        // mock corrupted gauntlet
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(1950, 5650, 0));

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnore() {
        // prepare mock
        Player other = mock(Player.class);

        // fire event
        plugin.onActorDeath(new ActorDeath(other));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreSafe() {
        // mock castle wars
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(2400, 3100, 0));

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreCustom() {
        // update config mocks
        when(config.deathIgnoredRegions()).thenReturn("12336");
        notifier.init();

        // mock tutorial island
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3100, 3100, 0));

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyDeath()).thenReturn(false);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreValue() {
        // prepare mocks
        when(config.deathMinValue()).thenReturn(TUNA_PRICE + 1);
        when(client.isPrayerActive(Prayer.PROTECT_ITEM)).thenReturn(true);
        Item[] items = {
            new Item(ItemID.RUBY, 1),
            new Item(ItemID.TUNA, 1),
            new Item(ItemID.COAL, 1),
            new Item(ItemID.SHARK, 1),
            new Item(ItemID.OPAL, 1),
        };
        ItemContainer inv = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(inv);
        when(inv.getItems()).thenReturn(items);

        // fire event
        plugin.onActorDeath(new ActorDeath(localPlayer));

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
