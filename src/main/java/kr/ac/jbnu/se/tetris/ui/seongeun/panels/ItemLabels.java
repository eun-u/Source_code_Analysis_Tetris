package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import kr.ac.jbnu.se.tetris.item.ItemIds;

/** 아이템 ID를 화면과 전투 로그에 보여 줄 한국어 이름·행동 문구로 바꾸는 순수 함수 모음 */
final class ItemLabels {
    static final String GARBAGE_BOMB_TEXT = "가비지 폭탄";

    private ItemLabels() { }

    static String name(String id) {
        switch (id) {
            case ItemIds.DAMAGE_BOOST: return "공격 증폭";
            case ItemIds.GARBAGE_BOMB: return GARBAGE_BOMB_TEXT;
            case ItemIds.HEAL: return "회복";
            case ItemIds.SHIELD: return "보호막";
            case ItemIds.LINE_CLEANER: return "줄 정리";
            case ItemIds.FEVER_CHARGE: return "피버 충전";
            case ItemIds.TIME_WARP: return "시간 왜곡";
            case ItemIds.NULLIFY: return "무효화";
            default: return id;
        }
    }

    static String shortName(String id) {
        switch (id) {
            case ItemIds.DAMAGE_BOOST: return "증폭";
            case ItemIds.GARBAGE_BOMB: return "폭탄";
            case ItemIds.HEAL: return "회복";
            case ItemIds.SHIELD: return "방어";
            case ItemIds.LINE_CLEANER: return "정리";
            case ItemIds.FEVER_CHARGE: return "피버";
            case ItemIds.TIME_WARP: return "왜곡";
            case ItemIds.NULLIFY: return "무효화";
            default: return id;
        }
    }

    static String action(String id) {
        if (ItemIds.HEAL.equals(id)) return "회복 사용";
        if (ItemIds.SHIELD.equals(id)) return "보호막 발동";
        if (ItemIds.GARBAGE_BOMB.equals(id)) return GARBAGE_BOMB_TEXT;
        if (ItemIds.NULLIFY.equals(id)) return "무효화";
        return name(id) + " 사용";
    }
}
