package dinkplugin.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@UtilityClass
public class ConfigUtil {

    private final Pattern DELIM = Pattern.compile("[,;\\n]");

    /**
     * Set of our config keys that correspond to webhook URL lists.
     * <p>
     * These are used in {@link dinkplugin.SettingsManager} for special logic
     * to merge the previous value with the new value during config imports.
     *
     * @see dinkplugin.DinkPluginConfig
     */
    public final Collection<String> WEBHOOK_CONFIG_KEYS = ImmutableSet.of(
        "discordWebhook",
        "collectionWebhook",
        "petWebhook",
        "levelWebhook",
        "lootWebhook",
        "deathWebhook",
        "slayerWebhook",
        "questWebhook",
        "clueWebhook",
        "speedrunWebhook",
        "killCountWebhook",
        "combatTaskWebhook",
        "diaryWebhook",
        "gambleWebhook"
    );

    public Stream<String> readDelimited(String value) {
        if (value == null) return Stream.empty();
        return DELIM.splitAsStream(value)
            .map(String::trim)
            .filter(StringUtils::isNotEmpty);
    }

    @Nullable
    public Object convertTypeFromJson(@NotNull Type type, @NotNull Object in) {
        if (in instanceof Boolean)
            return type == boolean.class || type == Boolean.class ? in : null;

        if (in instanceof Number) {
            Number n = (Number) in;

            if (type == int.class || type == Integer.class)
                return n.intValue();

            if (type == long.class || type == Long.class)
                return n.longValue();

            if (type == float.class || type == Float.class)
                return n.floatValue();

            if (type == double.class || type == Double.class)
                return n.doubleValue();

            if (type == byte.class || type == Byte.class)
                return n.byteValue();

            if (type == short.class || type == Short.class)
                return n.shortValue();

            if (type == Instant.class)
                return Instant.ofEpochMilli(n.longValue());

            if (type == Duration.class)
                return Duration.ofMillis(n.longValue());

            return null;
        }

        if (in instanceof String) {
            String s = (String) in;

            if (type == String.class)
                return s;

            if (type == Color.class)
                return ColorUtil.fromString(s);

            if (type instanceof Class && ((Class<?>) type).isEnum()) {
                try {
                    // noinspection unchecked,rawtypes
                    return Enum.valueOf((Class<? extends Enum>) type, s);
                } catch (Exception e) {
                    return null;
                }
            }

            if (type == Instant.class) {
                try {
                    return Instant.parse(s);
                } catch (Exception e) {
                    return null;
                }
            }

            if (type == Duration.class) {
                try {
                    return Duration.parse(s);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return null;
    }

    public boolean isSettingsOpen(@NotNull Client client) {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        Widget widget = client.getWidget(WidgetInfo.SETTINGS_INIT);
        return widget != null && !widget.isHidden();
    }

    public boolean isKillCountFilterInvalid(int varbitValue) {
        // spam filter must be disabled for kill count chat message
        return varbitValue > 0;
    }

    public boolean isCollectionLogInvalid(int varbitValue) {
        // we require chat notification for collection log notifier
        return varbitValue != 1 && varbitValue != 3;
    }

    public boolean isRepeatPopupInvalid(int varbitValue) {
        // we discourage repeat notifications for combat task notifier if unintentional
        return varbitValue > 0;
    }

    public boolean isPetLootInvalid(int varbitValue) {
        // LOOT_DROP_NOTIFICATIONS and UNTRADEABLE_LOOT_DROPS must both be set to 1 for reliable pet name parsing
        return varbitValue < 1;
    }
}
