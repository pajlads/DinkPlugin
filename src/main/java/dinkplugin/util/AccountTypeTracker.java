package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import dinkplugin.SettingsManager;
import dinkplugin.domain.AccountType;
import dinkplugin.domain.FilterMode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class AccountTypeTracker {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private DinkPluginConfig config;

    @Inject
    private SettingsManager settingsManager;

    private volatile AccountType accountType;

    public void init() {
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                this.accountType = AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
            }
        });
    }

    public void clear() {
        this.accountType = null;
    }

    public void onVarbit(VarbitChanged event) {
        if (event.getVarbitId() == VarbitID.IRONMAN) {
            this.accountType = AccountType.get(event.getVarbitId());
        }
    }

    public void onAccountChange() {
        this.clear();
        this.init();
    }

    public void onConfig(String configKey) {
        if ("nameFilterMode".equals(configKey) || "deniedAccountTypes".equals(configKey) || "ignoredNames".equals(configKey)) {
            this.clear();
            this.init();
        }
    }

    /**
     * @return whether the player name passes the configured filtered RSNs and the account type is permitted
     */
    public boolean accountPassesConfig() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        var accountType = this.accountType;
        if (accountType == null) {
            log.warn("Encountered null account type for non-null player!");
            return false;
        }
        if (config.nameFilterMode() != FilterMode.ALLOW && config.deniedAccountTypes().contains(accountType)) {
            log.info("Skipping notification due to denied account type");
            return false;
        }
        return settingsManager.isNamePermitted(player.getName());
    }

}
