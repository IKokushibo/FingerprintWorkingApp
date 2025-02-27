package org.Fingerprint;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.Map;

public class LoginScreen extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private static final String ACT_LOGIN = "login";
    private static final String ACT_CANCEL = "cancel";

    // Colors
    private static final Color PRIMARY_COLOR = new Color(15, 76, 26);
    private static final Color SECONDARY_COLOR = new Color(220, 53, 69);
    private static final Color FIELD_BORDER_COLOR = new Color(200, 200, 200);
    private static final Color FIELD_FOCUS_COLOR = new Color(44, 123, 52);

    // Simple in-memory user database (in a real app, this would be stored securely)
    private static final Map<String, String> USERS = new HashMap<>();
    static {
        USERS.put("admin", "admin123");
        USERS.put("user", "user123");
    }

    private JDialog m_dlgParent;
    private JTextField m_textUsername;
    private JPasswordField m_textPassword;
    private boolean m_loginSuccess = false;

    public LoginScreen() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 520)); // Slightly increased height


        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));


        JLabel logoLabel = new JLabel();
        logoLabel.setIcon(new ImageIcon(getClass().getResource("/DTECLogo.png")));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JLabel moduleLabel = new JLabel("DTEC Fingerprint Login");
        moduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        moduleLabel.setForeground(Color.WHITE);
        moduleLabel.setFont(new Font("Arial", Font.BOLD, 20)); // Increased font size

        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(logoLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(moduleLabel);
        headerPanel.add(Box.createVerticalStrut(5));

        add(headerPanel, BorderLayout.NORTH);


        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        contentPanel.setBackground(Color.WHITE);


        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.BOLD, 16));
        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lblUsername);
        contentPanel.add(Box.createVerticalStrut(8));

        m_textUsername = createStyledTextField();
        contentPanel.add(m_textUsername);
        contentPanel.add(Box.createVerticalStrut(20));

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Arial", Font.BOLD, 16));
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lblPassword);
        contentPanel.add(Box.createVerticalStrut(8));

        m_textPassword = createStyledPasswordField();
        contentPanel.add(m_textPassword);
        contentPanel.add(Box.createVerticalStrut(30));


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton loginButton = createStyledButton("Login", ACT_LOGIN, PRIMARY_COLOR);
        JButton cancelButton = createStyledButton("Cancel", ACT_CANCEL, SECONDARY_COLOR);

        buttonPanel.add(loginButton);
        buttonPanel.add(Box.createHorizontalGlue()); // This pushes the buttons apart
        buttonPanel.add(cancelButton);

        contentPanel.add(buttonPanel);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 16));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Increased height
        textField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create rounded border with padding
        textField.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER_COLOR, 1, true), // Outer border - rounded
                new EmptyBorder(5, 10, 5, 10)  // Inner padding
        ));


        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                textField.setBorder(new CompoundBorder(
                        new LineBorder(FIELD_FOCUS_COLOR, 2, true),
                        new EmptyBorder(4, 9, 4, 9)  // Adjusted to maintain same size
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                textField.setBorder(new CompoundBorder(
                        new LineBorder(FIELD_BORDER_COLOR, 1, true),
                        new EmptyBorder(5, 10, 5, 10)
                ));
            }
        });

        return textField;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); // Increased height
        passwordField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create rounded border with padding
        passwordField.setBorder(new CompoundBorder(
                new LineBorder(FIELD_BORDER_COLOR, 1, true), // Outer border - rounded
                new EmptyBorder(5, 10, 5, 10)  // Inner padding
        ));


        passwordField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(new CompoundBorder(
                        new LineBorder(FIELD_FOCUS_COLOR, 2, true),
                        new EmptyBorder(4, 9, 4, 9)  // Adjusted to maintain same size
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(new CompoundBorder(
                        new LineBorder(FIELD_BORDER_COLOR, 1, true),
                        new EmptyBorder(5, 10, 5, 10)
                ));
            }
        });

        return passwordField;
    }

    private JButton createStyledButton(String text, String command, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setFocusPainted(false);


        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)
        ));

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 50)); // Increased height


        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ACT_LOGIN)) {
            String username = m_textUsername.getText();
            String password = new String(m_textPassword.getPassword());

            if (validateLogin(username, password)) {
                m_loginSuccess = true;
                m_dlgParent.setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getActionCommand().equals(ACT_CANCEL)) {
            m_loginSuccess = false;
            m_dlgParent.setVisible(false);
        }
    }

    private boolean validateLogin(String username, String password) {
        return USERS.containsKey(username) && USERS.get(username).equals(password);
    }

    public boolean doModal(JDialog dlgParent) {
        m_dlgParent = dlgParent;
        m_dlgParent.setContentPane(this);
        m_dlgParent.pack();
        m_dlgParent.setLocationRelativeTo(null);
        m_dlgParent.setVisible(true);

        return m_loginSuccess;
    }
//    Hello

    public static boolean showLoginDialog() {
        LoginScreen loginScreen = new LoginScreen();
        JDialog dlg = new JDialog((JDialog)null, "DTEC Fingerprint Login", true);

        try {
            ImageIcon icon = new ImageIcon(LoginScreen.class.getResource("/DTECLogo.png"));
            dlg.setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Icon not found: " + e.getMessage());
        }

        boolean result = loginScreen.doModal(dlg);
        dlg.dispose();

        return result;
    }
}