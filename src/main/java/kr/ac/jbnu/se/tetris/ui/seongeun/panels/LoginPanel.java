package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;

public class LoginPanel extends JPanel {

    private JTextField idField;
    private JPasswordField passwordField;

    private GameButton loginButton;
    private GameButton signUpButton;

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(13, 23, 40));

        JPanel loginBox = new JPanel();
        loginBox.setOpaque(false);
        loginBox.setLayout(new BoxLayout(loginBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("TETRIS MONSTER");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        idField = new JTextField(15);
        passwordField = new JPasswordField(15);

        idField.setMaximumSize(idField.getPreferredSize());
        passwordField.setMaximumSize(passwordField.getPreferredSize());

        loginButton = new GameButton("로컬 시작");
        signUpButton = new GameButton("회원가입");

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBox.add(titleLabel);
        loginBox.add(Box.createVerticalStrut(15));
        JLabel localNotice = new JLabel("계정 연동 없이 이 PC의 저장 데이터를 사용합니다.");
        localNotice.setForeground(new Color(174, 207, 221));
        loginBox.add(localNotice);

        loginBox.add(Box.createVerticalStrut(15));
        loginBox.add(loginButton);

        loginBox.add(Box.createVerticalStrut(5));
        loginBox.add(signUpButton);
        signUpButton.setEnabled(false);
        signUpButton.setToolTipText("계정 서비스는 현재 제공하지 않습니다.");

        add(loginBox);
    }

    public void setLoginAction(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void setSignUpAction(ActionListener listener) {
        signUpButton.addActionListener(listener);
    }

    
}
