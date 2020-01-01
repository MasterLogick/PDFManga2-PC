package net.ddns.logick;

import org.jsoup.nodes.Document;

import javax.swing.*;
import java.util.List;

public interface Parser {

    ImageIcon getCoverImage(Document mainPage);

    String getHtmlEncodedData(Document mainPage);

    int getChaptersCount(Document mainPage);

    List<String> getChaptersLocations(Document mainPage);

    List<String> getChaptersNames(Document mainPage);

    List<String> getChapterImagesLocations(Document chapterPage);

    void preparePostProcessor(PostDownloadingProcessing postProcessor);
}
