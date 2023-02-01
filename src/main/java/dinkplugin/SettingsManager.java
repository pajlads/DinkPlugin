package dinkplugin;

import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.notifiers.CombatTaskNotifier;
import dinkplugin.notifiers.KillCountNotifier;
import dinkplugin.notifiers.PetNotifier;
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

    private final Collection<String> ignoredNames = new HashSet<>();

    private final Client client;
    private final ClientThread clientThread;
    private final DinkPlugin plugin;
    private final DinkPluginConfig config;

    /**
     * Check whether a username is not on the configured ignore list.
     *
     * @param name the local player's name
     * @return whether notifications for this player should be sent
     */
    @Synchronized
    public boolean isNamePermitted(String name) {
        return name != null && !ignoredNames.contains(name.toLowerCase());
    }

    @VisibleForTesting
    public void init() {
        setIgnoredNames(config.ignoredNames());
    }

    void onConfigChanged(ConfigChanged event) {
        if (!CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        String key = event.getKey();
        String value = event.getNewValue();

        if ("ignoredNames".equals(key)) {
            setIgnoredNames(value);
            return;
        }

        if (client.getGameState() == GameState.LOGGED_IN) {
            if ("killCountEnabled".equals(key) && "true".equals(value)) {
                clientThread.invokeLater(() -> {
                    if (isKillCountFilterInvalid(client.getVarbitValue(KillCountNotifier.KILL_COUNT_SPAM_FILTER))) {
                        plugin.addChatWarning(KillCountNotifier.SPAM_WARNING);
                    }
                });
                return;
            }

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
                return;
            }

            if ("petEnabled".equals(key) && "true".equals(value)) {
                clientThread.invokeLater(() -> {
                    if (isPetLootInvalid(client.getVarbitValue(PetNotifier.UNTRADEABLE_LOOT_DROPS)) || isPetLootInvalid(client.getVarbitValue(PetNotifier.LOOT_DROP_NOTIFICATIONS))) {
                        plugin.addChatWarning(PetNotifier.UNTRADEABLE_WARNING);
                    }
                });
            }
        }
    }

    void onVarbitChanged(VarbitChanged event) {
        int id = event.getVarbitId();
        int value = event.getValue();

        if (id == KillCountNotifier.KILL_COUNT_SPAM_FILTER && isKillCountFilterInvalid(value) && config.notifyKillCount()) {
            warnForGameSetting(KillCountNotifier.SPAM_WARNING);
        }

        if (id == CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP && isRepeatPopupInvalid(value) && config.notifyCombatTask()) {
            warnForGameSetting(CombatTaskNotifier.REPEAT_WARNING);
        }

        if (id == Varbits.COLLECTION_LOG_NOTIFICATION && isCollectionLogInvalid(value) && config.notifyCollectionLog()) {
            warnForGameSetting(CollectionNotifier.ADDITION_WARNING);
        }

        if ((id == PetNotifier.LOOT_DROP_NOTIFICATIONS || id == PetNotifier.UNTRADEABLE_LOOT_DROPS) && isPetLootInvalid(value) && config.notifyPet()) {
            warnForGameSetting(PetNotifier.UNTRADEABLE_WARNING);
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
    private void setIgnoredNames(String configValue) {
        ignoredNames.clear();
        readDelimited(configValue)
            .map(String::toLowerCase)
            .forEach(ignoredNames::add);
        log.debug("Updated RSN Deny List to: {}", ignoredNames);

        // clear any outdated notifier state
        plugin.resetNotifiers();
    }

    private static boolean isKillCountFilterInvalid(int varbitValue) {
        // spam filter must be disabled for kill count chat message
        return varbitValue > 0;
    }

    private static boolean isCollectionLogInvalid(int varbitValue) {
        // we require chat notification for collection log notifier
        return varbitValue != 1 && varbitValue != 3;
    }

    private static boolean isRepeatPopupInvalid(int varbitValue) {
        // we discourage repeat notifications for combat task notifier if unintentional
        return varbitValue > 0;
    }

    private static boolean isPetLootInvalid(int varbitValue) {
        // LOOT_DROP_NOTIFICATIONS and UNTRADEABLE_LOOT_DROPS must both be set to 1 for reliable pet name parsing
        return varbitValue < 1;
    }
}
