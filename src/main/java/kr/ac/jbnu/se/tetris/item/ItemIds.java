package kr.ac.jbnu.se.tetris.item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 아이템 식별자 문자열의 단일 정의 위치. 문자열 리터럴 중복(S1192)과 오타를 막는다. */
public final class ItemIds {
    public static final String DAMAGE_BOOST = "damage_boost";
    public static final String GARBAGE_BOMB = "garbage_bomb";
    public static final String HEAL = "heal";
    public static final String SHIELD = "shield";
    public static final String LINE_CLEANER = "line_cleaner";
    public static final String FEVER_CHARGE = "fever_charge";
    public static final String TIME_WARP = "time_warp";
    public static final String NULLIFY = "nullify";

    /** 전체 아이템 ID. 기존 목록과 같은 순서를 유지한다. */
    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            DAMAGE_BOOST, GARBAGE_BOMB, HEAL, SHIELD, LINE_CLEANER, FEVER_CHARGE, TIME_WARP, NULLIFY));

    private ItemIds() { }
}
