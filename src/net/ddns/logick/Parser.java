package net.ddns.logick;

import javax.swing.*;
import java.io.File;
import java.net.URI;

public interface Parser {

    MangaData parse(URI mangaMainPageURI, JTextArea logTextArea);

    void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea textArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar);
}
