package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
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
    protected SettingsManager settingsManager;

    @Inject
    protected Client client;

    @Inject
    protected ClientThread clientThread;

    protected volatile Boolean state;

    public void init() {
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN && !settingsManager.justLoggedIn()) {
                this.populateState();
            }
        });
    }

    public void clear() {
        this.state = null;
    }

    public boolean hasValidState() {
        Boolean valid = this.state;
        return valid != null && valid;
    }

    public void onTick() {
        if (this.state == null && !settingsManager.justLoggedIn()) {
            populateState();
        }
    }

    protected void refresh() {
        this.clear();
        this.init();
    }

    protected abstract void populateState();

}
