package net.ddns.logick;

import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.File;
import java.net.URI;

public interface Parser {

//    MangaData parse(URI mangaMainPageURI, JTextArea logTextArea);

    ImageIcon getCoverImage(Document mainPage, JTextArea logTextArea);

    String getHtmlEncodedData(Document mainPage, JTextArea logTextArea);

    int getChaptersCount(Document mainPage, JTextArea logTextArea);

    void toZipFiles(URI mangaMainPageURI, int from, int to, File output, JTextArea textArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar);

    void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea textArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar);
}
