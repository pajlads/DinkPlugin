package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@UtilityClass
public class WorldUtils {

    private final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private final Set<Integer> LMS_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
    private final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7513, 7514, 7769, 7770, 8025, 8026);
    private final Set<Integer> SOUL_REGIONS = ImmutableSet.of(8493, 8749, 9005);
    private final int CASTLE_WARS_REGION = 9520;

    @VisibleForTesting
    public final int CASTLE_WARS_COUNTDOWN = 380;
    @Varbit
    private final int CASTLE_WARS_X_OFFSET = 156;
    @Varbit
    private final int CASTLE_WARS_Y_OFFSET = 157;

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

    public boolean isCastleWars(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == CASTLE_WARS_REGION &&
            (client.getVarpValue(CASTLE_WARS_COUNTDOWN) > 0 || client.getVarbitValue(CASTLE_WARS_X_OFFSET) > 0 || client.getVarbitValue(CASTLE_WARS_Y_OFFSET) > 0);
    }

    public boolean isLastManStanding(Client client) {
        if (LMS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID()))
            return true;

        Widget widget = client.getWidget(WidgetInfo.LMS_KDA);
        return widget != null && !widget.isHidden();
    }

    public boolean isPestControl(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PEST_CONTROL_BLUE_SHIELD);
        return widget != null;
    }

    public boolean isPlayerOwnedHouse(Client client) {
        return POH_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public boolean isSafeArea(Client client) {
        return isCastleWars(client) || isPestControl(client) || isPlayerOwnedHouse(client);
    }

    public boolean isSoulWars(Client client) {
        return SOUL_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

}
