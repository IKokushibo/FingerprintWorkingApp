package org.Fingerprint.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.digitalpersona.uareu.*;

public class Selection
		extends JPanel
		implements ActionListener
{
	private static final long serialVersionUID = 2;
	private static final String ACT_BACK = "back";
	private static final String ACT_REFRESH = "refresh";
	private static final String ACT_GETCAPS = "getcaps";

	private static final Color DARK_GREEN = new Color(15, 76, 26);
	private ReaderCollection m_collection;
	private JList m_listReaders;
	private JDialog m_dlgParent;

	private Selection(ReaderCollection collection) {
		m_collection = collection;

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(400, 600));  // Made taller to accommodate bigger buttons

		// Header Panel (Green part with logo)
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		headerPanel.setBackground(DARK_GREEN);
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Add logo
		ImageIcon logo = new ImageIcon(getClass().getResource("/DTECLogo.png"));
		JLabel logoLabel = new JLabel(logo);
		logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(logoLabel);

		// Add Fingerprint Module text
		JLabel moduleLabel = new JLabel("DTEC Fingerprint Module");
		moduleLabel.setForeground(Color.WHITE);
		moduleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		moduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(Box.createVerticalStrut(10));
		headerPanel.add(moduleLabel);
		headerPanel.add(Box.createVerticalStrut(10));

		// Content Panel (White part)
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Available readers label
		JLabel lblReader = new JLabel("Available readers:");
		lblReader.setFont(new Font("Arial", Font.PLAIN, 14));
		lblReader.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(lblReader);
		contentPanel.add(Box.createVerticalStrut(10));

		// Reader list
		m_listReaders = new JList();
		m_listReaders.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_listReaders.setBackground(Color.WHITE);
		JScrollPane paneReaders = new JScrollPane(m_listReaders);
		paneReaders.setPreferredSize(new Dimension(360, 120));
		paneReaders.setMaximumSize(new Dimension(360, 120));
		paneReaders.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPanel.add(paneReaders);
		contentPanel.add(Box.createVerticalStrut(20));

		// Button Panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(3, 1, 15, 15));  // Changed to GridLayout for equal spacing
		buttonPanel.setBackground(Color.WHITE);
		buttonPanel.setMaximumSize(new Dimension(360, 180));  // Control overall button panel size

		// Create original buttons with new style
		JButton btnRefresh = createStyledButton("Refresh list", ACT_REFRESH);
		JButton btnGetCaps = createStyledButton("Get reader capabilities", ACT_GETCAPS);
		JButton btnBack = createStyledButton("Select Reader", ACT_BACK);

		buttonPanel.add(btnRefresh);
		buttonPanel.add(btnGetCaps);
		buttonPanel.add(btnBack);

		// Wrap button panel in another panel to maintain centering
		JPanel buttonWrapper = new JPanel();
		buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.X_AXIS));
		buttonWrapper.setBackground(Color.WHITE);
		buttonWrapper.add(Box.createHorizontalGlue());
		buttonWrapper.add(buttonPanel);
		buttonWrapper.add(Box.createHorizontalGlue());

		contentPanel.add(buttonWrapper);

		// Add panels to main panel
		add(headerPanel, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
	}

	private JButton createStyledButton(String text, String command) {
		JButton button = new JButton(text);
		button.setActionCommand(command);
		button.addActionListener(this);
		button.setBackground(DARK_GREEN);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Arial", Font.BOLD, 16));  // Increased font size
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(360, 50));  // Made buttons bigger
		button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return button;
	}

	private void doModal(JDialog dlgParent) {
		RefreshList();
		m_dlgParent = dlgParent;
		m_dlgParent.setContentPane(this);
		m_dlgParent.pack();
		m_dlgParent.setLocationRelativeTo(null);
		m_dlgParent.toFront();
		m_dlgParent.setVisible(true);
		m_dlgParent.dispose();
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(ACT_BACK)) {
			m_dlgParent.setVisible(false);
		}
		if(e.getActionCommand().equals(ACT_REFRESH)) {
			RefreshList();
		}
		if(e.getActionCommand().equals(ACT_GETCAPS)) {
			Reader reader = getSelectedReader();
			if(null != reader) Capabilities.Show(reader);
		}
	}

	private void RefreshList() {
		try {
			m_collection.GetReaders();
		}
		catch(UareUException e) {
			MessageBox.DpError("ReaderCollection.GetReaders()", e);
		}

		Vector<String> strNames = new Vector<String>();
		for(int i = 0; i < m_collection.size(); i++) {
			strNames.add(m_collection.get(i).GetDescription().name);
		}
		m_listReaders.setListData(strNames);
	}

	private Reader getSelectedReader() {
		if(-1 == m_listReaders.getSelectedIndex()) return null;
		return m_collection.get(m_listReaders.getSelectedIndex());
	}

	public static Reader Select(ReaderCollection collection) {
		JDialog dlg = new JDialog((JDialog)null, "Select reader", true);
		Selection selection = new Selection(collection);
		selection.doModal(dlg);
		return selection.getSelectedReader();
	}
}
