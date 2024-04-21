package dinkplugin;

import dinkplugin.util.Utils;
import lombok.Value;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class VersionManager {
    public static final String VERSION_CONFIG_KEY = "pluginVersion";
    private static final long NOTIFICATION_DELAY_SECONDS = 3;
    private static final NavigableMap<Version, String> VERSIONS = new TreeMap<>(
        Comparator.comparingInt(Version::getMajor)
            .thenComparingInt(Version::getMinor)
            .thenComparingInt(Version::getPatch)
    );

    private final Version latest = VERSIONS.lastKey();

    @Inject
    private DinkPlugin plugin;

    @Inject
    private DinkPluginConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SettingsManager settingsManager;

    @Inject
    private ScheduledExecutorService executor;

    void onStart() {
        if (config.pluginVersion().isEmpty()) {
            if (settingsManager.hasModifiedConfig()) {
                // Dink was already installed/used & launched for the first time since VersionManager was created
                setStoredVersion("1.8.4");
            } else {
                // first time launching Dink; no chat message necessary
                setStoredVersion(latest.toString());
            }
        }
    }

    void onGameState(GameState oldState, GameState newState) {
        if (newState != GameState.LOGGED_IN || oldState != GameState.LOGGING_IN) {
            // only check version when LOGGING_IN => LOGGED_IN
            return;
        }

        Version storedVersion = Version.of(config.pluginVersion());
        if (storedVersion == null || storedVersion.equals(latest)) {
            return;
        }

        setStoredVersion(latest.toString());
        executor.schedule(() -> {
            SortedMap<Version, String> latestUpdates = VERSIONS.tailMap(storedVersion, false);
            if (latestUpdates.isEmpty()) {
                return;
            }
            String displayVersion = latest.getPatch() == 0
                ? String.format("%d.%d.X", latest.getMajor(), latest.getMinor()) // avoids needing changelog for each patch
                : latest.toString();
            plugin.addChatMessage(
                "Updated to v" + displayVersion,
                Utils.GREEN,
                String.join("; ", latestUpdates.values())
            );
        }, NOTIFICATION_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    void onProfileChange() {
        // if profile changes, set pluginVersion to latest so users don't see old changelogs on login
        setStoredVersion(latest.toString());
    }

    private void setStoredVersion(String version) {
        configManager.setConfiguration(SettingsManager.CONFIG_GROUP, VERSION_CONFIG_KEY, version);
    }

    private static void register(String version, String changelog) {
        VERSIONS.put(Version.of(version), changelog);
    }

    @Value
    private static class Version {
        int major;
        int minor;
        int patch;

        @Override
        public String toString() {
            return String.format("%d.%d.%d", major, minor, patch);
        }

        @Nullable
        public static Version of(@NotNull String version) {
            String[] parts = StringUtils.split(version, '.');
            if (parts.length < 3) return null;
            int major, minor, patch;
            try {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
                patch = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                return null;
            }
            return new Version(major, minor, patch);
        }
    }

    static {
        register("1.9.0", "Notifications now report monster drop rarity");
        register("1.10.0", "Chat messages that match custom patterns can trigger notifications");
        register("1.10.1", "Level notifier now triggers at XP milestones with 5M as the default interval");
    }
}
