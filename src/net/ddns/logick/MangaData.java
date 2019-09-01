package net.ddns.logick;

import javax.swing.*;

class MangaData {
    ImageIcon cover;
    String htmlData;
    String defaultFileName;
    int chaptersCount;

    MangaData(ImageIcon cover, String htmlData, String defaultFileName, int chaptersCount) {
        this.cover = cover;
        this.htmlData = htmlData;
        this.chaptersCount = chaptersCount;
        this.defaultFileName = defaultFileName;
    }
}
