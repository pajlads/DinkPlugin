package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.annotations.Varp;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@UtilityClass
public class WorldUtils {

    private final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private final Set<Integer> BA_REGIONS = ImmutableSet.of(7508, 7509, 10322);
    private final Set<Integer> CASTLE_WARS_REGIONS = ImmutableSet.of(9520, 9620);
    private final Set<Integer> CLAN_WARS_REGIONS = ImmutableSet.of(12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390, 13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156);
    private final Set<Integer> LMS_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
    private final Set<Integer> MAGE_TRAIN_REGIONS = ImmutableSet.of(13462, 13463);
    private final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7513, 7514, 7769, 7770, 8025, 8026);
    private final Set<Integer> SOUL_REGIONS = ImmutableSet.of(8493, 8748, 8749, 9005);
    private final Set<Integer> TZHAAR_REGIONS = ImmutableSet.of(9551, 9552);
    private final int BURTHORPE_REGION = 8781;
    private final int INFERNO_REGION = 9043;
    private final int NMZ_REGION = 9033;
    private final int SORCERESS_REGION = 11605;
    private final int TITHE_REGION = 7222;
    private final int TRAWLER_REGION = 7499;

    @VisibleForTesting
    public final @Varp int CASTLE_WARS_COUNTDOWN = 380;
    private final @Varbit int CASTLE_WARS_X_OFFSET = 156;
    private final @Varbit int CASTLE_WARS_Y_OFFSET = 157;

    public boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public boolean isPvpWorld(Set<WorldType> worldType) {
        return worldType.contains(WorldType.PVP) || worldType.contains(WorldType.DEADMAN);
    }

    public boolean isPvpSafeZone(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PVP_WORLD_SAFE_ZONE);
        return widget != null && !widget.isSelfHidden();
    }

    public boolean isBarbarianAssault(Client client) {
        return BA_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isBurthorpeGameRoom(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == BURTHORPE_REGION;
    }

    public boolean isCastleWars(Client client) {
        return CASTLE_WARS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()) &&
            (client.getVarpValue(CASTLE_WARS_COUNTDOWN) > 0 || client.getVarbitValue(CASTLE_WARS_X_OFFSET) > 0 || client.getVarbitValue(CASTLE_WARS_Y_OFFSET) > 0);
    }

    public boolean isClanWars(Client client) {
        return CLAN_WARS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isInferno(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == INFERNO_REGION;
    }

    public boolean isLastManStanding(Client client) {
        if (LMS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))
            return true;

        Widget widget = client.getWidget(WidgetInfo.LMS_KDA);
        return widget != null && !widget.isHidden();
    }

    public boolean isMageTrainingArena(Client client) {
        return MAGE_TRAIN_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isNightmareZone(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == NMZ_REGION;
    }

    public boolean isPestControl(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PEST_CONTROL_BLUE_SHIELD);
        return widget != null;
    }

    public boolean isPlayerOwnedHouse(Client client) {
        return POH_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isSafeArea(Client client) {
        return isCastleWars(client) || isPestControl(client) || isPlayerOwnedHouse(client) ||
            isLastManStanding(client) || isSoulWars(client) || isBarbarianAssault(client) ||
            isNightmareZone(client) || isInferno(client) || isClanWars(client) ||
            isTzHaar(client) || isFishingTrawler(client) || isMageTrainingArena(client) ||
            isSorceressGarden(client) || isTitheFarm(client) || isBurthorpeGameRoom(client);
    }

    public boolean isSorceressGarden(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == SORCERESS_REGION;
    }

    public boolean isSoulWars(Client client) {
        return SOUL_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isFishingTrawler(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == TRAWLER_REGION;
    }

    public boolean isTitheFarm(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == TITHE_REGION;
    }

    public boolean isTzHaar(Client client) {
        return TZHAAR_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

}
