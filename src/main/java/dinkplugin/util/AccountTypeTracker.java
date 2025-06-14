package dinkplugin.util;

import dinkplugin.domain.AccountType;
import dinkplugin.domain.FilterMode;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class AccountTypeTracker extends BooleanStateTracker {

    @Override
    protected void populateState() {
        Player player = client.getLocalPlayer();
        var accountType = AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
        if (player == null || player.getName() == null || accountType == null) {
            this.state = null;
            return;
        } else if (config.nameFilterMode() != FilterMode.ALLOW && config.deniedAccountTypes().contains(accountType)) {
            this.state = false;
        } else {
            this.state = settingsManager.isNamePermitted(player.getName());
        }
        log.debug("Initialized account tracker for {} {}: {}", accountType, player.getName(), this.state);
    }

    public void onVarbit(VarbitChanged event) {
        if (event.getVarbitId() == VarbitID.IRONMAN) {
            this.refresh();
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

}
