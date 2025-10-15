package dinkplugin.util;

import dinkplugin.domain.SeasonalPolicy;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;

import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
public class WorldTypeTracker extends BooleanStateTracker {

    @Override
    protected void populateState() {
        Set<WorldType> world = client.getWorldType();
        var seasonalPolicy = config.seasonalPolicy();
        if (WorldUtils.isGridMaster(client)) {
            this.state = seasonalPolicy != SeasonalPolicy.REJECT;
        } else if (seasonalPolicy == SeasonalPolicy.REJECT && world.contains(WorldType.SEASONAL)) {
            this.state = false;
        } else if (WorldUtils.isIgnoredWorld(world)) {
            this.state = false;
        } else {
            this.state = true;
        }
        log.debug("Initialized world tracker for {}: {}", world, state);
    }

    public void onWorldChange() {
        this.refresh();
    }

    public void onConfig(String configKey) {
        if ("seasonalPolicy".equals(configKey)) {
            this.refresh();
        }
    }

}
