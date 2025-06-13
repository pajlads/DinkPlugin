package dinkplugin.util;

import dinkplugin.domain.SeasonalPolicy;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;

import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
public class WorldTypeTracker extends AbstractBoolTracker {

    @Override
    protected void populateState() {
        Set<WorldType> world = client.getWorldType();
        if (config.seasonalPolicy() == SeasonalPolicy.REJECT && world.contains(WorldType.SEASONAL)) {
            this.state = false;
        } else {
            this.state = !WorldUtils.isIgnoredWorld(world);
        }
    }

    public void onWorldChange() {
        this.init();
    }

    public void onConfig(String configKey) {
        if ("seasonalPolicy".equals(configKey)) {
            this.refresh();
        }
    }

}
