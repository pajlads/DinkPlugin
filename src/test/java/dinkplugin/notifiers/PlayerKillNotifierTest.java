package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.PlayerLookupService;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.PlayerKillNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.WorldUtils;
import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.kit.KitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static net.runelite.api.HitsplatID.DAMAGE_OTHER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerKillNotifierTest extends MockedNotifierTest {

    private static final String TARGET = "Romy";
    private static final int LEVEL = 99;
    private static final int MY_HP = 10;
    private static final int WORLD = 420;
    private static final WorldPoint LOCATION = new WorldPoint(3000, 4000, 0);
    private static final int WEAPON_PRICE = 100_000;
    private static final int TOP_PRICE = 120_000;
    private static final int LEGS_PRICE = 80_000;
    private static final int HAND_PRICE = 100_000;
    private static final int SHIELD_PRICE = 200;
    private static final int EQUIPMENT_VALUE = WEAPON_PRICE + TOP_PRICE + LEGS_PRICE + HAND_PRICE + SHIELD_PRICE;
    private static final Map<KitType, SerializedItemStack> EQUIPMENT;

    @Bind
    @InjectMocks
    PlayerKillNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyPk()).thenReturn(true);
        when(config.pkMinValue()).thenReturn(EQUIPMENT_VALUE);
        when(config.pkIncludeLocation()).thenReturn(true);
        when(config.pkNotifyMessage()).thenReturn("%USERNAME% has PK'd %TARGET%");
        when(config.playerLookupService()).thenReturn(PlayerLookupService.NONE);

        // init client mocks
        when(client.getWorld()).thenReturn(WORLD);
        when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(MY_HP);

        // init item mocks
        for (SerializedItemStack item : EQUIPMENT.values()) {
            mockItem(item.getId(), item.getPriceEach(), item.getName());
        }
    }

    @Test
    void testNotify() {
        // init mocks
        when(config.playerLookupService()).thenReturn(PlayerLookupService.OSRS_HISCORE);
        Player target = mockPlayer();

        // fire event
        int damage = 12;
        notifier.onHitsplat(event(target, damage));
        notifier.onTick();

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " has PK'd {{target}}")
                        .replacement("{{target}}", Replacements.ofLink(TARGET, PlayerLookupService.OSRS_HISCORE.getPlayerUrl(TARGET)))
                        .build()
                )
                .type(NotificationType.PLAYER_KILL)
                .playerName(PLAYER_NAME)
                .extra(new PlayerKillNotificationData(TARGET, LEVEL, EQUIPMENT, WORLD, LOCATION, MY_HP, damage))
                .build()
        );
    }

    @Test
    void testNotifyMulti() {
        // init mocks
        Player target = mockPlayer();

        // fire events
        notifier.onHitsplat(event(target, 5));
        notifier.onHitsplat(event(target, 7));
        notifier.onTick();

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has PK'd %s", PLAYER_NAME, TARGET)))
                .type(NotificationType.PLAYER_KILL)
                .playerName(PLAYER_NAME)
                .extra(new PlayerKillNotificationData(TARGET, LEVEL, EQUIPMENT, WORLD, LOCATION, MY_HP, 12))
                .build()
        );
    }

    @Test
    void testNotifyMultiOther() {
        // init mocks
        Player target = mockPlayer();

        // fire event
        int damage = 12;
        notifier.onHitsplat(event(target, damage));
        HitsplatApplied eventOther = new HitsplatApplied();
        eventOther.setActor(target);
        eventOther.setHitsplat(new Hitsplat(DAMAGE_OTHER, 13, 1));
        notifier.onHitsplat(eventOther);
        notifier.onTick();

        // verify notification
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(buildTemplate(String.format("%s has PK'd %s", PLAYER_NAME, TARGET)))
                .type(NotificationType.PLAYER_KILL)
                .playerName(PLAYER_NAME)
                .extra(new PlayerKillNotificationData(TARGET, LEVEL, EQUIPMENT, WORLD, LOCATION, MY_HP, damage))
                .build()
        );
    }

    @Test
    void testIgnoreValue() {
        // init mocks
        Player target = mockPlayer();
        target.getPlayerComposition().getEquipmentIds()[KitType.SHIELD.getIndex()] = 0;

        // fire event
        notifier.onHitsplat(event(target, 13));
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreZero() {
        // init mocks
        Player target = mockPlayer();

        // fire event
        notifier.onHitsplat(event(target, 0));
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreSafe() {
        // init mocks
        Player target = mockPlayer();
        when(config.pkSkipSafe()).thenReturn(true);
        when(localPlayer.getWorldLocation()).thenReturn(
            WorldPoint.fromRegion(WorldUtils.TZHAAR_PIT, 0, 0, 0)
        );

        // fire event
        notifier.onHitsplat(event(target, 14));
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreFriendly() {
        // init mocks
        Player target = mockPlayer();
        when(config.pkSkipFriendly()).thenReturn(true);
        when(target.isFriend()).thenReturn(true);

        // fire event
        notifier.onHitsplat(event(target, 15));
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // init mocks
        Player target = mockPlayer();
        when(config.notifyPk()).thenReturn(false);

        // fire event
        notifier.onHitsplat(event(target, 16));
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreOther() {
        // init mocks
        Player target = mockPlayer();

        // fire event
        HitsplatApplied event = new HitsplatApplied();
        event.setActor(target);
        event.setHitsplat(new Hitsplat(DAMAGE_OTHER, 17, 1));
        notifier.onHitsplat(event);
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreSelf() {
        // fire event
        HitsplatApplied event = new HitsplatApplied();
        event.setActor(localPlayer);
        event.setHitsplat(new Hitsplat(HitsplatID.DAMAGE_ME, 18, 1));
        notifier.onHitsplat(event);
        notifier.onTick();

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private static Player mockPlayer() {
        Player target = mock(Player.class);
        when(target.getName()).thenReturn(TARGET);
        when(target.isDead()).thenReturn(true);
        when(target.getCombatLevel()).thenReturn(LEVEL);
        when(target.getWorldLocation()).thenReturn(LOCATION);
        PlayerComposition comp = mock(PlayerComposition.class);
        when(target.getPlayerComposition()).thenReturn(comp);
        int[] equipment = new int[KitType.values().length];
        EQUIPMENT.forEach((kit, item) -> equipment[kit.getIndex()] = item.getId() + 512);
        when(comp.getEquipmentIds()).thenReturn(equipment);
        return target;
    }

    private static HitsplatApplied event(Actor actor, int amount) {
        HitsplatApplied e = new HitsplatApplied();
        e.setActor(actor);
        e.setHitsplat(new Hitsplat(HitsplatID.DAMAGE_ME, amount, 1));
        return e;
    }

    static {
        Map<KitType, SerializedItemStack> m = new EnumMap<>(KitType.class);
        m.put(KitType.WEAPON, new SerializedItemStack(ItemID.ANCIENT_STAFF, 1, WEAPON_PRICE, "Ancient staff"));
        m.put(KitType.TORSO, new SerializedItemStack(ItemID.MYSTIC_ROBE_TOP, 1, TOP_PRICE, "Mystic robe top"));
        m.put(KitType.LEGS, new SerializedItemStack(ItemID.MYSTIC_ROBE_BOTTOM, 1, LEGS_PRICE, "Mystic robe bottom"));
        m.put(KitType.HANDS, new SerializedItemStack(ItemID.BARROWS_GLOVES, 1, HAND_PRICE, "Barrows gloves"));
        m.put(KitType.SHIELD, new SerializedItemStack(ItemID.UNHOLY_BOOK, 1, SHIELD_PRICE, "Unholy book"));
        EQUIPMENT = Collections.unmodifiableMap(m);
    }
}
