package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;

@Slf4j
public abstract class AbstractBoolTracker {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    protected ClientThread clientThread;

    protected volatile Boolean state;

    public void init() {
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                this.populateState();
            }
        });
    }

    public void clear() {
        this.state = null;
    }

    public boolean hasValidState() {
        var valid = this.state;
        if (valid == null) {
            this.init();
            if ((valid = this.state) == null) {
                log.warn("{} was not initialized before notification attempt", getClass().getSimpleName());
                return false;
            }
        }
        return valid;
    }

    public void onTick() {
        if (this.state == null) {
            populateState();
        }
    }

    protected void refresh() {
        this.clear();
        this.init();
    }

    protected abstract void populateState();

}
