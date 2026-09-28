package kr.ac.jbnu.se.tetris;

import kr.ac.jbnu.se.tetris.app.TetrisApplication;

/** 기존 Tetris.main 실행 설정 유지 및 EDT 기반 UI 생성 */
public final class Tetris {
    private Tetris() { }
    public static void main(String[] args) { TetrisApplication.main(args); }
}
