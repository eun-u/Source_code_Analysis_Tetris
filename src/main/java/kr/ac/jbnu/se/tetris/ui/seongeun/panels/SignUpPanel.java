package kr.ac.jbnu.se.tetris.ui.seongeun.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.GameButton;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.UniversityPixelTheme;

public class SignUpPanel extends JPanel {
    private final JTextField idField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField passwordConfirmField = new JPasswordField(20);
    private final GameButton registerButton = new GameButton("가입");
    private final GameButton backButton = new GameButton("돌아가기");

    public SignUpPanel() {
        setLayout(new GridBagLayout());
        setBackground(UniversityPixelTheme.BG);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel box = new JPanel(); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(UniversityPixelTheme.PANEL);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 3),
                new EmptyBorder(20, 26, 20, 26)));
        box.setPreferredSize(new Dimension(410, 430));

        JLabel eyebrow = label("RANKED PvP  /  계정 만들기", 13, UniversityPixelTheme.GOLD);
        JLabel title = label("회원가입", 23, UniversityPixelTheme.TEXT);
        box.add(eyebrow); box.add(Box.createVerticalStrut(5)); box.add(title);
        box.add(Box.createVerticalStrut(18));
        box.add(label("이메일", 13, UniversityPixelTheme.TEXT_SUB));
        field(idField); box.add(idField); box.add(Box.createVerticalStrut(10));
        box.add(label("비밀번호", 13, UniversityPixelTheme.TEXT_SUB));
        field(passwordField); box.add(passwordField); box.add(Box.createVerticalStrut(10));
        box.add(label("비밀번호 확인", 13, UniversityPixelTheme.TEXT_SUB));
        field(passwordConfirmField); box.add(passwordConfirmField);
        box.add(Box.createVerticalGlue());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0)); actions.setOpaque(false);
        actions.add(registerButton); actions.add(backButton); box.add(actions);
        add(box);
    }

    private JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text); label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(UniversityPixelTheme.font(size, Font.BOLD));
        return label;
    }
    private void field(JTextField field) {
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setPreferredSize(new Dimension(300, 36));
        field.setFont(UniversityPixelTheme.font(14, Font.PLAIN));
        field.setForeground(UniversityPixelTheme.TEXT);
        field.setBackground(UniversityPixelTheme.PANEL_LIGHT);
        field.setCaretColor(UniversityPixelTheme.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UniversityPixelTheme.LINE, 2),
                new EmptyBorder(5, 8, 5, 8)));
    }

    public void setRegisterAction(ActionListener listener) { registerButton.addActionListener(listener); }
    public void setBackAction(ActionListener listener) { backButton.addActionListener(listener); }
    public String getEmail() { return idField.getText().trim(); }
    public char[] getPassword() { return passwordField.getPassword(); }
    public char[] getPasswordConfirmation() { return passwordConfirmField.getPassword(); }
    public void clearPasswords() { passwordField.setText(""); passwordConfirmField.setText(""); }
    public void setBusy(boolean busy) { registerButton.setEnabled(!busy); }
}
