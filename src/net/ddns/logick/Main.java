package net.ddns.logick;

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
import java.net.URI;

public class Main {
    static final String[] SUPPORTED_LANGUAGES = new String[]{"en", "ru"};
    static final String USAGE_STRING = "Usage: java -jar PDFManga2.jar [THREADS_COUNT]\n[THREADS_COUNT] - optional parameter that sets amount of image downloading threads, 10 - default";
    static String CURRENT_LANGUAGE = "en";
    static int THREADS_COUNT = 10;
    static int TIMEOUT = 1000 * 30;
    static int MAX_RETRIES = 10;
    static String HOME_DIR = System.getProperty("user.home");
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
        aboutMenuItem = new JMenuItem(Language.get("menu.about"));
        settingsMenuItem = new JMenuItem(Language.get("menu.settings"));
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu(Language.get("menu.file"))).add(settingsMenuItem);
        menuBar.add(new JMenu(Language.get("menu.help"))).add(aboutMenuItem);
        frame.setJMenuBar(menuBar);
        disableChildren(onStartDisabledContainers);
        createUIComponents();
        frame.pack();
        initListeners();
    }

    public static void main(String[] args) {
        MyPreferences.loadPrefs();
        try {
            if (args.length != 0) THREADS_COUNT = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println(USAGE_STRING);
            JOptionPane.showMessageDialog(null, USAGE_STRING);
            System.exit(0);
        }
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

    static void cancelOnProgressBar(JProgressBar main, JProgressBar secondary) {
        main.setValue(0);
        main.setString(Language.get("progress.canceled"));
        secondary.setValue(0);
        secondary.setString(Language.get("progress.canceled"));
    }

    private void initListeners() {
        aboutMenuItem.addActionListener(actionEvent -> JOptionPane.showMessageDialog(frame, Language.get("message.about")));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ParseManager.cancel();
                System.exit(0);
            }
        });

        settingsMenuItem.addActionListener(actionEvent -> {
            new MyPreferences().show();
        });

        checkButton.addActionListener(actionEvent -> {
            checkButtonPressed();
        });
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

        browseButton.addActionListener(actionEvent -> {
            browseButtonPressed();
        });

        cancelButton.addActionListener(actionEvent -> {
            cancelButtonPressed();
        });

        startButton.addActionListener(actionEvent -> new Thread(new Runnable() {
            @Override
            public void run() {
                startButtonPressed();
            }
        }).start());

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
                    ex.printStackTrace();
                    logTextArea.append(String.format(Language.get("message.NaN") + "\n", startChapterNumberTextField.getText()));
                    startChapterNumberTextField.setText("");
                }
            }
        });

        divisionCountTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    Integer.parseInt(divisionCountTextField.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logTextArea.append(String.format(Language.get("message.NaN") + "\n", divisionCountTextField.getText()));
                    divisionCountTextField.setText("");
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
                    ex.printStackTrace();
                    logTextArea.append(String.format(Language.get("message.NaN") + "\n", endChapterNumberTextField.getText()));
                    endChapterNumberTextField.setText("");
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

    private void show() {
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
    }

    private void checkButtonPressed() {
        if (!urlTextField.getText().isEmpty()) {
            currentMangaData = ParseManager.parse(urlTextField.getText(), logTextArea);
            if (currentMangaData == null) {
                JOptionPane.showMessageDialog(frame, String.format(Language.get("message.unknown_domain") + "\n", URI.create(urlTextField.getText()).getHost()));
                return;
            }
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

    private void startButtonPressed() {
        startButton.setEnabled(false);
        if (isToZipFile) ParseManager.toZipFiles(
                URI.create(urlTextField.getText()),
                isFullDownload ? 1 : Integer.parseInt(startChapterNumberTextField.getText()),
                isFullDownload ? maxChaptersCount : Integer.parseInt(endChapterNumberTextField.getText()),
                selectedFile, logTextArea, prefixTextField.getText().isEmpty() ? "" : prefixTextField.getText(),
                mainProgressBar, secondaryProgressBar);
        else ParseManager.download(
                URI.create(urlTextField.getText()),
                isFullDownload ? 1 : Integer.parseInt(startChapterNumberTextField.getText()),
                isFullDownload ? maxChaptersCount : Integer.parseInt(endChapterNumberTextField.getText()),
                isDivideEnabled ? Integer.parseInt(divisionCountTextField.getText()) : 1, selectedFile,
                logTextArea, prefixTextField.getText().isEmpty() ? "" : prefixTextField.getText(),
                mainProgressBar, secondaryProgressBar);
        startButton.setEnabled(true);
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
}
