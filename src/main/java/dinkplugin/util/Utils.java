package dinkplugin.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.ColorUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

public class Utils {

    public static final Color PINK = ColorUtil.fromHex("#f40098"); // analogous to RED in CIELCh_uv color space
    public static final Color RED = ColorUtil.fromHex("#ca2a2d"); // red used in pajaW

    private static final Pattern TIME_PATTERN = Pattern.compile("\\b(?:(?<hours>\\d+):)?(?<minutes>\\d+):(?<seconds>\\d{2})(?:\\.(?<fractional>\\d{2}))?\\b");

    private static final Set<WorldType> IGNORED_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD);

    private static final Set<Integer> LMS_REGIONS = ImmutableSet.of(13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
    private static final Set<Integer> POH_REGIONS = ImmutableSet.of(7257, 7513, 7514, 7769, 7770, 8025, 8026);
    private static final Set<Integer> SOUL_REGIONS = ImmutableSet.of(8493, 8749, 9005);
    private static final int CASTLE_WARS_REGION = 9520;

    @VisibleForTesting
    public static final int CASTLE_WARS_COUNTDOWN = 380;
    @Varbit
    private static final int CASTLE_WARS_X_OFFSET = 156;
    @Varbit
    private static final int CASTLE_WARS_Y_OFFSET = 157;

    @VisibleForTesting
    public static final @Varbit int ENABLE_PRECISE_TIMING = 11866;

    public static boolean isIgnoredWorld(Set<WorldType> worldType) {
        return !Collections.disjoint(IGNORED_WORLDS, worldType);
    }

    public static boolean isPvpWorld(Set<WorldType> worldType) {
        return worldType.contains(WorldType.PVP) || worldType.contains(WorldType.DEADMAN);
    }

    public static boolean isPvpSafeZone(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PVP_WORLD_SAFE_ZONE);
        return widget != null && !widget.isSelfHidden();
    }

    public static boolean isCastleWars(Client client) {
        return client.getLocalPlayer().getWorldLocation().getRegionID() == CASTLE_WARS_REGION &&
            (client.getVarpValue(CASTLE_WARS_COUNTDOWN) > 0 || client.getVarbitValue(CASTLE_WARS_X_OFFSET) > 0 || client.getVarbitValue(CASTLE_WARS_Y_OFFSET) > 0);
    }

    public static boolean isLastManStanding(Client client) {
        return LMS_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public static boolean isPestControl(Client client) {
        Widget widget = client.getWidget(WidgetInfo.PEST_CONTROL_BLUE_SHIELD);
        return widget != null;
    }

    public static boolean isPlayerOwnedHouse(Client client) {
        return POH_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public static boolean isSafeArea(Client client) {
        return isCastleWars(client) || isPestControl(client) || isPlayerOwnedHouse(client);
    }

    public static boolean isSoulWars(Client client) {
        return SOUL_REGIONS.contains(client.getLocalPlayer().getWorldLocation().getRegionID());
    }

    public static boolean isSettingsOpen(@NotNull Client client) {
        Widget widget = client.getWidget(WidgetInfo.SETTINGS_INIT);
        return widget != null && !widget.isSelfHidden();
    }

    public static String getPlayerName(Client client) {
        return client.getLocalPlayer().getName();
    }

    public static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static boolean hasImage(@NotNull MultipartBody body) {
        return body.parts().stream().anyMatch(part -> {
            MediaType type = part.body().contentType();
            return type != null && "image".equals(type.type());
        });
    }

    public static boolean isPreciseTiming(Client client) {
        return client.getVarbitValue(ENABLE_PRECISE_TIMING) > 0;
    }

    @NotNull
    public static Duration parseTime(String in) {
        Matcher m = TIME_PATTERN.matcher(in);
        if (!m.find()) return Duration.ZERO;

        int minutes = Integer.parseInt(m.group("minutes"));
        int seconds = Integer.parseInt(m.group("seconds"));

        Duration d = Duration.ofMinutes(minutes).plusSeconds(seconds);

        String hours = m.group("hours");
        if (hours != null) {
            d = d.plusHours(Integer.parseInt(hours));
        }

        String fractional = m.group("fractional");
        if (fractional != null) {
            // osrs sends 2 digits, but this is robust to changes
            String f = fractional.length() < 3 ? StringUtils.rightPad(fractional, 3, '0') : fractional.substring(0, 3);
            d = d.plusMillis(Integer.parseInt(f));
        }

        return d;
    }

    @NotNull
    public static String format(@Nullable Duration duration, boolean precise) {
        Temporal time = ObjectUtils.defaultIfNull(duration, Duration.ZERO).addTo(LocalTime.of(0, 0));
        StringBuilder sb = new StringBuilder();

        int h = time.get(HOUR_OF_DAY);
        if (h > 0)
            sb.append(String.format("%02d", h)).append(':');

        sb.append(String.format("%02d", time.get(MINUTE_OF_HOUR))).append(':');
        sb.append(String.format("%02d", time.get(SECOND_OF_MINUTE)));

        if (precise)
            sb.append('.').append(String.format("%02d", time.get(MILLI_OF_SECOND) / 10));

        return sb.toString();
    }

    // Credit to: https://github.com/oliverpatrick/Enhanced-Discord-Notifications/blob/master/src/main/java/com/enhanceddiscordnotifications/EnhancedDiscordNotificationsPlugin.java
    // This method existed and seemed fairly solid.

    private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: [Qq]uest)?[!.]?$");
    private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: [Qq]uest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$");
    private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
    private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");

    public static String parseQuestWidget(final String text) {
        // "You have completed The Corsair Curse!"
        final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
        // "'One Small Favour' completed!"
        final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
        final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;
        if (!questMatchFinal.matches()) {
            return "Unable to find quest name!";
        }

        String quest = questMatchFinal.group("quest");
        String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

        if (verb.contains("kind of")) {
            quest += " partial completion";
        } else if (verb.contains("completely")) {
            quest += " II";
        }

        if (RFD_TAGS.stream().anyMatch((quest + verb)::contains)) {
            quest = "Recipe for Disaster - " + quest;
        }

        if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains)) {
            quest += " Quest";
        }

        return quest;
    }
}
