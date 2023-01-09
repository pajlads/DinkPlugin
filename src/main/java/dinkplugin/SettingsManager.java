package dinkplugin;

import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import dinkplugin.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class SettingsManager {
    private static final String CONFIG_GROUP = "dinkplugin";
    private static final Pattern DELIM = Pattern.compile("[,;\\n]");

    private final Collection<String> nameDenyList = new HashSet<>();

    private final Client client;
    private final ClientThread clientThread;
    private final DinkPlugin plugin;
    private final DinkPluginConfig config;

    @Synchronized
    public boolean testUsername(String name) {
        return name != null && !nameDenyList.contains(name.toLowerCase());
    }

    @VisibleForTesting
    public void init() {
        setNameDenyList(config.nameDenyList());
    }

    void onConfigChanged(ConfigChanged event) {
        if (!CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        String key = event.getKey();
        String value = event.getNewValue();

        if ("rsnDenyList".equals(key)) {
            setNameDenyList(value);
            return;
        }

        if (client.getGameState() == GameState.LOGGED_IN) {
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

    private Stream<String> readDelimited(String value) {
        if (value == null) return Stream.empty();
        return DELIM.splitAsStream(value)
            .map(String::trim)
            .filter(StringUtils::isNotEmpty);
    }

    @Synchronized
    private void setNameDenyList(String configValue) {
        nameDenyList.clear();
        readDelimited(configValue)
            .map(String::toLowerCase)
            .forEach(nameDenyList::add);
        log.debug("Updated RSN Deny List to: {}", nameDenyList);

        if (plugin != null)
            plugin.resetNotifiers();
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
