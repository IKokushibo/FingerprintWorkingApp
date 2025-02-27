package org.Fingerprint.ui;

import com.digitalpersona.uareu.*;
import lombok.extern.slf4j.Slf4j;
import org.Fingerprint.LocalVariable;
import org.Fingerprint.web_socket.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Verification extends JPanel implements ActionListener {
    private static final long serialVersionUID = 6;
    private static final String ACT_BACK = "back";
    private static final Color DARK_GREEN = new Color(15, 76, 26);

    private CaptureThread m_capture;
    private Reader m_reader;
    private Fmd[] m_fmds;
    private JDialog m_dlgParent;
    private JTextArea m_text;
    private static List<Fingerprint> encodedFingerprints;
    private static List<String> registeredFingerprints;

    private static MyStompClient myStompClient;

    private final String m_strPrompt1 = "Verification started...\n\n      Place the registered finger on the reader\n\n";

    private Verification(Reader reader) {
        m_reader = reader;
        m_fmds = new Fmd[1];
        encodedFingerprints = new ArrayList<>();
        registeredFingerprints = new ArrayList<>();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 600));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(DARK_GREEN);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Logo
        JLabel logoLabel = new JLabel();
        logoLabel.setIcon(new ImageIcon(getClass().getResource("/DTECLogo.png")));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel moduleLabel = new JLabel("Fingerprint Module");
        moduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        moduleLabel.setForeground(Color.WHITE);
        moduleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(logoLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(moduleLabel);
        headerPanel.add(Box.createVerticalStrut(10));

        add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(Color.WHITE);

        // Reader selection area
        JLabel lblReader = new JLabel("Verification Area:");
        lblReader.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblReader.setFont(new Font("Arial", Font.PLAIN, 14));
        contentPanel.add(lblReader);
        contentPanel.add(Box.createVerticalStrut(10));

        // Text Area
        m_text = new JTextArea(22, 1);
        m_text.setEditable(false);
        m_text.setFont(new Font("Arial", Font.PLAIN, 14));
        m_text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane paneReader = new JScrollPane(m_text);
        paneReader.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        paneReader.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(paneReader);
        contentPanel.add(Box.createVerticalStrut(20));

        // Back Button
        JButton btnBack = new JButton("Back");
        btnBack.setActionCommand(ACT_BACK);
        btnBack.addActionListener(this);
        btnBack.setBackground(DARK_GREEN);
        btnBack.setForeground(Color.WHITE);
        btnBack.setFont(new Font("Arial", Font.BOLD, 16));
        btnBack.setPreferredSize(new Dimension(180, 50));
        btnBack.setMaximumSize(new Dimension(180, 50));
        btnBack.setFocusPainted(false);
        btnBack.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(btnBack);
        contentPanel.add(Box.createVerticalStrut(10));

        add(contentPanel, BorderLayout.CENTER);
        setOpaque(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ACT_BACK)) {
            StopCaptureThread();
        } else if (e.getActionCommand().equals(CaptureThread.ACT_CAPTURE)) {
            CaptureThread.CaptureEvent evt = (CaptureThread.CaptureEvent) e;
            if (ProcessCaptureResult(evt)) {
                WaitForCaptureThread();
                StartCaptureThread();
            } else {
                m_dlgParent.setVisible(false);
            }
        }
    }

    private void StartCaptureThread() {
        m_capture = new CaptureThread(m_reader, false, Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT);
        m_capture.start(this);
    }

    private void StopCaptureThread() {
        if (null != m_capture) m_capture.cancel();
    }

    private void WaitForCaptureThread() {
        if (null != m_capture) m_capture.join(1000);
    }

    private boolean ProcessCaptureResult(CaptureThread.CaptureEvent evt) {
        boolean bCanceled = false;

        if (null != evt.capture_result) {
            if (null != evt.capture_result.image && Reader.CaptureQuality.GOOD == evt.capture_result.quality) {
                Engine engine = UareUGlobal.GetEngine();

                try {
                    // Create FMD from captured image
                    Fmd capturedFmd = engine.CreateFmd(evt.capture_result.image, Fmd.Format.ANSI_378_2004);
                    m_fmds[0] = capturedFmd;

                    if (null != m_fmds[0]) {
                        boolean matchFound = false;
                        log.info("TRY {}", registeredFingerprints.size());
                        for (var encodedFingerprint : registeredFingerprints) {
                            try {
                                // Decode stored fingerprint
                                byte[] fingerprintBytes = Base64.getDecoder().decode(encodedFingerprint);

                                // Create FMD directly from stored data
                                Fmd storedFmd = UareUGlobal.GetImporter().ImportFmd(fingerprintBytes, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);

                                // Compare fingerprints
                                int falsematch_rate = engine.Compare(m_fmds[0], 0, storedFmd, 0);
                                int target_falsematch_rate = Engine.PROBABILITY_ONE / 100000;

                                if (falsematch_rate < target_falsematch_rate) {
                                    String ip = IpApiUtil.getMyIp();
//                                    log.info("IP Address of Current Device : {}", ip);
                                    if (ip == null) {
                                        JOptionPane.showMessageDialog(null, "Failed to send fingerprint print");
                                        break;
                                    }
                                    Map<String, String> tempMap = new HashMap<>();
                                    tempMap.put("username", LocalVariable.username);
                                    matchFound = true;
                                    m_text.append("Fingerprint matched!\n");
                                    String str = String.format("Dissimilarity score: 0x%x\n", falsematch_rate);
                                    m_text.append(str);
                                    str = String.format("False match rate: %e\n\n", (double) (falsematch_rate) / Engine.PROBABILITY_ONE);
                                    m_text.append(str);
                                    myStompClient.sendValidatedFingerprint(tempMap);

                                    // re-fetch the fingerprints of the current user
                                    fetchFingerprint();
                                    break;
                                }
                            } catch (UareUException e) {
                                m_text.append("Error comparing fingerprints: " + e.getMessage() + "\n");
                            }
                        }

                        if (!matchFound) {
                            m_text.append("The registered fingerprint does not match. \n __________________________________________ \n\n");
                        }

                        // Reset for next capture
                        m_fmds[0] = null;
                        m_text.append(m_strPrompt1);
                    }
                } catch (UareUException e) {
                    MessageBox.DpError("Error processing fingerprint", e);
                    bCanceled = true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error processing fingerprint");
                }
            } else if (Reader.CaptureQuality.CANCELED == evt.capture_result.quality) {
                bCanceled = true;
            } else {
                MessageBox.BadQuality(evt.capture_result.quality);
            }
        } else if (null != evt.exception) {
            MessageBox.DpError("Capture", evt.exception);
            bCanceled = true;
        } else if (null != evt.reader_status) {
            MessageBox.BadStatus(evt.reader_status);
            bCanceled = true;
        }

        return !bCanceled;
    }

    private void doModal(JDialog dlgParent) {
        try {
            m_reader.Open(Reader.Priority.COOPERATIVE);
            StartCaptureThread();
            m_text.append(m_strPrompt1);

            m_dlgParent = dlgParent;
            m_dlgParent.setContentPane(this);
            m_dlgParent.pack();
            m_dlgParent.setLocationRelativeTo(null);
            m_dlgParent.toFront();
            m_dlgParent.setVisible(true);
            m_dlgParent.dispose();

            StopCaptureThread();
            WaitForCaptureThread();
            m_reader.Close();
        } catch (UareUException e) {
            MessageBox.DpError("Reader operation failed", e);
        }
    }

    private static void fetchedRegisteredFingerprints() throws IOException, InterruptedException {
        var fingerprints = FingerprintUtil.getRegisteredFingerprints();
        for (String fingerprint : fingerprints) {
            registeredFingerprints.add(fingerprint);
        }
    }

    private static void fetchFingerprint() throws ExecutionException, InterruptedException {
        if (encodedFingerprints != null)
            encodedFingerprints.clear();

        myStompClient = new MyStompClient(new MessageListener() {
            @Override
            public void onMessageReceive(Map<String, Object> message) throws Exception {
                Object body = message.get("body");
                if (body instanceof Map) {
                    encodedFingerprints.addAll(Objects.requireNonNull(FingerprintUtil.getFingerprints(body)));
                } else {
                    System.out.println("Unexpected body type: " + body.getClass());
                }
            }
        }, "");
    }

    public static void Run(Reader reader) throws ExecutionException, InterruptedException, IOException {
        fetchFingerprint();
        fetchedRegisteredFingerprints();
        JDialog dlg = new JDialog((JDialog) null, "Verification", true);
        Verification verification = new Verification(reader);
        verification.doModal(dlg);
    }
}