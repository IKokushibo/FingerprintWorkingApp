package org.Fingerprint.ui;

import com.digitalpersona.uareu.*;
import lombok.extern.slf4j.Slf4j;
import org.Fingerprint.web_socket.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Enrollment
        extends JPanel
        implements ActionListener {

    private static MyStompClient myStompClient;
    private static final Color DARK_GREEN = new Color(15, 76, 26);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 14);


    public class EnrollmentThread
            extends Thread
            implements Engine.EnrollmentCallback {
        public static final String ACT_PROMPT = "enrollment_prompt";
        public static final String ACT_CAPTURE = "enrollment_capture";
        public static final String ACT_FEATURES = "enrollment_features";
        public static final String ACT_DONE = "enrollment_done";
        public static final String ACT_CANCELED = "enrollment_canceled";

        public class EnrollmentEvent extends ActionEvent {
            private static final long serialVersionUID = 102;

            public Reader.CaptureResult capture_result;
            public Reader.Status reader_status;
            public UareUException exception;
            public Fmd enrollment_fmd;

            public EnrollmentEvent(Object source, String action, Fmd fmd, Reader.CaptureResult cr, Reader.Status st, UareUException ex) {
                super(source, ActionEvent.ACTION_PERFORMED, action);
                capture_result = cr;
                reader_status = st;
                exception = ex;
                enrollment_fmd = fmd;
            }
        }

        private final Reader m_reader;
        private CaptureThread m_capture;
        private ActionListener m_listener;
        private boolean m_bCancel;

        protected EnrollmentThread(Reader reader, ActionListener listener) {
            m_reader = reader;
            m_listener = listener;
        }

        public Engine.PreEnrollmentFmd GetFmd(Fmd.Format format) {
            Engine.PreEnrollmentFmd prefmd = null;

            while (null == prefmd && !m_bCancel) {
                //start capture thread
                m_capture = new CaptureThread(m_reader, false, Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT);
                m_capture.start(null);

                //prompt for finger
                SendToListener(ACT_PROMPT, null, null, null, null);

                //wait till done
                m_capture.join(0);

                //check result
                CaptureThread.CaptureEvent evt = m_capture.getLastCaptureEvent();
                if (null != evt.capture_result) {
                    if (Reader.CaptureQuality.CANCELED == evt.capture_result.quality) {
                        //capture canceled, return null
                        break;
                    } else if (null != evt.capture_result.image && Reader.CaptureQuality.GOOD == evt.capture_result.quality) {
                        //acquire engine
                        Engine engine = UareUGlobal.GetEngine();

                        try {
                            //extract features
                            Fmd fmd = engine.CreateFmd(evt.capture_result.image, Fmd.Format.ANSI_378_2004);

                            //return prefmd
                            prefmd = new Engine.PreEnrollmentFmd();
                            prefmd.fmd = fmd;
                            prefmd.view_index = 0;

                            //send success
                            SendToListener(ACT_FEATURES, null, null, null, null);
                        } catch (UareUException e) {
                            //send extraction error
                            SendToListener(ACT_FEATURES, null, null, null, e);
                        }
                    } else {
                        //send quality result
                        SendToListener(ACT_CAPTURE, null, evt.capture_result, evt.reader_status, evt.exception);
                    }
                } else {
                    //send capture error
                    SendToListener(ACT_CAPTURE, null, evt.capture_result, evt.reader_status, evt.exception);
                }
            }

            return prefmd;
        }

        public void cancel() {
            m_bCancel = true;
            if (null != m_capture) m_capture.cancel();
        }

        private void SendToListener(String action, Fmd fmd, Reader.CaptureResult cr, Reader.Status st, UareUException ex) {
            if (null == m_listener || null == action || action.equals("")) return;

            final EnrollmentEvent evt = new EnrollmentEvent(this, action, fmd, cr, st, ex);

            //invoke listener on EDT thread
            try {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        m_listener.actionPerformed(evt);
                    }
                });
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            //acquire engine
            Engine engine = UareUGlobal.GetEngine();

            try {
                m_bCancel = false;
                while (!m_bCancel) {
                    //run enrollment
                    Fmd fmd = engine.CreateEnrollmentFmd(Fmd.Format.ANSI_378_2004, this);

                    //send result
                    if (null != fmd) {
                        SendToListener(ACT_DONE, fmd, null, null, null);
                    } else {
                        SendToListener(ACT_CANCELED, null, null, null, null);
                        break;
                    }
                }
            } catch (UareUException e) {
                SendToListener(ACT_DONE, null, null, null, e);
            }
        }
    }


    private static final long serialVersionUID = 6;
    private static final String ACT_BACK = "back";

    private EnrollmentThread m_enrollment;
    private Reader m_reader;
    private JDialog m_dlgParent;
    private JTextArea m_text;
    private boolean m_bJustStarted;

    private static List<String> registeredFingerprints;

    private Enrollment(Reader reader) {
        m_reader = reader;
        m_bJustStarted = true;
        m_enrollment = new EnrollmentThread(m_reader, this);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 600));

        // Header Panel (Green part with logo)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(DARK_GREEN);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add logo
        ImageIcon logo = new ImageIcon(getClass().getResource("/DTECLogo.png"));
        JLabel logoLabel = new JLabel(logo);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(logoLabel);

        // Add Fingerprint Module text
        JLabel moduleLabel = new JLabel("Fingerprint Enrollment");
        moduleLabel.setForeground(Color.WHITE);
        moduleLabel.setFont(TITLE_FONT);
        moduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(moduleLabel);
        headerPanel.add(Box.createVerticalStrut(10));

        // Content Panel (White part)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Text Area with custom styling
        m_text = new JTextArea(15, 1);
        m_text.setEditable(false);
        m_text.setFont(TEXT_FONT);
        m_text.setMargin(new Insets(10, 10, 10, 10));
        m_text.setBackground(new Color(245, 245, 245));
        JScrollPane paneReader = new JScrollPane(m_text);
        paneReader.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Make text area expand to fill available space
        paneReader.setPreferredSize(new Dimension(460, 300));
        paneReader.setMaximumSize(new Dimension(460, 300));
        paneReader.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(paneReader);
        contentPanel.add(Box.createVerticalStrut(20));

        // Back button with matching style
        JButton btnBack = new JButton("Back");
        btnBack.setActionCommand(ACT_BACK);
        btnBack.addActionListener(this);
        btnBack.setBackground(DARK_GREEN);
        btnBack.setForeground(Color.WHITE);
        btnBack.setFont(new Font("Arial", Font.BOLD, 16));
        btnBack.setFocusPainted(false);
        btnBack.setPreferredSize(new Dimension(360, 50));
        btnBack.setMaximumSize(new Dimension(360, 50));
        btnBack.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(btnBack);
        contentPanel.add(Box.createVerticalStrut(20));

        // Add panels to main panel
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        setOpaque(true);
    }

    public void actionPerformed(ActionEvent e) {
        Engine engine = UareUGlobal.GetEngine();

        if (e.getActionCommand().equals(ACT_BACK)) {
            //destroy dialog to cancel enrollment
            m_dlgParent.setVisible(false);
        } else {
            EnrollmentThread.EnrollmentEvent evt = (EnrollmentThread.EnrollmentEvent) e;

            if (e.getActionCommand().equals(EnrollmentThread.ACT_PROMPT)) {
                if (m_bJustStarted) {
                    m_text.append("Enrollment Started\n");
                    m_text.append("    Put any finger on the reader\n");
                } else {
                    m_text.append("    Put the same finger on the reader\n");
                }
                m_bJustStarted = false;
            } else if (e.getActionCommand().equals(EnrollmentThread.ACT_CAPTURE)) {
                if (null != evt.capture_result) {
                    MessageBox.BadQuality(evt.capture_result.quality);
                } else if (null != evt.exception) {
                    MessageBox.DpError("Capture", evt.exception);
                } else if (null != evt.reader_status) {
                    MessageBox.BadStatus(evt.reader_status);
                }
                m_bJustStarted = false;
            } else if (e.getActionCommand().equals(EnrollmentThread.ACT_FEATURES)) {
                if (null == evt.exception) {
                    m_text.append("    Fingerprint Captured, Features extracted\n ______________________________________ \n\n");
                } else {
                    MessageBox.DpError("Feature extraction", evt.exception);
                }
                m_bJustStarted = false;
            } else if (e.getActionCommand().equals(EnrollmentThread.ACT_DONE)) {
                if (null == evt.exception) {
                    String str = String.format("    Enrollment template created, size: %d\n______________________________________ \n\n", evt.enrollment_fmd.getData().length);
                    m_text.append(str);
                    try {
                        fetchFingerprint();


                        boolean isFingerprintExisting = false;

                        // perform checking if the fingerprint that just registered matches to the stored fingerprints
                        for(var registeredFingerprint : registeredFingerprints){
                            byte[] fingerprintBytes = Base64.getDecoder().decode(registeredFingerprint);
                            // Create FMD directly from stored data
                            Fmd storedFmd = UareUGlobal.GetImporter().ImportFmd(fingerprintBytes, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);

                            // Compare fingerprints
                            int falsematch_rate = engine.Compare(evt.enrollment_fmd, 0, storedFmd, 0);
                            int target_falsematch_rate = Engine.PROBABILITY_ONE / 100000;

                            if (falsematch_rate < target_falsematch_rate) {
                                isFingerprintExisting = true;
                                break;
                            }

                        }

                        if(!isFingerprintExisting){
                            String username = JOptionPane.showInputDialog(null, "Enter ID Number");
                            if (username == null || username.isEmpty())
                                throw new Exception("");
                            byte[] data = evt.enrollment_fmd.getData();
                            Map<String, String> d = new HashMap<>();
                            d.put("data", Base64.getEncoder().encodeToString(data));
                            d.put("date", String.valueOf(LocalDate.now()));
                            UserUtil.username = username;
                            d.put("username", username);
                            myStompClient.sendEnrollmentMessage(d);
                        }else{
                            JOptionPane.showMessageDialog(null, "This Fingerprint is already registered. Please try again with a different fingerprint.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Something went wrong");
                    }
                } else {
                    MessageBox.DpError("Enrollment template creation", evt.exception);
                }
                m_bJustStarted = true;
            } else if (e.getActionCommand().equals(EnrollmentThread.ACT_CANCELED)) {
                //canceled, destroy dialog
                m_dlgParent.setVisible(false);
            }

            //cancel enrollment if any exception or bad reader status
            if (null != evt.exception) {
                m_dlgParent.setVisible(false);
            } else if (null != evt.reader_status && Reader.ReaderStatus.READY != evt.reader_status.status && Reader.ReaderStatus.NEED_CALIBRATION != evt.reader_status.status) {
                m_dlgParent.setVisible(false);
            }
        }
    }

    private void doModal(JDialog dlgParent) {
        //open reader
        try {
            m_reader.Open(Reader.Priority.COOPERATIVE);
        } catch (UareUException e) {
            MessageBox.DpError("Reader.Open()", e);
        }

        //start enrollment thread
        m_enrollment.start();

        //bring up modal dialog
        m_dlgParent = dlgParent;
        m_dlgParent.setContentPane(this);
        m_dlgParent.pack();
        m_dlgParent.setLocationRelativeTo(null);
        m_dlgParent.setVisible(true);
        m_dlgParent.dispose();

        //stop enrollment thread
        m_enrollment.cancel();

        //close reader
        try {
            m_reader.Close();
        } catch (UareUException e) {
            MessageBox.DpError("Reader.Close()", e);
        }
    }

    private static void fetchFingerprint() {
        List<String> fingerprints = FingerprintUtil.getAllStoredFingerprints();
        if (fingerprints != null){
            registeredFingerprints.clear();
            registeredFingerprints.addAll(fingerprints);
        }
    }

    public static void Run(Reader reader) throws URISyntaxException, InterruptedException, ExecutionException {
        registeredFingerprints = new ArrayList<>();
        myStompClient = new MyStompClient(new MessageListener() {
            @Override
            public void onMessageReceive(Map<String, Object> message) throws Exception {
            }
        }, "");
        JDialog dlg = new JDialog((JDialog) null, "Enrollment", true);
        Enrollment enrollment = new Enrollment(reader);
        enrollment.doModal(dlg);
    }
}
