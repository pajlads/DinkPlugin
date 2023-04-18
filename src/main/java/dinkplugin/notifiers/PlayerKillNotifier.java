package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.PkNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.WorldUtils;
import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
public class PlayerKillNotifier extends BaseNotifier {

    private static final KitType[] EQUIPMENT;

    /**
     * Contains the damage inflicted by the local player on various targets in a short time period.
     * <p>
     * Uses weak keys to not hinder garbage collection
     * (and avoid synchronization if we were forced to do Map#clear on plugin shutdown).
     */
    private final Map<Player, Integer> attacked = new WeakHashMap<>(4);

    @Inject
    private ItemManager itemManager;

    @Override
    public boolean isEnabled() {
        if (!config.notifyPk())
            return false;

        // duplicated logic from super class but allow Duel Arena
        EnumSet<WorldType> world = client.getWorldType().clone(); // fast on RegularEnumSet
        world.remove(WorldType.PVP_ARENA);
        return !WorldUtils.isIgnoredWorld(world) && settingsManager.isNamePermitted(client.getLocalPlayer().getName());
    }

    @Override
    protected String getWebhookUrl() {
        return config.pkWebhook();
    }

    public void onHitsplat(HitsplatApplied event) {
        Hitsplat hit = event.getHitsplat();
        int amount = hit.getAmount();
        if (amount <= 0 || !hit.isMine())
            return;

        if (!config.notifyPk())
            return;

        Actor actor = event.getActor();
        if (actor == client.getLocalPlayer() || !(actor instanceof Player))
            return;

        Player target = (Player) actor;
        attacked.merge(target, amount, Integer::sum);
    }

    public void onTick() {
        // micro-optimization: this check is very fast for empty WeakHashMap & can avoid creating a HashIterator
        if (attacked.isEmpty())
            return;

        attacked.forEach((target, damage) -> {
            if (target.isDead())
                handleKill(target, damage);
        });

        attacked.clear();
    }

    private void handleKill(Player target, int myLastDamage) {
        if (!isEnabled())
            return;

        if (config.pkSkipFriendly() && isFriendly(target))
            return;

        if (config.pkSkipSafe() && (WorldUtils.isSafeArea(client) || client.getWorldType().contains(WorldType.PVP_ARENA)))
            return;

        Map<KitType, SerializedItemStack> equipment = getEquipment(target.getPlayerComposition());
        long value = ItemUtils.getTotalPrice(equipment.values());
        long minValue = config.pkMinValue();
        if (value < minValue)
            return;

        PkNotificationData extra = new PkNotificationData(
            target.getName(),
            target.getCombatLevel(),
            equipment,
            client.getWorld(),
            target.getWorldLocation(),
            client.getBoostedSkillLevel(Skill.HITPOINTS),
            myLastDamage
        );

        String localPlayer = client.getLocalPlayer().getName();
        String message = config.pkNotifyMessage()
            .replace("%USERNAME%", localPlayer)
            .replace("%TARGET%", target.getName());

        createMessage(config.pkSendImage(), NotificationBody.builder()
            .type(NotificationType.PLAYER_KILL)
            .text(message)
            .extra(extra)
            .playerName(localPlayer)
            .build());
    }

    private boolean isFriendly(Player target) {
        return target.isFriend() || target.isClanMember() || target.isFriendsChatMember()
            || (target.getTeam() != 0 && target.getTeam() == client.getLocalPlayer().getTeam());
    }

    private Map<KitType, SerializedItemStack> getEquipment(PlayerComposition comp) {
        if (comp == null) return Collections.emptyMap();

        int[] equipmentIds = comp.getEquipmentIds();
        int n = equipmentIds.length;

        Map<KitType, SerializedItemStack> map = new EnumMap<>(KitType.class);
        for (KitType slot : EQUIPMENT) {
            int index = slot.getIndex();
            if (index >= n) continue;
            int id = equipmentIds[index];
            if (id >= 512) {
                SerializedItemStack item = ItemUtils.stackFromItem(itemManager, id - 512, 1);
                map.put(slot, item);
            }
        }
        return map;
    }

    static {
        // omits ARMS, HAIR, JAW because they don't correspond to items
        EQUIPMENT = new KitType[] {
            KitType.HEAD,
            KitType.CAPE,
            KitType.AMULET,
            KitType.WEAPON,
            KitType.TORSO,
            KitType.SHIELD,
            KitType.LEGS,
            KitType.HANDS,
            KitType.BOOTS
        };
    }
}
