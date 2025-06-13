package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.domain.SeasonalPolicy;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Slf4j
@Singleton
public class WorldTypeTracker {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    protected ClientThread clientThread;

    private volatile Boolean validWorld = null;

    public void init() {
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                this.checkValidity();
            }
        });
    }

    public void clear() {
        this.validWorld = null;
    }

    public void onWorldChange() {
        this.checkValidity();
    }

    public void onConfig(String configKey) {
        if ("seasonalPolicy".equals(configKey)) {
            this.clear();
            this.init();
        }
    }

    public boolean worldPassesConfig() {
        var valid = this.validWorld;
        if (valid == null) {
            checkValidity();
            return this.validWorld;
        }
        return valid;
    }

    private void checkValidity() {
        Set<WorldType> world = client.getWorldType();
        if (config.seasonalPolicy() == SeasonalPolicy.REJECT && world.contains(WorldType.SEASONAL)) {
            this.validWorld = false;
        } else {
            this.validWorld = !WorldUtils.isIgnoredWorld(world);
        }
    }

}
