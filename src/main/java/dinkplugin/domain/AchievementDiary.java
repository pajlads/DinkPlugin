package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.VarbitID;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum AchievementDiary {
    ARDOUGNE_EASY(VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, 1, "Ardougne", Difficulty.EASY),
    ARDOUGNE_MEDIUM(VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, 1, "Ardougne", Difficulty.MEDIUM),
    ARDOUGNE_HARD(VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, 1, "Ardougne", Difficulty.HARD),
    ARDOUGNE_ELITE(VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, 1, "Ardougne", Difficulty.ELITE),
    DESERT_EASY(VarbitID.DESERT_DIARY_EASY_COMPLETE, 5, "Desert", Difficulty.EASY),
    DESERT_MEDIUM(VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, 5, "Desert", Difficulty.MEDIUM),
    DESERT_HARD(VarbitID.DESERT_DIARY_HARD_COMPLETE, 5, "Desert", Difficulty.HARD),
    DESERT_ELITE(VarbitID.DESERT_DIARY_ELITE_COMPLETE, 5, "Desert", Difficulty.ELITE),
    FALADOR_EASY(VarbitID.FALADOR_DIARY_EASY_COMPLETE, 2, "Falador", Difficulty.EASY),
    FALADOR_MEDIUM(VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, 2, "Falador", Difficulty.MEDIUM),
    FALADOR_HARD(VarbitID.FALADOR_DIARY_HARD_COMPLETE, 2, "Falador", Difficulty.HARD),
    FALADOR_ELITE(VarbitID.FALADOR_DIARY_ELITE_COMPLETE, 2, "Falador", Difficulty.ELITE),
    FREMENNIK_EASY(VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, 3, "Fremennik", Difficulty.EASY),
    FREMENNIK_MEDIUM(VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, 3, "Fremennik", Difficulty.MEDIUM),
    FREMENNIK_HARD(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, 3, "Fremennik", Difficulty.HARD),
    FREMENNIK_ELITE(VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, 3, "Fremennik", Difficulty.ELITE),
    KANDARIN_EASY(VarbitID.KANDARIN_DIARY_EASY_COMPLETE, 4, "Kandarin", Difficulty.EASY),
    KANDARIN_MEDIUM(VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, 4, "Kandarin", Difficulty.MEDIUM),
    KANDARIN_HARD(VarbitID.KANDARIN_DIARY_HARD_COMPLETE, 4, "Kandarin", Difficulty.HARD),
    KANDARIN_ELITE(VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, 4, "Kandarin", Difficulty.ELITE),
    KARAMJA_EASY(VarbitID.ATJUN_EASY_DONE, 0, "Karamja", Difficulty.EASY),
    KARAMJA_MEDIUM(VarbitID.ATJUN_MED_DONE, 0, "Karamja", Difficulty.MEDIUM),
    KARAMJA_HARD(VarbitID.ATJUN_HARD_DONE, 0, "Karamja", Difficulty.HARD),
    KARAMJA_ELITE(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, 0, "Karamja", Difficulty.ELITE),
    KOUREND_EASY(VarbitID.KOUREND_DIARY_EASY_COMPLETE, 11, "Kourend & Kebos", Difficulty.EASY),
    KOUREND_MEDIUM(VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, 11, "Kourend & Kebos", Difficulty.MEDIUM),
    KOUREND_HARD(VarbitID.KOUREND_DIARY_HARD_COMPLETE, 11, "Kourend & Kebos", Difficulty.HARD),
    KOUREND_ELITE(VarbitID.KOUREND_DIARY_ELITE_COMPLETE, 11, "Kourend & Kebos", Difficulty.ELITE),
    LUMBRIDGE_EASY(VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, 6, "Lumbridge & Draynor", Difficulty.EASY),
    LUMBRIDGE_MEDIUM(VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, 6, "Lumbridge & Draynor", Difficulty.MEDIUM),
    LUMBRIDGE_HARD(VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, 6, "Lumbridge & Draynor", Difficulty.HARD),
    LUMBRIDGE_ELITE(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, 6, "Lumbridge & Draynor", Difficulty.ELITE),
    MORYTANIA_EASY(VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, 7, "Morytania", Difficulty.EASY),
    MORYTANIA_MEDIUM(VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, 7, "Morytania", Difficulty.MEDIUM),
    MORYTANIA_HARD(VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, 7, "Morytania", Difficulty.HARD),
    MORYTANIA_ELITE(VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE, 7, "Morytania", Difficulty.ELITE),
    VARROCK_EASY(VarbitID.VARROCK_DIARY_EASY_COMPLETE, 8, "Varrock", Difficulty.EASY),
    VARROCK_MEDIUM(VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, 8, "Varrock", Difficulty.MEDIUM),
    VARROCK_HARD(VarbitID.VARROCK_DIARY_HARD_COMPLETE, 8, "Varrock", Difficulty.HARD),
    VARROCK_ELITE(VarbitID.VARROCK_DIARY_ELITE_COMPLETE, 8, "Varrock", Difficulty.ELITE),
    WESTERN_EASY(VarbitID.WESTERN_DIARY_EASY_COMPLETE, 10, "Western Provinces", Difficulty.EASY),
    WESTERN_MEDIUM(VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, 10, "Western Provinces", Difficulty.MEDIUM),
    WESTERN_HARD(VarbitID.WESTERN_DIARY_HARD_COMPLETE, 10, "Western Provinces", Difficulty.HARD),
    WESTERN_ELITE(VarbitID.WESTERN_DIARY_ELITE_COMPLETE, 10, "Western Provinces", Difficulty.ELITE),
    WILDERNESS_EASY(VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, 9, "Wilderness", Difficulty.EASY),
    WILDERNESS_MEDIUM(VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, 9, "Wilderness", Difficulty.MEDIUM),
    WILDERNESS_HARD(VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, 9, "Wilderness", Difficulty.HARD),
    WILDERNESS_ELITE(VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE, 9, "Wilderness", Difficulty.ELITE);

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
