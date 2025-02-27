package org.Fingerprint;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import org.Fingerprint.ui.Enrollment;
import org.Fingerprint.ui.MessageBox;
import org.Fingerprint.ui.Selection;
import org.Fingerprint.ui.Verification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class DTECFingerprint extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1;

	private static final String ACT_SELECTION = "selection";
	private static final String ACT_VERIFICATION = "verification";
	private static final String ACT_ENROLLMENT = "enrollment";
	private static final String ACT_EXIT = "exit";

	private JDialog m_dlgParent;
	private JTextArea m_textReader;
	private ReaderCollection m_collection;
	private Reader m_reader;

	private DTECFingerprint() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(400, 600));

		// Header panel with logo and title
		JPanel headerPanel = new JPanel();
		headerPanel.setBackground(new Color(15, 76, 26));
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Logo
		JLabel logoLabel = new JLabel();
		logoLabel.setIcon(new ImageIcon(getClass().getResource("/DTECLogo.png")));
		logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Title
		JLabel moduleLabel = new JLabel("DTEC Fingerprint Module");
		moduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		moduleLabel.setForeground(Color.WHITE);
		moduleLabel.setFont(new Font("Arial", Font.BOLD, 18));

		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(logoLabel);
		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(moduleLabel);
		headerPanel.add(Box.createVerticalStrut(10));

		add(headerPanel, BorderLayout.NORTH);

		// Content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.setBackground(Color.WHITE);

		// Reader selection area
		JLabel lblReader = new JLabel("Select Reader:");
		lblReader.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblReader.setFont(new Font("Arial", Font.PLAIN, 14));
		contentPanel.add(lblReader);
		contentPanel.add(Box.createVerticalStrut(10));

		m_textReader = new JTextArea(1, 15);
		m_textReader.setEditable(false);
		m_textReader.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		m_textReader.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(m_textReader);
		contentPanel.add(Box.createVerticalStrut(20));

		// Buttons panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2, 2, 15, 15));
		buttonPanel.setBackground(Color.WHITE);

		addStyledButton(buttonPanel, "Select New Reader", ACT_SELECTION);
		addStyledButton(buttonPanel, "Run Verification", ACT_VERIFICATION);
		addStyledButton(buttonPanel, "Run Enrollment", ACT_ENROLLMENT);
		addStyledButton(buttonPanel, "Exit", ACT_EXIT, new Color(220, 53, 69)); // Red exit button

		contentPanel.add(buttonPanel);

		add(contentPanel, BorderLayout.CENTER);
	}

	private void addStyledButton(JPanel panel, String text, String command) {
		addStyledButton(panel, text, command, new Color(15, 76, 26));
	}

	private void addStyledButton(JPanel panel, String text, String command, Color backgroundColor) {
		panel.add(createStyledButton(text, command, backgroundColor));
	}

	private JButton createStyledButton(String text, String command, Color backgroundColor) {
		JButton button = new JButton(text);
		button.setActionCommand(command);
		button.addActionListener(this);
		button.setBackground(backgroundColor);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Arial", Font.BOLD, 16));
		button.setPreferredSize(new Dimension(180, 50));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return button;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(ACT_SELECTION)) {
			m_reader = Selection.Select(m_collection);
			if(null != m_reader) {
				m_textReader.setText(m_reader.GetDescription().name);
			} else {
				m_textReader.setText("");
			}
		} else if(e.getActionCommand().equals(ACT_VERIFICATION)) {
			if(null == m_reader) {
				MessageBox.Warning("Reader is not selected");
			} else {
				try {
					Verification.Run(m_reader);
				} catch (ExecutionException | InterruptedException ex) {
					throw new RuntimeException(ex);
				} catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
		} else if(e.getActionCommand().equals(ACT_ENROLLMENT)) {
			if(null == m_reader) {
				MessageBox.Warning("Reader is not selected");
			} else {
				try {
					Enrollment.Run(m_reader);
				} catch (URISyntaxException | InterruptedException | ExecutionException ex) {
					throw new RuntimeException(ex);
				}
			}
		} else if(e.getActionCommand().equals(ACT_EXIT)) {
			m_dlgParent.setVisible(false);
		}
	}

	private void doModal(JDialog dlgParent) {
		m_dlgParent = dlgParent;
		m_dlgParent.setContentPane(this);
		m_dlgParent.pack();
		m_dlgParent.setLocationRelativeTo(null);
		m_dlgParent.setVisible(true);
		m_dlgParent.dispose();
	}

	private static void createAndShowGUI() {
		DTECFingerprint paneContent = new DTECFingerprint();

		try {
			paneContent.m_collection = UareUGlobal.GetReaderCollection();
		} catch(UareUException e) {
			MessageBox.DpError("UareUGlobal.getReaderCollection()", e);
			return;
		}

		JDialog dlg = new JDialog((JDialog)null, "DTEC Fingerprint Application", true);
		try {
			ImageIcon icon = new ImageIcon(DTECFingerprint.class.getResource("/DTECLogo.png"));
			dlg.setIconImage(icon.getImage());  // Set as taskbar icon
		} catch (Exception e) {
			System.err.println("Icon not found: " + e.getMessage());
		}
		paneContent.doModal(dlg);

		try {
			UareUGlobal.DestroyReaderCollection();
		} catch(UareUException e) {
			MessageBox.DpError("UareUGlobal.destroyReaderCollection()", e);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Show login screen first
				boolean loginSuccess = LoginScreen.showLoginDialog();

				// Only proceed to main application if login was successful
				if (loginSuccess) {
					createAndShowGUI();
				} else {
					System.exit(0);
				}
			}
		});
	}
}