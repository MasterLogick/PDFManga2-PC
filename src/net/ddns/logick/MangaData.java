package net.ddns.logick;

import javax.swing.*;
import java.net.URI;

class MangaData {
    public static final MangaData EMPTY = new MangaData(null, null, "", "", "", 0);
    ImageIcon cover;
    String htmlData;
    String defaultFileName;
    String defaultFilePrefix;
    URI mangaURI;
    int chaptersCount;

    MangaData(URI mangaURI, ImageIcon cover, String htmlData, String defaultFileName, String defaultFilePrefix, int chaptersCount) {
        this.mangaURI = mangaURI;
        this.cover = cover;
        this.htmlData = htmlData;
        this.defaultFilePrefix = defaultFilePrefix;
        this.chaptersCount = chaptersCount;
        this.defaultFileName = defaultFileName;
    }
}
