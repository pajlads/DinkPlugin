package dinkplugin;

import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = { @Inject })
class SettingsValidator {
    private static final String CONFIG_GROUP = "dinkplugin";

    private final Client client;
    private final ClientThread clientThread;
    private final DinkPlugin plugin;
    private final DinkPluginConfig config;

    void onConfigChanged(ConfigChanged event) {
        if (!CONFIG_GROUP.equals(event.getGroup()) || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        String key = event.getKey();
        String value = event.getNewValue();

        if ("combatTaskEnabled".equals(key) && "true".equals(value)) {
            clientThread.invokeLater(() -> {
                if (client.getVarbitValue(CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP) > 0) {
                    plugin.addChatWarning(CombatTaskNotifier.REPEAT_WARNING);
                }
            });
            return;
        }

        if ("collectionLogEnabled".equals(key) && "true".equals(value)) {
            clientThread.invokeLater(() -> {
                if (client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) % 2 != 1) {
                    plugin.addChatWarning(CollectionNotifier.ADDITION_WARNING);
                }
            });
        }
    }

    void onVarbitChanged(VarbitChanged event) {
        if (event.getVarbitId() == CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP && event.getValue() > 0 && config.notifyCombatTask()) {
            warnForGameSetting(CombatTaskNotifier.REPEAT_WARNING);
        }

        if (event.getVarbitId() == Varbits.COLLECTION_LOG_NOTIFICATION && event.getValue() % 2 != 1 && config.notifyCollectionLog()) {
            warnForGameSetting(CollectionNotifier.ADDITION_WARNING);
        }
    }

    private void warnForGameSetting(String message) {
        if (Utils.isSettingsOpen(client)) {
            plugin.addChatWarning(message);
        } else {
            log.warn(message);
        }
    }
}
