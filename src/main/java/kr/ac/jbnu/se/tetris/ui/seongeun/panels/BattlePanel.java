package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.Board;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.CharacterView;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.FeverBar;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.HPBar;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.ItemSlot;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.ItemData;
import kr.ac.jbnu.se.tetris.ui.seongeun.model.PlayerData;
import kr.ac.jbnu.se.tetris.battle.BattleState;
import kr.ac.jbnu.se.tetris.battle.ParticipantState;

public class BattlePanel extends JPanel {

    private JLabel enemyNameLabel;
    private JLabel playerNameLabel;

    private Board enemyBoard;
    private Board playerBoard;

    private JLabel enemyStatusLabel;
    private JLabel playerStatusLabel;

    private HPBar enemyHPBar;
    private HPBar playerHPBar;

    private FeverBar enemyFeverBar;
    private FeverBar playerFeverBar;

    private CharacterView enemyCharacterView;
    private CharacterView playerCharacterView;

    private JPanel enemyCharacterArea;
    private JPanel playerCharacterArea;

    private JPanel enemyItemArea;
    private JPanel playerItemArea;

    // DUMMY - 병합 전 삭제
    private GameButton resultTestButton;

    public BattlePanel() {

        // BattlePanel 전체 레이아웃
        setLayout(new BorderLayout());

        // 상단 제목
        JLabel titleLabel = new JLabel("BATTLE", SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // 상대와 사용자를 좌우로 배치할 영역
        JPanel battleArea = new JPanel(new GridBagLayout());

        // =====================================================
        // 상대 영역 - 왼쪽
        // =====================================================

        JPanel enemyArea = new JPanel();
        enemyArea.setLayout(new BoxLayout(enemyArea, BoxLayout.Y_AXIS));

        enemyNameLabel = new JLabel("Enemy");
        enemyNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        enemyHPBar = new HPBar(100, 100);
        enemyHPBar.setPreferredSize(new Dimension(180, 25));
        enemyHPBar.setMaximumSize(new Dimension(180, 25));
        enemyHPBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        enemyFeverBar = new FeverBar(0, 100);
        enemyFeverBar.setPreferredSize(new Dimension(180, 25));
        enemyFeverBar.setMaximumSize(new Dimension(180, 25));
        enemyFeverBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 캐릭터 표시 영역
        enemyCharacterArea = new JPanel(new BorderLayout());
        enemyCharacterArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 아이템 표시 영역
        enemyItemArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        enemyItemArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 상태 표시
        enemyStatusLabel = new JLabel("0");
        enemyStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 상대 보드
        enemyBoard = new Board(enemyStatusLabel, false);
        enemyBoard.setPreferredSize(new Dimension(180, 360));
        enemyBoard.setMinimumSize(new Dimension(180, 360));
        enemyBoard.setMaximumSize(new Dimension(180, 360));
        enemyBoard.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        enemyBoard.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 상대 화면 구성
        enemyArea.add(enemyNameLabel);
        enemyArea.add(Box.createVerticalStrut(5));

        enemyArea.add(enemyHPBar);
        enemyArea.add(Box.createVerticalStrut(10));

        enemyArea.add(enemyFeverBar);
        enemyArea.add(Box.createVerticalStrut(10));

        enemyArea.add(enemyCharacterArea);
        enemyArea.add(Box.createVerticalStrut(10));

        enemyArea.add(enemyItemArea);
        enemyArea.add(Box.createVerticalStrut(10));

        enemyArea.add(enemyBoard);
        enemyArea.add(Box.createVerticalStrut(5));

        enemyArea.add(enemyStatusLabel);

        // =====================================================
        // 사용자 영역 - 오른쪽
        // =====================================================

        JPanel playerArea = new JPanel();
        playerArea.setLayout(new BoxLayout(playerArea, BoxLayout.Y_AXIS));

        playerNameLabel = new JLabel("Player");
        playerNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        playerHPBar = new HPBar(100, 100);
        playerHPBar.setPreferredSize(new Dimension(240, 25));
        playerHPBar.setMaximumSize(new Dimension(240, 25));
        playerHPBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        playerFeverBar = new FeverBar(0, 100);
        playerFeverBar.setPreferredSize(new Dimension(240, 25));
        playerFeverBar.setMaximumSize(new Dimension(240, 25));
        playerFeverBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 캐릭터 표시 영역
        playerCharacterArea = new JPanel(new BorderLayout());
        playerCharacterArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 아이템 표시 영역
        playerItemArea = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        playerItemArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 상태 표시
        playerStatusLabel = new JLabel("0");
        playerStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 사용자 보드
        playerBoard = new Board(playerStatusLabel, true);
        playerBoard.setPreferredSize(new Dimension(240, 440));
        playerBoard.setMinimumSize(new Dimension(240, 440));
        playerBoard.setMaximumSize(new Dimension(240, 440));
        playerBoard.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        playerBoard.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 사용자 화면 구성
        playerArea.add(playerNameLabel);
        playerArea.add(Box.createVerticalStrut(5));

        playerArea.add(playerHPBar);
        playerArea.add(Box.createVerticalStrut(10));

        playerArea.add(playerFeverBar);
        playerArea.add(Box.createVerticalStrut(10));

        playerArea.add(playerCharacterArea);
        playerArea.add(Box.createVerticalStrut(10));

        playerArea.add(playerItemArea);
        playerArea.add(Box.createVerticalStrut(10));

        playerArea.add(playerBoard);
        playerArea.add(Box.createVerticalStrut(5));

        playerArea.add(playerStatusLabel);

        // =====================================================
        // 좌우 배치
        // =====================================================

        GridBagConstraints enemyConstraints = new GridBagConstraints();
        enemyConstraints.gridx = 0;
        enemyConstraints.gridy = 0;
        enemyConstraints.insets = new Insets(20, 20, 20, 40);

        battleArea.add(enemyArea, enemyConstraints);

        GridBagConstraints playerConstraints = new GridBagConstraints();
        playerConstraints.gridx = 1;
        playerConstraints.gridy = 0;
        playerConstraints.insets = new Insets(20, 40, 20, 20);

        battleArea.add(playerArea, playerConstraints);

        add(battleArea, BorderLayout.CENTER);

        // DUMMY - 결과 화면 이동 테스트용
        resultTestButton = new GameButton("Result Test");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(resultTestButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setPlayers(PlayerData myPlayer, PlayerData enemyPlayer) {

        playerNameLabel.setText(myPlayer.getNickname());
        enemyNameLabel.setText(enemyPlayer.getNickname());

        // 기존 캐릭터 화면 제거
        enemyCharacterArea.removeAll();
        playerCharacterArea.removeAll();

        // 전달받은 PlayerData로 CharacterView 생성
        enemyCharacterView = new CharacterView(enemyPlayer);
        playerCharacterView = new CharacterView(myPlayer);

        enemyCharacterArea.add(enemyCharacterView, BorderLayout.CENTER);
        playerCharacterArea.add(playerCharacterView, BorderLayout.CENTER);

        // 화면 갱신
        enemyCharacterArea.revalidate();
        enemyCharacterArea.repaint();

        playerCharacterArea.revalidate();
        playerCharacterArea.repaint();
    }

    public void setItems(ItemData[] playerItems, ItemData[] enemyItems) {

        playerItemArea.removeAll();
        enemyItemArea.removeAll();

        String[] keys = {"Q", "W", "E"};

        for (int i = 0; i < 3; i++) {

            ItemData playerItem = i < playerItems.length ? playerItems[i] : null;
            ItemData enemyItem = i < enemyItems.length ? enemyItems[i] : null;

            enemyItemArea.add(new ItemSlot(keys[i], enemyItem));
            playerItemArea.add(new ItemSlot(keys[i], playerItem));
        }

        enemyItemArea.revalidate();
        enemyItemArea.repaint();

        playerItemArea.revalidate();
        playerItemArea.repaint();
    }

    // 사용자 Board 게임 시작
    public void startBattle() {
        playerBoard.start();
        SwingUtilities.invokeLater(() -> playerBoard.requestFocusInWindow());
    }

    // DUMMY - 결과 화면 이동 테스트용
    public void setResultAction(ActionListener listener) {
        resultTestButton.addActionListener(listener);
    }

    public Board getPlayerBoard() {
        return playerBoard;
    }

    public Board getEnemyBoard() {
        return enemyBoard;
    }

    /** 원본 화면의 기존 자리마다 실제 전투 상태를 표시한다. */
    public void setState(BattleState state, String localId) {
        if (state == null || localId == null) return;
        ParticipantState local = state.getParticipant(localId);
        if (local == null) return;
        ParticipantState opponent = null;
        for (ParticipantState participant : state.getParticipants().values()) {
            if (!localId.equals(participant.getId())) {
                opponent = participant;
                break;
            }
        }
        if (opponent == null) return;

        playerNameLabel.setText(local.getName());
        enemyNameLabel.setText(opponent.getName());
        playerHPBar.setHP(local.getHp(), local.getMaxHp());
        enemyHPBar.setHP(opponent.getHp(), opponent.getMaxHp());
        playerBoard.setState(local.getGameState());
        enemyBoard.setState(opponent.getGameState());
    }
}
