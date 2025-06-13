package dinkplugin.util;

import dinkplugin.SettingsManager;
import dinkplugin.domain.AccountType;
import dinkplugin.domain.FilterMode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class AccountTypeTracker extends AbstractStateTracker<AccountType> {

    @Inject
    private SettingsManager settingsManager;

    @Override
    protected void populateState() {
        this.state = AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
    }

    public void onVarbit(VarbitChanged event) {
        if (event.getVarbitId() == VarbitID.IRONMAN) {
            this.state = AccountType.get(event.getVarbitId());
        }
    }

    public void onAccountChange() {
        this.refresh();
    }

    public void onConfig(String configKey) {
        if ("nameFilterMode".equals(configKey) || "deniedAccountTypes".equals(configKey) || "ignoredNames".equals(configKey)) {
            this.refresh();
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
        var accountType = this.state;
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
