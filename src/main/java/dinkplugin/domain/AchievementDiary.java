package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum AchievementDiary {
    ARDOUGNE_EASY(Varbits.DIARY_ARDOUGNE_EASY, "Ardougne", Difficulty.EASY),
    ARDOUGNE_MEDIUM(Varbits.DIARY_ARDOUGNE_MEDIUM, "Ardougne", Difficulty.MEDIUM),
    ARDOUGNE_HARD(Varbits.DIARY_ARDOUGNE_HARD, "Ardougne", Difficulty.HARD),
    ARDOUGNE_ELITE(Varbits.DIARY_ARDOUGNE_ELITE, "Ardougne", Difficulty.ELITE),
    DESERT_EASY(Varbits.DIARY_DESERT_EASY, "Desert", Difficulty.EASY),
    DESERT_MEDIUM(Varbits.DIARY_DESERT_MEDIUM, "Desert", Difficulty.MEDIUM),
    DESERT_HARD(Varbits.DIARY_DESERT_HARD, "Desert", Difficulty.HARD),
    DESERT_ELITE(Varbits.DIARY_DESERT_ELITE, "Desert", Difficulty.ELITE),
    FALADOR_EASY(Varbits.DIARY_FALADOR_EASY, "Falador", Difficulty.EASY),
    FALADOR_MEDIUM(Varbits.DIARY_FALADOR_MEDIUM, "Falador", Difficulty.MEDIUM),
    FALADOR_HARD(Varbits.DIARY_FALADOR_HARD, "Falador", Difficulty.HARD),
    FALADOR_ELITE(Varbits.DIARY_FALADOR_ELITE, "Falador", Difficulty.ELITE),
    FREMENNIK_EASY(Varbits.DIARY_FREMENNIK_EASY, "Fremennik", Difficulty.EASY),
    FREMENNIK_MEDIUM(Varbits.DIARY_FREMENNIK_MEDIUM, "Fremennik", Difficulty.MEDIUM),
    FREMENNIK_HARD(Varbits.DIARY_FREMENNIK_HARD, "Fremennik", Difficulty.HARD),
    FREMENNIK_ELITE(Varbits.DIARY_FREMENNIK_ELITE, "Fremennik", Difficulty.ELITE),
    KANDARIN_EASY(Varbits.DIARY_KANDARIN_EASY, "Kandarin", Difficulty.EASY),
    KANDARIN_MEDIUM(Varbits.DIARY_KANDARIN_MEDIUM, "Kandarin", Difficulty.MEDIUM),
    KANDARIN_HARD(Varbits.DIARY_KANDARIN_HARD, "Kandarin", Difficulty.HARD),
    KANDARIN_ELITE(Varbits.DIARY_KANDARIN_ELITE, "Kandarin", Difficulty.ELITE),
    KARAMJA_EASY(Varbits.DIARY_KARAMJA_EASY, "Karamja", Difficulty.EASY),
    KARAMJA_MEDIUM(Varbits.DIARY_KARAMJA_MEDIUM, "Karamja", Difficulty.MEDIUM),
    KARAMJA_HARD(Varbits.DIARY_KARAMJA_HARD, "Karamja", Difficulty.HARD),
    KARAMJA_ELITE(Varbits.DIARY_KARAMJA_ELITE, "Karamja", Difficulty.ELITE),
    KOUREND_EASY(Varbits.DIARY_KOUREND_EASY, "Kourend & Kebos", Difficulty.EASY),
    KOUREND_MEDIUM(Varbits.DIARY_KOUREND_MEDIUM, "Kourend & Kebos", Difficulty.MEDIUM),
    KOUREND_HARD(Varbits.DIARY_KOUREND_HARD, "Kourend & Kebos", Difficulty.HARD),
    KOUREND_ELITE(Varbits.DIARY_KOUREND_ELITE, "Kourend & Kebos", Difficulty.ELITE),
    LUMBRIDGE_EASY(Varbits.DIARY_LUMBRIDGE_EASY, "Lumbridge & Draynor", Difficulty.EASY),
    LUMBRIDGE_MEDIUM(Varbits.DIARY_LUMBRIDGE_MEDIUM, "Lumbridge & Draynor", Difficulty.MEDIUM),
    LUMBRIDGE_HARD(Varbits.DIARY_LUMBRIDGE_HARD, "Lumbridge & Draynor", Difficulty.HARD),
    LUMBRIDGE_ELITE(Varbits.DIARY_LUMBRIDGE_ELITE, "Lumbridge & Draynor", Difficulty.ELITE),
    MORYTANIA_EASY(Varbits.DIARY_MORYTANIA_EASY, "Morytania", Difficulty.EASY),
    MORYTANIA_MEDIUM(Varbits.DIARY_MORYTANIA_MEDIUM, "Morytania", Difficulty.MEDIUM),
    MORYTANIA_HARD(Varbits.DIARY_MORYTANIA_HARD, "Morytania", Difficulty.HARD),
    MORYTANIA_ELITE(Varbits.DIARY_MORYTANIA_ELITE, "Morytania", Difficulty.ELITE),
    VARROCK_EASY(Varbits.DIARY_VARROCK_EASY, "Varrock", Difficulty.EASY),
    VARROCK_MEDIUM(Varbits.DIARY_VARROCK_MEDIUM, "Varrock", Difficulty.MEDIUM),
    VARROCK_HARD(Varbits.DIARY_VARROCK_HARD, "Varrock", Difficulty.HARD),
    VARROCK_ELITE(Varbits.DIARY_VARROCK_ELITE, "Varrock", Difficulty.ELITE),
    WESTERN_EASY(Varbits.DIARY_WESTERN_EASY, "Western Provinces", Difficulty.EASY),
    WESTERN_MEDIUM(Varbits.DIARY_WESTERN_MEDIUM, "Western Provinces", Difficulty.MEDIUM),
    WESTERN_HARD(Varbits.DIARY_WESTERN_HARD, "Western Provinces", Difficulty.HARD),
    WESTERN_ELITE(Varbits.DIARY_WESTERN_ELITE, "Western Provinces", Difficulty.ELITE),
    WILDERNESS_EASY(Varbits.DIARY_WILDERNESS_EASY, "Wilderness", Difficulty.EASY),
    WILDERNESS_MEDIUM(Varbits.DIARY_WILDERNESS_MEDIUM, "Wilderness", Difficulty.MEDIUM),
    WILDERNESS_HARD(Varbits.DIARY_WILDERNESS_HARD, "Wilderness", Difficulty.HARD),
    WILDERNESS_ELITE(Varbits.DIARY_WILDERNESS_ELITE, "Wilderness", Difficulty.ELITE);

    public static final Map<Integer, Pair<String, Difficulty>> DIARIES = Collections.unmodifiableMap(
        Arrays.stream(values())
            .collect(Collectors.toMap(AchievementDiary::getId, diary -> Pair.of(diary.getArea(), diary.getDifficulty())))
    );

    @Varbit
    private final int id;
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
