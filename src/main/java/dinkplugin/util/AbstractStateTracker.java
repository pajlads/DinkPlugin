package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;

public abstract class AbstractStateTracker<T> {

    @Inject
    protected DinkPluginConfig config;

    @Inject
    protected Client client;

    @Inject
    protected ClientThread clientThread;

    protected volatile T state;

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

    protected void refresh() {
        this.clear();
        this.init();
    }

    protected abstract void populateState();

}
