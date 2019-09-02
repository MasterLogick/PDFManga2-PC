package net.ddns.logick;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class Main {
    static final String USAGE_STRING = "Usage: java -jar PDFManga2.jar [THREADS_COUNT]\n[THREADS_COUNT] - optional parameter that sets amount of image downloading threads, 10 - default";
    static int THREADS_COUNT = 10;
    private JFrame frame;
    private JFileChooser fileChooser;
    private JPanel mainPanel;
    private JTextField urlTextField;
    private JButton checkButton;
    private JPanel checkPanel;
    private JLabel coverLabel;
    private JLabel descriptionLabel;
    private JPanel fullDownloadCheckboxPanel;
    private JCheckBox downloadFullMangaCheckBox;
    private JPanel chaptersPanel;
    private JTextField startChapterNumberTextField;
    private JTextField endChapterNumberTextField;
    private JPanel outputPanel;
    private JLabel outputNameLabel;
    private JPanel prefixPanel;
    private JTextField prefixTextField;
    private JTextField outputPathTextField;
    private JButton browseButton;
    private JPanel divisionCheckBoxPanel;
    private JCheckBox divideMangaCheckBox;
    private JPanel divisionPanel;
    private JTextField divisionCountTextField;
    private JPanel controlPanel;
    private JButton cancelButton;
    private JButton startButton;
    private JPanel processPanel;
    private JProgressBar mainProgressBar;
    private JProgressBar secondaryProgressBar;
    private JTextArea logTextArea;
    private JButton clearLogButton;
    private JScrollPane scrollPane;
    private Container[] onCheckEnabledContainers;
    private boolean isFullDownload;
    private boolean isDivideEnabled;
    private FileFilter pdfFilter;
    private FileFilter dirFilter;
    private File selectedFile;
    private int maxChaptersCount;
    private int scrollValue = 0;
    private boolean autoScrollToBottom = true;
    private JMenuItem menuItem;

    public Main() {
        maxChaptersCount = 0;
        Container[] onStartDisabledContainers = new Container[]{fullDownloadCheckboxPanel, chaptersPanel, outputPanel, prefixPanel, divisionCheckBoxPanel, divisionPanel, controlPanel, processPanel};
        onCheckEnabledContainers = new Container[]{fullDownloadCheckboxPanel, outputPanel, divisionCheckBoxPanel};
        pdfFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
            }

            @Override
            public String getDescription() {
                return "PDF File";
            }
        };
        dirFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directory";
            }
        };
        isFullDownload = true;
        isDivideEnabled = false;
        fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDFManga: Save manga");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.setMultiSelectionEnabled(false);
        frame = new JFrame("PDFManga2");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setContentPane(mainPanel);
        frame.pack();
        try {
            frame.setIconImage(ImageIO.read(Main.class.getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JMenu helpMenu = new JMenu("Help");
        menuItem = new JMenuItem("About");
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(helpMenu).add(menuItem);
        frame.setJMenuBar(menuBar);
        disableChildren(onStartDisabledContainers);
        initListeners();

    }

    private void initListeners() {
        menuItem.addActionListener(actionEvent -> JOptionPane.showMessageDialog(frame, "PDFManga 2\nOpen-source application for downloading manga from \"readmanga\" group sites\nWritten by: MasterLogick\nGithub: https://github.com/MasterLogick\n Version: 2.0.0"));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ParseManager.cancel();
                System.exit(0);
            }
        });
        checkButton.addActionListener(actionEvent -> {
            if (!urlTextField.getText().isEmpty()) {
                MangaData mangaData;
                mangaData = ParseManager.parse(urlTextField.getText(), logTextArea);
                if (mangaData == null) {
                    JOptionPane.showMessageDialog(frame, "Unknown domain: " + URI.create(urlTextField.getText()).getHost());
                    return;
                }
                coverLabel.setIcon(mangaData.cover);
                descriptionLabel.setText(mangaData.htmlData);
                checkPanel.setVisible(true);
                maxChaptersCount = mangaData.chaptersCount;
                startChapterNumberTextField.setText("1");
                endChapterNumberTextField.setText(String.valueOf(maxChaptersCount));
                enableChildren(onCheckEnabledContainers);
                fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory() + File.separator + mangaData.defaultFileName));
                frame.pack();
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
            if (isDivideEnabled) {
                outputNameLabel.setText("Output folder");
                String uselessFilePath = outputPathTextField.getText();
                int index = uselessFilePath.toLowerCase().lastIndexOf(".pdf");
                outputPathTextField.setText(uselessFilePath.substring(0, index <= 0 ? uselessFilePath.length() : index));
                enableChildren(divisionPanel, prefixPanel);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setFileFilter(dirFilter);
            } else {
                outputNameLabel.setText("Output file");
                outputPathTextField.setText(outputPathTextField.getText() + ".pdf");
                disableChildren(divisionPanel, prefixPanel);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setFileFilter(pdfFilter);
            }
        });
        browseButton.addActionListener(actionEvent -> {
            if (fileChooser.showDialog(frame, "Save") == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                outputPathTextField.setText(selectedFile.getAbsolutePath());
                enableChildren(controlPanel);
            }
        });
        cancelButton.addActionListener(actionEvent -> ParseManager.cancel());
        startButton.addActionListener(actionEvent -> new Thread(() -> {
            startButton.setEnabled(false);
            ParseManager.download(
                    URI.create(urlTextField.getText()),
                    isFullDownload ? 1 : Integer.parseInt(startChapterNumberTextField.getText()),
                    isFullDownload ? maxChaptersCount : Integer.parseInt(endChapterNumberTextField.getText()),
                    isDivideEnabled ? Integer.parseInt(divisionCountTextField.getText()) : 1, selectedFile,
                    logTextArea, prefixTextField.getText().isEmpty() ? "" : prefixTextField.getText(),
                    mainProgressBar, secondaryProgressBar);
            startButton.setEnabled(true);
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
                    logTextArea.append("Not A Number: " + startChapterNumberTextField.getText() + "\n");
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
                    logTextArea.append("Not A Number: " + divisionCountTextField.getText() + "\n");
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
                    logTextArea.append("Not A Number: " + endChapterNumberTextField.getText() + "\n");
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

    public static void main(String[] args) {
        try {
            if (args.length != 0) THREADS_COUNT = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println(USAGE_STRING);
            JOptionPane.showMessageDialog(null, USAGE_STRING);
            System.exit(0);
        }
        ParseManager.addParser(new ReadMangaParser(), "readmanga.me", "mintmanga.com","selfmanga.ru");
        new Main().show();
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

    static void cancelOnProgressBar(JProgressBar main, JProgressBar secondary) {
        main.setValue(0);
        main.setString("Canceled");
        secondary.setValue(0);
        secondary.setString("Canceled");
    }
}
