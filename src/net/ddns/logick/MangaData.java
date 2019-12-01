package net.ddns.logick;

import javax.swing.*;

class MangaData {
    ImageIcon cover;
    String htmlData;
    String defaultFileName;
    String defaultFilePrefix;
    int chaptersCount;

    MangaData(ImageIcon cover, String htmlData, String defaultFileName, String defaultFilePrefix, int chaptersCount) {
        this.cover = cover;
        this.htmlData = htmlData;
        this.defaultFilePrefix = defaultFilePrefix;
        this.chaptersCount = chaptersCount;
        this.defaultFileName = defaultFileName;
    }
}
