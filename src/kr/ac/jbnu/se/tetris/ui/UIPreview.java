package kr.ac.jbnu.se.tetris.ui;

import java.awt.CardLayout;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import kr.ac.jbnu.se.tetris.ui.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.model.PlayerData;
import kr.ac.jbnu.se.tetris.ui.model.RoomData;
import kr.ac.jbnu.se.tetris.ui.model.StoryProgressData;
import kr.ac.jbnu.se.tetris.ui.panels.BattlePanel;
import kr.ac.jbnu.se.tetris.ui.panels.LocalGamePanel;
import kr.ac.jbnu.se.tetris.ui.panels.LocalModePanel;
import kr.ac.jbnu.se.tetris.ui.panels.LoginPanel;
import kr.ac.jbnu.se.tetris.ui.panels.MainLobbyPanel;
import kr.ac.jbnu.se.tetris.ui.panels.ResultPanel;
import kr.ac.jbnu.se.tetris.ui.panels.RoomListPanel;
import kr.ac.jbnu.se.tetris.ui.panels.SignUpPanel;
import kr.ac.jbnu.se.tetris.ui.panels.StoryStageSelectPanel;
import kr.ac.jbnu.se.tetris.ui.panels.WaitingRoomPanel;

public class UIPreview {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // =====================================================
            // Frame
            // =====================================================

            JFrame frame = new JFrame("Tetris UI Preview");

            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);


            // =====================================================
            // 화면 전환용 CardLayout
            // =====================================================

            CardLayout cardLayout = new CardLayout();
            JPanel screenPanel = new JPanel(cardLayout);


            // =====================================================
            // 화면 생성
            // =====================================================

            LoginPanel loginPanel = new LoginPanel();
            SignUpPanel signUpPanel = new SignUpPanel();

            MainLobbyPanel lobbyPanel = new MainLobbyPanel();

            StoryStageSelectPanel storyStageSelectPanel = new StoryStageSelectPanel();

            RoomListPanel roomListPanel = new RoomListPanel();
            WaitingRoomPanel waitingRoomPanel = new WaitingRoomPanel();

            BattlePanel battlePanel = new BattlePanel();
            ResultPanel resultPanel = new ResultPanel();

            LocalModePanel localModePanel = new LocalModePanel();
            LocalGamePanel localGamePanel = new LocalGamePanel();


            // =====================================================
            // 화면 등록
            // =====================================================

            screenPanel.add(loginPanel, "LOGIN");
            screenPanel.add(signUpPanel, "SIGN_UP");

            screenPanel.add(lobbyPanel, "LOBBY");

            screenPanel.add(storyStageSelectPanel, "STORY_STAGE");

            screenPanel.add(roomListPanel, "ROOM_LIST");
            screenPanel.add(waitingRoomPanel, "WAITING_ROOM");

            screenPanel.add(battlePanel, "BATTLE");
            screenPanel.add(resultPanel, "RESULT");

            screenPanel.add(localModePanel, "LOCAL_MODE");
            screenPanel.add(localGamePanel, "LOCAL_GAME");


            // =====================================================
            // DUMMY DATA
            // 나중에 실제 서버 / 게임 데이터와 연결할 부분
            // =====================================================

            List<RoomData> dummyRooms = List.of(
                new RoomData("Room 01", 1, 2, "Waiting"),
                new RoomData("Room 02", 2, 2, "Playing"),
                new RoomData("Room 03", 1, 2, "Waiting"),
                new RoomData("Room 04", 2, 2, "Waiting"),
                new RoomData("Room 05", 1, 2, "Waiting")
            );

            PlayerData myPlayer = new PlayerData("MYplayer", 12, "Basic Character", false);

            PlayerData enemyPlayer = new PlayerData("Enemy01", 10, "Enemy Character", true);

            PlayerData storyEnemy = new PlayerData("Story Enemy", 10, "Enemy Character", true);


            ItemData[] playerItems = {
                new ItemData("potion", "Potion"),
                new ItemData("potion", "Potion"),
                new ItemData("shield", "Shield")
            };

            ItemData[] enemyItems = {
                new ItemData("bomb", "Bomb"),
                new ItemData("shield", "Shield")
            };


            // =====================================================
            // Story 진행도 DUMMY
            //
            // 0 = Stage 1 일반
            // 1 = Stage 1 엘리트
            // 2 = Stage 1 보스
            // 3 = Stage 2 일반
            // 4 = Stage 2 엘리트
            // 5 = Stage 2 보스
            // 6 = Stage 3 일반
            // 7 = Stage 3 엘리트
            // 8 = Stage 3 보스
            //
            // 현재 5이므로 Stage 2 Boss까지 열려 있고
            // Stage 3은 Locked 상태
            // =====================================================

            StoryProgressData storyProgress = new StoryProgressData(5);

            storyStageSelectPanel.updateProgress(storyProgress);


            // =====================================================
            // 현재 선택된 Story 정보
            // =====================================================

            int[] selectedStoryStage = {0};
            int[] selectedStoryDifficulty = {0};


            // =====================================================
            // 현재 Battle 종류
            //
            // STORY
            // ONLINE
            // =====================================================

            String[] battleMode = {"ONLINE"};


            // =====================================================
            // 새로 만든 방인지 확인하기 위한 값
            // =====================================================

            boolean[] createdRoomMode = {false};


            // =====================================================
            // Login → Lobby
            // =====================================================

            loginPanel.setLoginAction(e -> {
                cardLayout.show(screenPanel, "LOBBY");
            });


            // =====================================================
            // Login → Sign Up
            // =====================================================

            loginPanel.setSignUpAction(e -> {
                cardLayout.show(screenPanel, "SIGN_UP");
            });


            // =====================================================
            // Sign Up → Login
            // =====================================================

            signUpPanel.setBackAction(e -> {
                cardLayout.show(screenPanel, "LOGIN");
            });


            // =====================================================
            // 회원가입 테스트
            // 실제 회원가입 로직은 나중에 서버와 연결
            // =====================================================

            signUpPanel.setRegisterAction(e -> {
                JOptionPane.showMessageDialog(frame, "회원가입 테스트 완료");
                cardLayout.show(screenPanel, "LOGIN");
            });


            // =====================================================
            // Lobby → Story Stage Select
            // =====================================================

            lobbyPanel.setStoryAction(e -> {
                storyStageSelectPanel.updateProgress(storyProgress);
                cardLayout.show(screenPanel, "STORY_STAGE");
            });


            // =====================================================
            // Story Stage Select → Lobby
            // =====================================================

            storyStageSelectPanel.setBackAction(e -> {
                cardLayout.show(screenPanel, "LOBBY");
            });


            // =====================================================
            // Story Stage 1 - 일반
            // =====================================================

            storyStageSelectPanel.setStage1NormalAction(e -> {
                startStoryBattle(1, StoryProgressData.NORMAL, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 1 - 엘리트
            // =====================================================

            storyStageSelectPanel.setStage1EliteAction(e -> {
                startStoryBattle(1, StoryProgressData.ELITE, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 1 - 보스
            // =====================================================

            storyStageSelectPanel.setStage1BossAction(e -> {
                startStoryBattle(1, StoryProgressData.BOSS, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 2 - 일반
            // =====================================================

            storyStageSelectPanel.setStage2NormalAction(e -> {
                startStoryBattle(2, StoryProgressData.NORMAL, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 2 - 엘리트
            // =====================================================

            storyStageSelectPanel.setStage2EliteAction(e -> {
                startStoryBattle(2, StoryProgressData.ELITE, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 2 - 보스
            // =====================================================

            storyStageSelectPanel.setStage2BossAction(e -> {
                startStoryBattle(2, StoryProgressData.BOSS, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 3 - 일반
            // Locked 상태에서는 버튼 자체가 비활성화됨
            // =====================================================

            storyStageSelectPanel.setStage3NormalAction(e -> {
                startStoryBattle(3, StoryProgressData.NORMAL, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 3 - 엘리트
            // =====================================================

            storyStageSelectPanel.setStage3EliteAction(e -> {
                startStoryBattle(3, StoryProgressData.ELITE, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Story Stage 3 - 보스
            // =====================================================

            storyStageSelectPanel.setStage3BossAction(e -> {
                startStoryBattle(3, StoryProgressData.BOSS, selectedStoryStage, selectedStoryDifficulty,
                    battleMode, myPlayer, storyEnemy, playerItems, enemyItems,
                    battlePanel, resultPanel, cardLayout, screenPanel);
            });


            // =====================================================
            // Lobby → Online Battle
            // =====================================================

            lobbyPanel.setOnlineBattleAction(e -> {
                cardLayout.show(screenPanel, "ROOM_LIST");
            });


            // =====================================================
            // Room List → Lobby
            // =====================================================

            roomListPanel.setBackAction(e -> {
                cardLayout.show(screenPanel, "LOBBY");
            });


            // =====================================================
            // Room List
            // 기존 방 입장
            // =====================================================

            roomListPanel.setRooms(dummyRooms, room -> {

                battleMode[0] = "ONLINE";
                createdRoomMode[0] = false;

                myPlayer.setReady(false);

                // 현재는 READY 테스트를 위해 상대는 준비 완료 상태
                enemyPlayer.setReady(true);

                waitingRoomPanel.setRoom(room);
                waitingRoomPanel.setPlayers(myPlayer, enemyPlayer);

                cardLayout.show(screenPanel, "WAITING_ROOM");
            });


            // =====================================================
            // Room List
            // 방 만들기
            // =====================================================

            roomListPanel.setCreateRoomAction(e -> {

                String roomName = JOptionPane.showInputDialog(
                    frame,
                    "방 이름을 입력하세요.",
                    "방 만들기",
                    JOptionPane.PLAIN_MESSAGE
                );

                if (roomName == null) {
                    return;
                }

                roomName = roomName.trim();

                if (roomName.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "방 이름을 입력해주세요.");
                    return;
                }

                battleMode[0] = "ONLINE";
                createdRoomMode[0] = true;

                myPlayer.setReady(false);

                RoomData createdRoom = new RoomData(roomName, 1, 2, "Waiting");

                waitingRoomPanel.setRoom(createdRoom);

                // 상대가 아직 없는 상태
                waitingRoomPanel.setPlayers(myPlayer, null);

                cardLayout.show(screenPanel, "WAITING_ROOM");
            });


            // =====================================================
            // Waiting Room READY
            // =====================================================

            waitingRoomPanel.setReadyAction(e -> {

                myPlayer.setReady(!myPlayer.isReady());


                // -------------------------------------------------
                // 내가 만든 방
                // 아직 상대가 없으므로 Battle 시작 안 함
                // -------------------------------------------------

                if (createdRoomMode[0]) {
                    waitingRoomPanel.setPlayers(myPlayer, null);
                    return;
                }


                // -------------------------------------------------
                // 기존 방 입장
                // -------------------------------------------------

                waitingRoomPanel.setPlayers(myPlayer, enemyPlayer);

                if (myPlayer.isReady() && enemyPlayer.isReady()) {

                    battleMode[0] = "ONLINE";

                    battlePanel.setPlayers(myPlayer, enemyPlayer);
                    battlePanel.setItems(playerItems, enemyItems);

                    resultPanel.setReturnButtonText("대기방으로");

                    cardLayout.show(screenPanel, "BATTLE");

                    battlePanel.startBattle();
                }
            });


            // =====================================================
            // Waiting Room → Room List
            // =====================================================

            waitingRoomPanel.setBackAction(e -> {

                myPlayer.setReady(false);
                createdRoomMode[0] = false;

                cardLayout.show(screenPanel, "ROOM_LIST");
            });


            // =====================================================
            // Battle → Result
            //
            // 현재 BattlePanel의 Result Test 버튼을
            // 임시 승리 판정으로 사용
            // =====================================================

            battlePanel.setResultAction(e -> {

                // -------------------------------------------------
                // Story Battle이었다면
                // 선택한 Story Stage를 클리어 처리
                // -------------------------------------------------

                if (battleMode[0].equals("STORY")) {

                    storyProgress.clearStage(
                        selectedStoryStage[0],
                        selectedStoryDifficulty[0]
                    );

                    storyStageSelectPanel.updateProgress(storyProgress);
                }


                // -------------------------------------------------
                // Result DUMMY DATA
                // -------------------------------------------------

                resultPanel.setResult(
                    "VICTORY",
                    myPlayer.getNickname(),
                    12,
                    5,
                    320,
                    100
                );

                cardLayout.show(screenPanel, "RESULT");
            });


            // =====================================================
            // Result → 이전 화면
            // =====================================================

            resultPanel.setReturnAction(e -> {

                // -------------------------------------------------
                // Story 결과
                // → Story Stage Select
                // -------------------------------------------------

                if (battleMode[0].equals("STORY")) {

                    storyStageSelectPanel.updateProgress(storyProgress);

                    cardLayout.show(screenPanel, "STORY_STAGE");

                    return;
                }


                // -------------------------------------------------
                // Online 결과
                // → Waiting Room
                // -------------------------------------------------

                myPlayer.setReady(false);
                enemyPlayer.setReady(true);

                waitingRoomPanel.setPlayers(myPlayer, enemyPlayer);

                cardLayout.show(screenPanel, "WAITING_ROOM");
            });


            // =====================================================
            // Result → Lobby
            // =====================================================

            resultPanel.setLobbyAction(e -> {

                myPlayer.setReady(false);
                createdRoomMode[0] = false;

                cardLayout.show(screenPanel, "LOBBY");
            });


            // =====================================================
            // Lobby → Local Mode
            // =====================================================

            lobbyPanel.setLocalModeAction(e -> {
                cardLayout.show(screenPanel, "LOCAL_MODE");
            });


            // =====================================================
            // Local Mode → Lobby
            // =====================================================

            localModePanel.setBackAction(e -> {
                cardLayout.show(screenPanel, "LOBBY");
            });


            // =====================================================
            // Local Mode → Infinite
            // =====================================================

            localModePanel.setInfiniteAction(e -> {

                cardLayout.show(screenPanel, "LOCAL_GAME");

                localGamePanel.startMode("Infinite");
            });


            // =====================================================
            // Local Mode → Sprint
            // =====================================================

            localModePanel.setSprintAction(e -> {

                cardLayout.show(screenPanel, "LOCAL_GAME");

                localGamePanel.startMode("Sprint");
            });


            // =====================================================
            // Local Game → Local Mode
            // =====================================================

            localGamePanel.setBackAction(e -> {
                cardLayout.show(screenPanel, "LOCAL_MODE");
            });


            // =====================================================
            // Frame 실행
            // =====================================================

            frame.add(screenPanel);

            cardLayout.show(screenPanel, "LOGIN");

            frame.setVisible(true);
        });
    }


    // =============================================================
    // Story Battle 공통 시작 메서드
    // =============================================================

    private static void startStoryBattle(
        int stage,
        int difficulty,
        int[] selectedStoryStage,
        int[] selectedStoryDifficulty,
        String[] battleMode,
        PlayerData myPlayer,
        PlayerData storyEnemy,
        ItemData[] playerItems,
        ItemData[] enemyItems,
        BattlePanel battlePanel,
        ResultPanel resultPanel,
        CardLayout cardLayout,
        JPanel screenPanel
    ) {

        selectedStoryStage[0] = stage;
        selectedStoryDifficulty[0] = difficulty;

        battleMode[0] = "STORY";

        battlePanel.setPlayers(myPlayer, storyEnemy);
        battlePanel.setItems(playerItems, enemyItems);

        resultPanel.setReturnButtonText("Story로");

        cardLayout.show(screenPanel, "BATTLE");

        battlePanel.startBattle();
    }
}