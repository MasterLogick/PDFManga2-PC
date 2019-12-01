package net.ddns.logick;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MyPreferences {
    private static final Preferences pref = Preferences.userNodeForPackage(Main.class);
    private JLabel maxRetriesLabel;
    private JTextField maxRetriesTextField;
    private JLabel timeoutLabel;
    private JTextField timeoutTextField;
    private JLabel threadCountLabel;
    private JTextField threadCountTextField;
    private JButton cancelButton;
    private JButton acceptButton;
    private JPanel mainPanel;
    private JComboBox languageComboBox;
    private JFrame frame;

    public MyPreferences() {
        maxRetriesTextField.setText(String.valueOf(Main.MAX_RETRIES));
        timeoutTextField.setText(String.valueOf(Main.TIMEOUT));
        threadCountTextField.setText(String.valueOf(Main.THREADS_COUNT));
        for (int i = 0; i < Main.SUPPORTED_LANGUAGES.length; i++) {
            languageComboBox.addItem(Main.SUPPORTED_LANGUAGES[i]);
            if (Main.SUPPORTED_LANGUAGES[i].equals(Main.CURRENT_LANGUAGE)) languageComboBox.setSelectedIndex(i);
        }
        frame = new JFrame(Language.get("frame_name.settings"));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setContentPane(mainPanel);
        createUIComponents();
        frame.pack();
        initListeners();
    }

    public static void loadPrefs() {
        Main.CURRENT_LANGUAGE = pref.get("CURRENT_LANGUAGE", "en");
        Main.MAX_RETRIES = pref.getInt("MAX_RETRIES", 10);
        Main.THREADS_COUNT = pref.getInt("THREADS_COUNT", 10);
        Main.TIMEOUT = pref.getInt("TIMEOUT", 30 * 1000);
        Main.HOME_DIR = pref.get("HOME_DIR", System.getProperty("user.home"));
    }

    public static void storePrefs() {
        pref.put("CURRENT_LANGUAGE", Main.CURRENT_LANGUAGE);
        pref.putInt("MAX_RETRIES", Main.MAX_RETRIES);
        pref.putInt("THREADS_COUNT", Main.THREADS_COUNT);
        pref.putInt("TIMEOUT", Main.TIMEOUT);
        pref.put("HOME_DIR", Main.HOME_DIR);
        try {
            pref.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, Language.get("message.preferences_saving_error"));
        }
    }

    private void initListeners() {
        maxRetriesTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                try {
                    int val = Integer.parseInt(maxRetriesTextField.getText());
                    if (val < 1)
                        maxRetriesTextField.setText("1");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    maxRetriesTextField.setText(String.valueOf(Main.MAX_RETRIES));
                }
            }
        });
        timeoutTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                try {
                    int val = Integer.parseInt(timeoutTextField.getText());
                    if (val < 1)
                        timeoutTextField.setText("1");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    timeoutTextField.setText(String.valueOf(Main.TIMEOUT));
                }
            }
        });
        threadCountTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
                try {
                    int val = Integer.parseInt(threadCountTextField.getText());
                    if (val < 1)
                        threadCountTextField.setText("1");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    threadCountTextField.setText(String.valueOf(Main.THREADS_COUNT));
                }
            }
        });
        cancelButton.addActionListener(actionEvent -> {
            frame.dispose();
        });
        acceptButton.addActionListener(actionEvent -> {
            Main.MAX_RETRIES = Integer.parseInt(maxRetriesTextField.getText());
            Main.TIMEOUT = Integer.parseInt(timeoutTextField.getText());
            Main.THREADS_COUNT = Integer.parseInt(threadCountTextField.getText());
            Main.CURRENT_LANGUAGE = Main.SUPPORTED_LANGUAGES[languageComboBox.getSelectedIndex()];
            storePrefs();
            frame.dispose();
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                frame.dispose();
            }
        });
    }

    public void show() {
        frame.setVisible(true);
    }

    private void createUIComponents() {
        maxRetriesLabel.setText(Language.get("label.max_retries"));
        timeoutLabel.setText(Language.get("label.timeout"));
        threadCountLabel.setText(Language.get("label.thread_count"));
        cancelButton.setText(Language.get("button.settings_cancel"));
        acceptButton.setText(Language.get("button.accept"));
    }
}
