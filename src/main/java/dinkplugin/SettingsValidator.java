package dinkplugin;

import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import dinkplugin.util.Utils;
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
                if (isRepeatPopupInvalid(client.getVarbitValue(CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP))) {
                    plugin.addChatWarning(CombatTaskNotifier.REPEAT_WARNING);
                }
            });
            return;
        }

        if ("collectionLogEnabled".equals(key) && "true".equals(value)) {
            clientThread.invokeLater(() -> {
                if (isCollectionLogInvalid(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION))) {
                    plugin.addChatWarning(CollectionNotifier.ADDITION_WARNING);
                }
            });
        }
    }

    void onVarbitChanged(VarbitChanged event) {
        int value = event.getValue();

        if (event.getVarbitId() == CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP && isRepeatPopupInvalid(value) && config.notifyCombatTask()) {
            warnForGameSetting(CombatTaskNotifier.REPEAT_WARNING);
        }

        if (event.getVarbitId() == Varbits.COLLECTION_LOG_NOTIFICATION && isCollectionLogInvalid(value) && config.notifyCollectionLog()) {
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

    private static boolean isCollectionLogInvalid(int varbitValue) {
        // we require chat notification for collection log notifier
        return varbitValue != 1 && varbitValue != 3;
    }

    private static boolean isRepeatPopupInvalid(int varbitValue) {
        // we discourage repeat notifications for combat task notifier if unintentional
        return varbitValue > 0;
    }
}
