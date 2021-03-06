package net.ddns.logick;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
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
    private JLabel homeDirLabel;
    private JTextField homeDirTextField;
    private JLabel requestCooldownLabel;
    private JTextField requestCooldownTextField;
    private JButton cancelButton;
    private JButton acceptButton;
    private JPanel mainPanel;
    private JComboBox<String> languageComboBox;
    private JFrame frame;

    public MyPreferences() {
        $$$setupUI$$$();
        maxRetriesTextField.setText(String.valueOf(Main.MAX_RETRIES));
        timeoutTextField.setText(String.valueOf(Main.TIMEOUT));
        threadCountTextField.setText(String.valueOf(Main.THREADS_COUNT));
        for (int i = 0; i < Main.SUPPORTED_LANGUAGES.length; i++) {
            languageComboBox.addItem(Main.SUPPORTED_LANGUAGES[i]);
            if (Main.SUPPORTED_LANGUAGES[i].equals(Main.CURRENT_LANGUAGE)) languageComboBox.setSelectedIndex(i);
        }
        requestCooldownTextField.setText(String.valueOf(Main.REQUEST_COOLDOWN));
        homeDirTextField.setText(Main.HOME_DIR);
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
        Main.REQUEST_COOLDOWN = pref.getInt("REQUEST_COOLDOWN", 1000);
    }

    public static void storePrefs() {
        pref.put("CURRENT_LANGUAGE", Main.CURRENT_LANGUAGE);
        pref.putInt("MAX_RETRIES", Main.MAX_RETRIES);
        pref.putInt("THREADS_COUNT", Main.THREADS_COUNT);
        pref.putInt("TIMEOUT", Main.TIMEOUT);
        pref.put("HOME_DIR", Main.HOME_DIR);
        pref.putInt("REQUEST_COOLDOWN", Main.REQUEST_COOLDOWN);
        try {
            pref.flush();
        } catch (BackingStoreException e) {
            Main.LOG.error(Language.get("message.error.preferences_saving"), e);
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
        cancelButton.addActionListener(actionEvent -> frame.dispose());
        acceptButton.addActionListener(actionEvent -> {
            Main.MAX_RETRIES = Integer.parseInt(maxRetriesTextField.getText());
            Main.TIMEOUT = Integer.parseInt(timeoutTextField.getText());
            Main.THREADS_COUNT = Integer.parseInt(threadCountTextField.getText());
            Main.CURRENT_LANGUAGE = Main.SUPPORTED_LANGUAGES[languageComboBox.getSelectedIndex()];
            Main.HOME_DIR = homeDirTextField.getText();
            Main.REQUEST_COOLDOWN = Integer.parseInt(requestCooldownTextField.getText());
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
        requestCooldownLabel.setText(Language.get("label.request_cooldown"));
        homeDirLabel.setText(Language.get("label.home_dir"));
        cancelButton.setText(Language.get("button.settings_cancel"));
        acceptButton.setText(Language.get("button.accept"));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(8, 1, new Insets(5, 5, 5, 5), -1, -1));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maxRetriesLabel = new JLabel();
        maxRetriesLabel.setText("Max retries");
        panel1.add(maxRetriesLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxRetriesTextField = new JTextField();
        panel1.add(maxRetriesTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        timeoutLabel = new JLabel();
        timeoutLabel.setText("Timeout (in milisecs)");
        panel2.add(timeoutLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        timeoutTextField = new JTextField();
        panel2.add(timeoutTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        threadCountLabel = new JLabel();
        threadCountLabel.setText("Thread count");
        panel3.add(threadCountLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadCountTextField = new JTextField();
        panel3.add(threadCountTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel4, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel4.add(cancelButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        acceptButton = new JButton();
        acceptButton.setText("Accept");
        panel4.add(acceptButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Language");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel5.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        languageComboBox = new JComboBox();
        panel5.add(languageComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        homeDirLabel = new JLabel();
        homeDirLabel.setText("Home dir");
        panel6.add(homeDirLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        homeDirTextField = new JTextField();
        panel6.add(homeDirTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        requestCooldownLabel = new JLabel();
        requestCooldownLabel.setRequestFocusEnabled(false);
        requestCooldownLabel.setText("Request cooldown");
        panel7.add(requestCooldownLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        requestCooldownTextField = new JTextField();
        panel7.add(requestCooldownTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
