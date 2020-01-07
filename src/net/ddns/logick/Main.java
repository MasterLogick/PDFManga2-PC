package net.ddns.logick;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class Main {
    //todo make better chapter selection form
    static final String[] SUPPORTED_LANGUAGES = new String[]{"en", "ru"};
    private static final Object mainLock = new Object();
    private static final Object secondaryLock = new Object();
    public static TextAreaLog LOG;
    static Thread currentDownloadingThread = null;
    static String CURRENT_LANGUAGE = "en";
    static int THREADS_COUNT = 10;
    static int TIMEOUT = 1000 * 30;
    static int MAX_RETRIES = 10;
    static int REQUEST_COOLDOWN = 1000;
    static String HOME_DIR = System.getProperty("user.home");
    static private JProgressBar currentMainProgressBar;
    static private JProgressBar currentSecondaryProgressBar;
    private JFrame frame;
    private JFileChooser fileChooser;
    private JPanel mainPanel;
    private JPanel urlPanel;
    private JLabel urlLabel;
    private JTextField urlTextField;
    private JButton checkButton;
    private JPanel checkPanel;
    private JLabel coverLabel;
    private JLabel descriptionLabel;
    private JPanel fullDownloadCheckboxPanel;
    private JCheckBox downloadFullMangaCheckBox;
    private JPanel chaptersPanel;
    private JLabel downloadBeforeLabel;
    private JTextField startChapterNumberTextField;
    private JLabel downloadBetweenLabel;
    private JTextField endChapterNumberTextField;
    private JLabel downloadAfterLabel;
    private JPanel outputPanel;
    private JLabel outputNameLabel;
    private JPanel prefixPanel;
    private JLabel filesPrefixLabel;
    private JTextField prefixTextField;
    private JTextField outputPathTextField;
    private JButton browseButton;
    private JPanel divisionCheckBoxPanel;
    private JCheckBox divideMangaCheckBox;
    private JPanel divisionPanel;
    private JLabel divideBeforeLabel;
    private JTextField divisionCountTextField;
    private JLabel divideAfterLabel;
    private JPanel controlPanel;
    private JButton cancelButton;
    private JButton startButton;
    private JPanel processPanel;
    private JProgressBar mainProgressBar;
    private JProgressBar secondaryProgressBar;
    private JTextArea logTextArea;
    private JButton clearLogButton;
    private JScrollPane scrollPane;
    private JPanel toZipPanel;
    private JCheckBox toZipCheckbox;
    private JMenuItem aboutMenuItem;
    private JMenuItem settingsMenuItem;
    private Container[] onCheckEnabledContainers;
    private Container[] onStartDisabledContainers;
    private Container[] onZipEnabledContainers;
    private boolean isFullDownload;
    private boolean isToZipFile;
    private boolean isDivideEnabled;
    private FileFilter pdfFilter;
    private FileFilter dirFilter;
    private File selectedFile;
    private int maxChaptersCount;
    private int scrollValue = 0;
    private boolean autoScrollToBottom = true;
    private MangaData currentMangaData = null;

    public Main() {
        maxChaptersCount = 0;
        onStartDisabledContainers = new Container[]{toZipPanel, fullDownloadCheckboxPanel, chaptersPanel, outputPanel, prefixPanel, divisionCheckBoxPanel, divisionPanel, controlPanel, processPanel};
        onCheckEnabledContainers = new Container[]{toZipPanel, fullDownloadCheckboxPanel, outputPanel, divisionCheckBoxPanel, controlPanel};
        onZipEnabledContainers = new Container[]{toZipPanel, fullDownloadCheckboxPanel, outputPanel, prefixPanel, controlPanel};
        pdfFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return Language.get("file_filter.pdf");
            }
        };
        dirFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return Language.get("file_filter.dir");
            }
        };
        isFullDownload = true;
        isDivideEnabled = false;
        isToZipFile = false;
        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(Language.get("frame_name.browse"));
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSystemView(FileSystemView.getFileSystemView());
        frame = new JFrame(Language.get("frame_name.main"));
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setContentPane(mainPanel);
        try {
            frame.setIconImage(ImageIO.read(Main.class.getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        disableChildren(onStartDisabledContainers);
        createUIComponents();
        LOG = new TextAreaLog("MAIN", logTextArea);
        frame.pack();
        initListeners();
    }

    public static void main(String[] args) {
        MyPreferences.loadPrefs();
        MyPreferences.storePrefs();
        try {
            Language.loadLocale();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ParseManager.addParser(new ReMangaParser(), "remanga.org");
        ParseManager.addParser(new ReadMangaParser(), "readmanga.me", "mintmanga.live", "selfmanga.ru");
        new Main().show();
    }

    public static void increaseAndUpdateSecondaryProgressBarState(String text) {
        synchronized (secondaryLock) {
            currentSecondaryProgressBar.setValue(currentSecondaryProgressBar.getValue() + 1);
            currentSecondaryProgressBar.setString(text);
        }
    }

    public static void increaseAndUpdateMainProgressBarState(String text) {
        synchronized (mainLock) {
            currentMainProgressBar.setValue(currentMainProgressBar.getValue() + 1);
            currentMainProgressBar.setString(text);
        }
    }

    public static void cancelOnProgressBar() {
        currentMainProgressBar.setValue(0);
        currentMainProgressBar.setString(Language.get("progress.canceled"));
        currentSecondaryProgressBar.setValue(0);
        currentSecondaryProgressBar.setString(Language.get("progress.canceled"));
    }

    public static void completedOnProgressBar() {
        currentMainProgressBar.setValue(0);
        currentMainProgressBar.setString(Language.get("progress.completed"));
        currentSecondaryProgressBar.setValue(0);
        currentSecondaryProgressBar.setString(Language.get("progress.completed"));
    }

    public static void initialiseSecondaryProgressBar(int max, String text) {
        currentMainProgressBar.setMaximum(max);
        currentMainProgressBar.setValue(0);
        currentMainProgressBar.setString(text);
    }

    public void show() {
        frame.setVisible(true);
    }

    private void enableChildren(Container... arr) {
        for (Container c :
                arr) {
            for (Component comp :
                    c.getComponents()) {
                comp.setEnabled(true);
            }
        }
    }

    private void disableChildren(Container... arr) {
        for (Container c :
                arr) {
            for (Component comp :
                    c.getComponents()) {
                comp.setEnabled(false);
            }
        }
    }

    private void createUIComponents() {
        urlLabel.setText(Language.get("label.url"));
        checkButton.setText(Language.get("button.check"));
        downloadFullMangaCheckBox.setText(Language.get("checkbox.full_download"));
        downloadBeforeLabel.setText(Language.get("label.download_bounds.before"));
        downloadBetweenLabel.setText(Language.get("label.download_bounds.between"));
        downloadAfterLabel.setText(Language.get("label.download_bounds.after"));
        outputNameLabel.setText(Language.get("label.output_path.file"));
        browseButton.setText(Language.get("button.browse"));
        filesPrefixLabel.setText(Language.get("label.files_prefix"));
        divideMangaCheckBox.setText(Language.get("checkbox.division"));
        divideBeforeLabel.setText(Language.get("label.division.before"));
        divideAfterLabel.setText(Language.get("label.division.after"));
        cancelButton.setText(Language.get("button.main_cancel"));
        startButton.setText(Language.get("button.start"));
        clearLogButton.setText(Language.get("button.clear_log"));
        toZipCheckbox.setText(Language.get("button.to_zip_file"));
        currentMainProgressBar = mainProgressBar;
        currentSecondaryProgressBar = secondaryProgressBar;
        aboutMenuItem = new JMenuItem(Language.get("menu.about"));
        settingsMenuItem = new JMenuItem(Language.get("menu.settings"));
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu(Language.get("menu.file"))).add(settingsMenuItem);
        menuBar.add(new JMenu(Language.get("menu.help"))).add(aboutMenuItem);
        frame.setJMenuBar(menuBar);
    }

    private void checkButtonPressed() {
        if (!urlTextField.getText().isEmpty()) {
            currentMangaData = ParseManager.parse(urlTextField.getText());
            coverLabel.setIcon(currentMangaData.cover);
            descriptionLabel.setText(currentMangaData.htmlData);
            checkPanel.setVisible(true);
            maxChaptersCount = currentMangaData.chaptersCount;
            startChapterNumberTextField.setText("1");
            endChapterNumberTextField.setText(String.valueOf(maxChaptersCount));
            enableChildren(isToZipFile ? onZipEnabledContainers : onCheckEnabledContainers);
            String filePath = HOME_DIR + File.separator + ((isDivideEnabled || isToZipFile) ? "" : currentMangaData.defaultFileName);
            fileChooser.setSelectedFile(new File(filePath));
            selectedFile = fileChooser.getSelectedFile();
            outputPathTextField.setText(filePath);
            frame.pack();
        }
    }

    private void cancelButtonPressed() {
        ParseManager.cancel();
        startButton.setEnabled(true);
    }

    private void browseButtonPressed() {
        if (fileChooser.showDialog(frame, Language.get("button.save")) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            outputPathTextField.setText(selectedFile.getAbsolutePath());
            if (selectedFile.isDirectory()) HOME_DIR = selectedFile.getAbsolutePath();
            else HOME_DIR = selectedFile.getParent();
            MyPreferences.storePrefs();
            enableChildren(controlPanel);
        }
    }

    private void enableDivision() {
        outputNameLabel.setText(Language.get("label.output_path.dir"));
        if (!outputPathTextField.getText().isEmpty()) {
            outputPathTextField.setText(HOME_DIR + File.separator);
            prefixTextField.setText(currentMangaData.defaultFilePrefix);
            selectedFile = new File(HOME_DIR + File.separator);
            fileChooser.setSelectedFile(selectedFile);
        }
        enableChildren(divisionPanel, prefixPanel);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setFileFilter(dirFilter);
    }

    private void disableDivision() {
        outputNameLabel.setText(Language.get("label.output_path.file"));
        if (!outputPathTextField.getText().isEmpty()) {
            outputPathTextField.setText(HOME_DIR + File.separator + currentMangaData.defaultFileName);
            selectedFile = new File(HOME_DIR + File.separator + currentMangaData.defaultFileName);
            fileChooser.setSelectedFile(selectedFile);
        }
        disableChildren(divisionPanel, prefixPanel);
        prefixTextField.setText("");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(pdfFilter);
    }

    private void startButtonPressed() {
        startButton.setEnabled(false);
        if (isToZipFile)
            ParseManager.download(
                    currentMangaData.mangaURI,
                    isFullDownload ? 1 : Integer.parseInt(startChapterNumberTextField.getText()),
                    isFullDownload ? maxChaptersCount : Integer.parseInt(endChapterNumberTextField.getText()),
                    new ToZipPostProcessing(selectedFile, prefixTextField.getText().isEmpty() ? "" : prefixTextField.getText()));
        else ParseManager.download(
                currentMangaData.mangaURI,
                isFullDownload ? 1 : Integer.parseInt(startChapterNumberTextField.getText()),
                isFullDownload ? maxChaptersCount : Integer.parseInt(endChapterNumberTextField.getText()),
                new ToPDFPostProcessing(isDivideEnabled ? Integer.parseInt(divisionCountTextField.getText()) : 1,
                        selectedFile, prefixTextField.getText().isEmpty() ? "" : prefixTextField.getText()));
        startButton.setEnabled(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    private void initListeners() {
        aboutMenuItem.addActionListener(actionEvent -> JOptionPane.showMessageDialog(frame, Language.get("message.about")));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (currentDownloadingThread != null) ParseManager.cancel();
                System.exit(0);
            }
        });

        settingsMenuItem.addActionListener(actionEvent -> new MyPreferences().show());

        checkButton.addActionListener(actionEvent -> checkButtonPressed());
        toZipCheckbox.addActionListener(actionEvent -> {
            isToZipFile = !isToZipFile;
            if (isToZipFile) {
                enableDivision();
                disableChildren(onStartDisabledContainers);
                enableChildren(onZipEnabledContainers);
            } else {
                disableDivision();
                disableChildren(onStartDisabledContainers);
                enableChildren(onCheckEnabledContainers);
                if (isDivideEnabled) {
                    enableChildren(divisionPanel);
                }
            }
        });
        downloadFullMangaCheckBox.addActionListener(actionEvent -> {
            isFullDownload = !isFullDownload;
            if (isFullDownload) {
                disableChildren(chaptersPanel);
            } else {
                enableChildren(chaptersPanel);
            }
        });

        divideMangaCheckBox.addActionListener(actionEvent -> {
            isDivideEnabled = !isDivideEnabled;
            if (isDivideEnabled) enableDivision();
            else disableDivision();
        });

        browseButton.addActionListener(actionEvent -> browseButtonPressed());

        cancelButton.addActionListener(actionEvent -> cancelButtonPressed());

        startButton.addActionListener(actionEvent -> {
            currentDownloadingThread = new Thread(this::startButtonPressed);
            currentDownloadingThread.start();
        });

        clearLogButton.addActionListener(actionEvent -> logTextArea.setText(""));

        outputPathTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!outputPathTextField.getText().isEmpty()) {
                    String path = outputPathTextField.getText().trim();
                    File f = new File(path);
                    if (!path.equals(selectedFile.getAbsolutePath()) && f.exists() &&
                            fileChooser.accept(f)) {
                        selectedFile = f;
                        enableChildren(controlPanel);
                    }
                }
            }
        });

        startChapterNumberTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    int val = Integer.parseInt(startChapterNumberTextField.getText());
                    if (val < 1)
                        startChapterNumberTextField.setText("1");
                    if (val > Integer.parseInt(endChapterNumberTextField.getText())) {
                        startChapterNumberTextField.setText(endChapterNumberTextField.getText());
                    }
                } catch (Exception ex) {
                    LOG.error(String.format(Language.get("message.error.NaN"), startChapterNumberTextField.getText()), ex);
                    startChapterNumberTextField.setText("0");
                }
            }
        });

        divisionCountTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    Integer.parseInt(divisionCountTextField.getText());
                } catch (Exception ex) {
                    LOG.error(String.format(Language.get("message.error.NaN"), divisionCountTextField.getText()), ex);
                    divisionCountTextField.setText("1");
                }
            }
        });

        endChapterNumberTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    int val = Integer.parseInt(endChapterNumberTextField.getText());
                    if (val < Integer.parseInt(startChapterNumberTextField.getText()))
                        endChapterNumberTextField.setText(startChapterNumberTextField.getText());
                    if (val > maxChaptersCount) {
                        endChapterNumberTextField.setText(String.valueOf(maxChaptersCount));
                    }
                } catch (Exception ex) {
                    LOG.error(String.format(Language.get("message.error.NaN"), endChapterNumberTextField.getText()), ex);
                    endChapterNumberTextField.setText(String.valueOf(currentMangaData.chaptersCount));
                }
            }
        });

        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.addAdjustmentListener(e -> {
            Adjustable aj = e.getAdjustable();
            if (autoScrollToBottom) {
                if (scrollValue - aj.getValue() >= 45) autoScrollToBottom = false;
                else scrollBar.setValue(scrollBar.getMaximum());
            } else if (aj.getValue() + aj.getVisibleAmount() == aj.getMaximum()) autoScrollToBottom = true;
            scrollValue = aj.getValue();
        });
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
        mainPanel.setLayout(new GridLayoutManager(12, 1, new Insets(0, 0, 0, 0), -1, -1));
        checkPanel = new JPanel();
        checkPanel.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        checkPanel.setVisible(false);
        mainPanel.add(checkPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        coverLabel = new JLabel();
        coverLabel.setEnabled(true);
        coverLabel.setText("");
        coverLabel.setVisible(true);
        checkPanel.add(coverLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(156, 218), new Dimension(156, 218), new Dimension(156, 218), 0, false));
        descriptionLabel = new JLabel();
        descriptionLabel.setText("Label");
        descriptionLabel.setVerticalAlignment(0);
        checkPanel.add(descriptionLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlPanel = new JPanel();
        urlPanel.setLayout(new GridLayoutManager(1, 3, new Insets(5, 0, 0, 0), -1, -1));
        mainPanel.add(urlPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        urlLabel = new JLabel();
        urlLabel.setText("URL1");
        urlLabel.setVisible(true);
        urlPanel.add(urlLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTextField = new JTextField();
        urlTextField.setToolTipText("Enter manga main page address");
        urlPanel.add(urlTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        checkButton = new JButton();
        checkButton.setText("Check");
        urlPanel.add(checkButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chaptersPanel = new JPanel();
        chaptersPanel.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        chaptersPanel.setEnabled(false);
        mainPanel.add(chaptersPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        downloadBeforeLabel = new JLabel();
        downloadBeforeLabel.setText("Download from");
        chaptersPanel.add(downloadBeforeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        chaptersPanel.add(spacer1, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        startChapterNumberTextField = new JTextField();
        startChapterNumberTextField.setColumns(4);
        startChapterNumberTextField.setText("");
        chaptersPanel.add(startChapterNumberTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        downloadBetweenLabel = new JLabel();
        downloadBetweenLabel.setText("to");
        chaptersPanel.add(downloadBetweenLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        endChapterNumberTextField = new JTextField();
        endChapterNumberTextField.setColumns(4);
        chaptersPanel.add(endChapterNumberTextField, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        downloadAfterLabel = new JLabel();
        downloadAfterLabel.setText("");
        chaptersPanel.add(downloadAfterLabel, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputPanel = new JPanel();
        outputPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        outputPanel.setEnabled(false);
        mainPanel.add(outputPanel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        outputNameLabel = new JLabel();
        outputNameLabel.setText("Output file");
        outputPanel.add(outputNameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputPathTextField = new JTextField();
        outputPanel.add(outputPathTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        browseButton = new JButton();
        browseButton.setText("Browse");
        outputPanel.add(browseButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        divisionPanel = new JPanel();
        divisionPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        divisionPanel.setEnabled(false);
        mainPanel.add(divisionPanel, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        divideBeforeLabel = new JLabel();
        divideBeforeLabel.setText("Divide on");
        divisionPanel.add(divideBeforeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        divisionPanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        divisionCountTextField = new JTextField();
        divisionCountTextField.setColumns(4);
        divisionPanel.add(divisionCountTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        divideAfterLabel = new JLabel();
        divideAfterLabel.setText("files");
        divisionPanel.add(divideAfterLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        controlPanel.setEnabled(false);
        mainPanel.add(controlPanel, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        controlPanel.add(cancelButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Start");
        controlPanel.add(startButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        controlPanel.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        processPanel = new JPanel();
        processPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(processPanel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mainProgressBar = new JProgressBar();
        mainProgressBar.setMaximum(6);
        mainProgressBar.setString("");
        mainProgressBar.setStringPainted(true);
        processPanel.add(mainProgressBar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        secondaryProgressBar = new JProgressBar();
        secondaryProgressBar.setString("");
        secondaryProgressBar.setStringPainted(true);
        processPanel.add(secondaryProgressBar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        scrollPane.setAutoscrolls(true);
        processPanel.add(scrollPane, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setRows(20);
        scrollPane.setViewportView(logTextArea);
        clearLogButton = new JButton();
        clearLogButton.setText("Clear log");
        mainPanel.add(clearLogButton, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fullDownloadCheckboxPanel = new JPanel();
        fullDownloadCheckboxPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(fullDownloadCheckboxPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        downloadFullMangaCheckBox = new JCheckBox();
        downloadFullMangaCheckBox.setEnabled(true);
        downloadFullMangaCheckBox.setSelected(true);
        downloadFullMangaCheckBox.setText("Download full manga");
        fullDownloadCheckboxPanel.add(downloadFullMangaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        fullDownloadCheckboxPanel.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        divisionCheckBoxPanel = new JPanel();
        divisionCheckBoxPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(divisionCheckBoxPanel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        divideMangaCheckBox = new JCheckBox();
        divideMangaCheckBox.setEnabled(true);
        divideMangaCheckBox.setText("Divide manga to different files");
        divisionCheckBoxPanel.add(divideMangaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        divisionCheckBoxPanel.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        prefixPanel = new JPanel();
        prefixPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(prefixPanel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        filesPrefixLabel = new JLabel();
        filesPrefixLabel.setText("Files prefix");
        prefixPanel.add(filesPrefixLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        prefixTextField = new JTextField();
        prefixPanel.add(prefixTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        toZipPanel = new JPanel();
        toZipPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(toZipPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        toZipCheckbox = new JCheckBox();
        toZipCheckbox.setEnabled(true);
        toZipCheckbox.setText("To Zip Files");
        toZipPanel.add(toZipCheckbox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        toZipPanel.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        urlLabel.setLabelFor(urlTextField);
        downloadBeforeLabel.setLabelFor(startChapterNumberTextField);
        downloadBetweenLabel.setLabelFor(endChapterNumberTextField);
        outputNameLabel.setLabelFor(outputPathTextField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
