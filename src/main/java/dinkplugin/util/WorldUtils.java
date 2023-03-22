package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

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

    public boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public boolean isPvpWorld(Set<WorldType> worldType) {
        return worldType.contains(WorldType.PVP) || worldType.contains(WorldType.DEADMAN);
    }

    public boolean isPvpSafeZone(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PVP_WORLD_SAFE_ZONE);
        return widget != null && !widget.isHidden();
    }

    public boolean isBarbarianAssault(int regionId) {
        return BA_REGIONS.contains(regionId);
    }

    public boolean isBurthorpeGameRoom(int regionId) {
        return regionId == BURTHORPE_REGION;
    }

    public boolean isCastleWars(int regionId) {
        return CASTLE_WARS_REGIONS.contains(regionId);
    }

    public boolean isClanWars(int regionId) {
        return CLAN_WARS_REGIONS.contains(regionId);
    }

    public boolean isInferno(int regionId) {
        return regionId == INFERNO_REGION;
    }

    public boolean isLastManStanding(Client client) {
        if (LMS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))
            return true;

        Widget widget = client.getWidget(WidgetInfo.LMS_KDA);
        return widget != null && !widget.isHidden();
    }

    public boolean isMageTrainingArena(int regionId) {
        return MAGE_TRAIN_REGIONS.contains(regionId);
    }

    public boolean isNightmareZone(int regionId) {
        return regionId == NMZ_REGION;
    }

    public boolean isPestControl(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PEST_CONTROL_BLUE_SHIELD);
        return widget != null && !widget.isHidden();
    }

    public boolean isPlayerOwnedHouse(int regionId) {
        return POH_REGIONS.contains(regionId);
    }

    public boolean isSafeArea(Client client) {
        int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
        return isCastleWars(regionId) || isPestControl(client) || isPlayerOwnedHouse(regionId) ||
            isLastManStanding(client) || isSoulWars(regionId) || isBarbarianAssault(regionId) ||
            isNightmareZone(regionId) || isInferno(regionId) || isClanWars(regionId) ||
            isTzHaar(regionId) || isFishingTrawler(regionId) || isMageTrainingArena(regionId) ||
            isSorceressGarden(regionId) || isTitheFarm(regionId) || isBurthorpeGameRoom(regionId);
    }

    public boolean isSorceressGarden(int regionId) {
        return regionId == SORCERESS_REGION;
    }

    public boolean isSoulWars(int regionId) {
        return SOUL_REGIONS.contains(regionId);
    }

    public boolean isFishingTrawler(int regionId) {
        return regionId == TRAWLER_REGION;
    }

    public boolean isTitheFarm(int regionId) {
        return regionId == TITHE_REGION;
    }

    public boolean isTzHaar(int regionId) {
        return TZHAAR_REGIONS.contains(regionId);
    }

}
