package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.SeasonalPolicy;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
public class WorldTypeTracker extends AbstractStateTracker<Boolean> {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    protected ClientThread clientThread;

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
        this.populateState();
    }

    public void onConfig(String configKey) {
        if ("seasonalPolicy".equals(configKey)) {
            this.refresh();
        }
    }

    public boolean worldPassesConfig() {
        var valid = this.state;
        if (valid == null) {
            populateState();
            return this.state;
        }
        return valid;
    }

}
