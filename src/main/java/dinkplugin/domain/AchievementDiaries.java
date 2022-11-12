package dinkplugin.domain;

import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Varbits;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public enum AchievementDiaries {

    INSTANCE;

    @Getter
    private final Map<Integer, Diary> diaries;

    AchievementDiaries() {
        this.diaries = Arrays.stream(Varbits.class.getDeclaredFields())
            .filter(field -> Modifier.isPublic(field.getModifiers()))
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .filter(field -> Modifier.isFinal(field.getModifiers()))
            .filter(field -> field.getType() == int.class)
            .filter(field -> field.getName().startsWith("DIARY_"))
            .map(Diary::from)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Diary::getVarbitId, Function.identity()));
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        ELITE
    }

    @Value
    public static class Diary {
        int varbitId;
        String area;
        Difficulty difficulty;

        @Nullable
        static Diary from(@NotNull Field varbit) {
            try {
                int id = varbit.getInt(null);

                String fieldName = varbit.getName();
                String[] parts = StringUtils.split(fieldName, '_');
                if (parts.length != 3) return null;

                Difficulty difficulty = Difficulty.valueOf(parts[2]);

                String area = parts[1];
                area = area.charAt(0) + area.substring(1).toLowerCase();

                if ("Western".equals(area)) {
                    area = "Western Provinces";
                }

                // correction for karamja task completion special case
                // instead of yielding 1 or 0 values, they yield 2 or 1.
                // so we use karamja reward id's instead, which yield 1 or 0
                // however, the elite difficulty does not face this problem
                if ("Karamja".equals(area)) {
                    if (difficulty == Difficulty.EASY)
                        id = 3577;
                    else if (difficulty == Difficulty.MEDIUM)
                        id = 3598;
                    else if (difficulty == Difficulty.HARD)
                        id = 3610;
                }

                return new Diary(id, area, difficulty);
            } catch (Exception e) {
                log.debug("Failed to parse achievement diary: " + varbit.getName(), e);
                return null;
            }
        }
    }

}
