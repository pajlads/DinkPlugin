package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum AchievementDiary {
    ARDOUGNE_EASY(Varbits.DIARY_ARDOUGNE_EASY, 1, "Ardougne", Difficulty.EASY),
    ARDOUGNE_MEDIUM(Varbits.DIARY_ARDOUGNE_MEDIUM, 1, "Ardougne", Difficulty.MEDIUM),
    ARDOUGNE_HARD(Varbits.DIARY_ARDOUGNE_HARD, 1, "Ardougne", Difficulty.HARD),
    ARDOUGNE_ELITE(Varbits.DIARY_ARDOUGNE_ELITE, 1, "Ardougne", Difficulty.ELITE),
    DESERT_EASY(Varbits.DIARY_DESERT_EASY, 5, "Desert", Difficulty.EASY),
    DESERT_MEDIUM(Varbits.DIARY_DESERT_MEDIUM, 5, "Desert", Difficulty.MEDIUM),
    DESERT_HARD(Varbits.DIARY_DESERT_HARD, 5, "Desert", Difficulty.HARD),
    DESERT_ELITE(Varbits.DIARY_DESERT_ELITE, 5, "Desert", Difficulty.ELITE),
    FALADOR_EASY(Varbits.DIARY_FALADOR_EASY, 2, "Falador", Difficulty.EASY),
    FALADOR_MEDIUM(Varbits.DIARY_FALADOR_MEDIUM, 2, "Falador", Difficulty.MEDIUM),
    FALADOR_HARD(Varbits.DIARY_FALADOR_HARD, 2, "Falador", Difficulty.HARD),
    FALADOR_ELITE(Varbits.DIARY_FALADOR_ELITE, 2, "Falador", Difficulty.ELITE),
    FREMENNIK_EASY(Varbits.DIARY_FREMENNIK_EASY, 3, "Fremennik", Difficulty.EASY),
    FREMENNIK_MEDIUM(Varbits.DIARY_FREMENNIK_MEDIUM, 3, "Fremennik", Difficulty.MEDIUM),
    FREMENNIK_HARD(Varbits.DIARY_FREMENNIK_HARD, 3, "Fremennik", Difficulty.HARD),
    FREMENNIK_ELITE(Varbits.DIARY_FREMENNIK_ELITE, 3, "Fremennik", Difficulty.ELITE),
    KANDARIN_EASY(Varbits.DIARY_KANDARIN_EASY, 4, "Kandarin", Difficulty.EASY),
    KANDARIN_MEDIUM(Varbits.DIARY_KANDARIN_MEDIUM, 4, "Kandarin", Difficulty.MEDIUM),
    KANDARIN_HARD(Varbits.DIARY_KANDARIN_HARD, 4, "Kandarin", Difficulty.HARD),
    KANDARIN_ELITE(Varbits.DIARY_KANDARIN_ELITE, 4, "Kandarin", Difficulty.ELITE),
    KARAMJA_EASY(Varbits.DIARY_KARAMJA_EASY, 0, "Karamja", Difficulty.EASY),
    KARAMJA_MEDIUM(Varbits.DIARY_KARAMJA_MEDIUM, 0, "Karamja", Difficulty.MEDIUM),
    KARAMJA_HARD(Varbits.DIARY_KARAMJA_HARD, 0, "Karamja", Difficulty.HARD),
    KARAMJA_ELITE(Varbits.DIARY_KARAMJA_ELITE, 0, "Karamja", Difficulty.ELITE),
    KOUREND_EASY(Varbits.DIARY_KOUREND_EASY, 11, "Kourend & Kebos", Difficulty.EASY),
    KOUREND_MEDIUM(Varbits.DIARY_KOUREND_MEDIUM, 11, "Kourend & Kebos", Difficulty.MEDIUM),
    KOUREND_HARD(Varbits.DIARY_KOUREND_HARD, 11, "Kourend & Kebos", Difficulty.HARD),
    KOUREND_ELITE(Varbits.DIARY_KOUREND_ELITE, 11, "Kourend & Kebos", Difficulty.ELITE),
    LUMBRIDGE_EASY(Varbits.DIARY_LUMBRIDGE_EASY, 6, "Lumbridge & Draynor", Difficulty.EASY),
    LUMBRIDGE_MEDIUM(Varbits.DIARY_LUMBRIDGE_MEDIUM, 6, "Lumbridge & Draynor", Difficulty.MEDIUM),
    LUMBRIDGE_HARD(Varbits.DIARY_LUMBRIDGE_HARD, 6, "Lumbridge & Draynor", Difficulty.HARD),
    LUMBRIDGE_ELITE(Varbits.DIARY_LUMBRIDGE_ELITE, 6, "Lumbridge & Draynor", Difficulty.ELITE),
    MORYTANIA_EASY(Varbits.DIARY_MORYTANIA_EASY, 7, "Morytania", Difficulty.EASY),
    MORYTANIA_MEDIUM(Varbits.DIARY_MORYTANIA_MEDIUM, 7, "Morytania", Difficulty.MEDIUM),
    MORYTANIA_HARD(Varbits.DIARY_MORYTANIA_HARD, 7, "Morytania", Difficulty.HARD),
    MORYTANIA_ELITE(Varbits.DIARY_MORYTANIA_ELITE, 7, "Morytania", Difficulty.ELITE),
    VARROCK_EASY(Varbits.DIARY_VARROCK_EASY, 8, "Varrock", Difficulty.EASY),
    VARROCK_MEDIUM(Varbits.DIARY_VARROCK_MEDIUM, 8, "Varrock", Difficulty.MEDIUM),
    VARROCK_HARD(Varbits.DIARY_VARROCK_HARD, 8, "Varrock", Difficulty.HARD),
    VARROCK_ELITE(Varbits.DIARY_VARROCK_ELITE, 8, "Varrock", Difficulty.ELITE),
    WESTERN_EASY(Varbits.DIARY_WESTERN_EASY, 10, "Western Provinces", Difficulty.EASY),
    WESTERN_MEDIUM(Varbits.DIARY_WESTERN_MEDIUM, 10, "Western Provinces", Difficulty.MEDIUM),
    WESTERN_HARD(Varbits.DIARY_WESTERN_HARD, 10, "Western Provinces", Difficulty.HARD),
    WESTERN_ELITE(Varbits.DIARY_WESTERN_ELITE, 10, "Western Provinces", Difficulty.ELITE),
    WILDERNESS_EASY(Varbits.DIARY_WILDERNESS_EASY, 9, "Wilderness", Difficulty.EASY),
    WILDERNESS_MEDIUM(Varbits.DIARY_WILDERNESS_MEDIUM, 9, "Wilderness", Difficulty.MEDIUM),
    WILDERNESS_HARD(Varbits.DIARY_WILDERNESS_HARD, 9, "Wilderness", Difficulty.HARD),
    WILDERNESS_ELITE(Varbits.DIARY_WILDERNESS_ELITE, 9, "Wilderness", Difficulty.ELITE);

    public static final Map<Integer, AchievementDiary> DIARIES = Collections.unmodifiableMap(
        Arrays.stream(values()).collect(Collectors.toMap(AchievementDiary::getId, Function.identity()))
    );

    @Varbit
    private final int id;
    private final int areaId;
    private final String area;
    private final Difficulty difficulty;

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        ELITE;

        private final String displayName = this.name().charAt(0) + this.name().substring(1).toLowerCase();

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
