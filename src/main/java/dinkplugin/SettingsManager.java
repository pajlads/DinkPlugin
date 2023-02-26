package dinkplugin;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dinkplugin.util.ConfigUtil.*;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class SettingsManager {
    private static final String CONFIG_GROUP = "dinkplugin";
    private static final Set<Integer> PROBLEMATIC_VARBITS;

    /**
     * Maps our setting keys to their type for safe serialization and deserialization.
     */
    private final Map<String, Type> configValueTypes = new HashMap<>();

    /**
     * Maps section names to the corresponding config item keys to allow for selective export.
     */
    private final Map<String, Collection<String>> keysBySection = new HashMap<>();

    /**
     * User-specified RSNs that should not trigger webhook notifications.
     */
    private final Collection<String> ignoredNames = new HashSet<>();

    private final Gson gson;
    private final Client client;
    private final ClientThread clientThread;
    private final DinkPlugin plugin;
    private final DinkPluginConfig config;
    private final ConfigManager configManager;

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
        configManager.getConfigDescriptor(config).getItems().forEach(item -> {
            String key = item.key();
            configValueTypes.put(key, item.getType());

            String section = item.getItem().section();
            if (StringUtils.isNotEmpty(section)) {
                keysBySection.computeIfAbsent(
                    section.toLowerCase().replace(" ", ""),
                    s -> new HashSet<>()
                ).add(key);
            }
        });
    }

    void onCommand(CommandExecuted event) {
        String cmd = event.getCommand();
        if ("DinkImport".equalsIgnoreCase(cmd)) {
            importConfig();
        } else if ("DinkExport".equalsIgnoreCase(cmd)) {
            String[] args = event.getArguments();

            Predicate<String> includeKey;
            if (args == null || args.length == 0) {
                includeKey = k -> !WEBHOOK_CONFIG_KEYS.contains(k);
            } else {
                includeKey = k -> false;

                for (String arg : args) {
                    if ("all".equalsIgnoreCase(arg)) {
                        includeKey = k -> true;
                        break;
                    } else if ("webhooks".equalsIgnoreCase(arg)) {
                        includeKey = includeKey.or(WEBHOOK_CONFIG_KEYS::contains);
                    } else {
                        Collection<String> sectionKeys = keysBySection.get(arg.toLowerCase());
                        if (sectionKeys != null) {
                            includeKey = includeKey.or(sectionKeys::contains);
                        } else {
                            plugin.addChatWarning(String.format("Failed to identify config section to export: \"%s\"", arg));
                            return;
                        }
                    }
                }
            }

            exportConfig(includeKey);
        }
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

        if (client.getGameState() == GameState.LOGGED_IN) {
            checkVarbits(id, value);
        } else if (PROBLEMATIC_VARBITS.contains(id)) {
            clientThread.invokeLater(() -> checkVarbits(id, client.getVarbitValue(id)));
        }
    }

    private void checkVarbits(int id, int value) {
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
        if (isSettingsOpen(client)) {
            plugin.addChatWarning(message);
        } else {
            log.warn(message);
        }
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

    /**
     * Exports the full Dink config to a JSON map, excluding empty lists,
     * which is copied to the user's clipboard in string form
     */
    @Synchronized
    private void exportConfig(@NotNull Predicate<String> exportKey) {
        String prefix = CONFIG_GROUP + '.';
        Map<String, Object> configMap = configManager.getConfigurationKeys(prefix)
            .stream()
            .map(prop -> prop.substring(prefix.length()))
            .filter(exportKey)
            .map(key -> Pair.of(key, configValueTypes.get(key)))
            .filter(pair -> pair.getValue() != null)
            .map(pair -> Pair.of(pair.getKey(), configManager.getConfiguration(CONFIG_GROUP, pair.getKey(), pair.getValue())))
            .filter(pair -> pair.getValue() != null)
            .filter(pair -> {
                // only serialize webhook urls if they are not blank
                if (WEBHOOK_CONFIG_KEYS.contains(pair.getKey())) {
                    Object value = pair.getValue();
                    return value instanceof String && StringUtils.isNotBlank((String) value);
                }

                // always serialize everything else
                return true;
            })
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        Utils.copyToClipboard(gson.toJson(configMap))
            .thenRun(() -> plugin.addChatSuccess("Copied current configuration to clipboard"))
            .exceptionally(e -> {
                plugin.addChatWarning("Failed to copy config to clipboard");
                return null;
            });
    }

    /**
     * Imports a Dink JSON config export from the user's clipboard
     */
    @Synchronized
    private void importConfig() {
        Utils.readClipboard()
            .thenApplyAsync(json -> {
                if (json == null || json.isEmpty()) {
                    plugin.addChatWarning("Clipboard was empty");
                    return null;
                }

                try {
                    return gson.<Map<String, Object>>fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
                } catch (Exception e) {
                    String warning = "Failed to parse config from clipboard";
                    log.warn(warning, e);
                    plugin.addChatWarning(warning);
                    return null;
                }
            })
            .thenAcceptAsync(this::handleImport)
            .exceptionally(e -> {
                plugin.addChatWarning("Failed to read clipboard");
                return null;
            });
    }

    @Synchronized
    private void handleImport(Map<String, Object> map) {
        if (map == null) return;

        AtomicInteger numUpdated = new AtomicInteger();
        Collection<String> mergedConfigs = new TreeSet<>();
        map.forEach((key, rawValue) -> {
            Type valueType = configValueTypes.get(key);
            if (valueType == null) {
                log.debug("Encountered unrecognized config mapping during import: {} = {}", key, rawValue);
                return;
            }

            if (rawValue == null) {
                log.debug("Encountered null value for config key: {}", key);
                return;
            }

            Object value = convertTypeFromJson(valueType, rawValue);
            if (value == null) {
                log.debug("Encountered config value with incorrect type: {} = {}", key, rawValue);
                return;
            }

            Object prevValue = configManager.getConfiguration(CONFIG_GROUP, key, valueType);
            Object newValue;

            if (WEBHOOK_CONFIG_KEYS.contains(key) || "ignoredNames".equals(key)) {
                // special case: multi-line configs that should be merged (rather than replaced)
                assert prevValue == null || prevValue instanceof String;
                Collection<String> lines = readDelimited((String) prevValue).collect(Collectors.toCollection(LinkedHashSet::new));

                int oldCount = lines.size();
                assert value instanceof String;
                long added = readDelimited((String) value)
                    .map(lines::add)
                    .filter(Boolean::booleanValue)
                    .count();

                if (added > 0) {
                    newValue = String.join("\n", lines);

                    if (oldCount > 0) {
                        String displayName = "discordWebhook".equals(key) ? "primaryWebhook" : key;
                        mergedConfigs.add(displayName);
                    }
                } else {
                    newValue = null;
                }
            } else {
                if (Objects.equals(value, prevValue)) {
                    newValue = null;
                } else {
                    newValue = value;
                }
            }

            if (newValue != null) {
                configManager.setConfiguration(CONFIG_GROUP, key, newValue);
                numUpdated.incrementAndGet();
            }
        });

        plugin.addChatSuccess(
            String.format(
                "Updated %d config settings (from %d total specified in import). " +
                    "Please close and open the plugin settings panel for these changes to be visually reflected.",
                numUpdated.get(),
                map.size()
            )
        );

        if (!mergedConfigs.isEmpty()) {
            plugin.addChatSuccess("The following settings were merged (rather than being overwritten): " + String.join(", ", mergedConfigs));
        }
    }

    static {
        PROBLEMATIC_VARBITS = ImmutableSet.of(
            KillCountNotifier.KILL_COUNT_SPAM_FILTER,
            CombatTaskNotifier.COMBAT_TASK_REPEAT_POPUP,
            Varbits.COLLECTION_LOG_NOTIFICATION,
            PetNotifier.LOOT_DROP_NOTIFICATIONS,
            PetNotifier.UNTRADEABLE_LOOT_DROPS
        );
    }
}
