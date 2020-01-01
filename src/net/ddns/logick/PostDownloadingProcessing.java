package net.ddns.logick;

import java.awt.image.BufferedImage;
import java.util.TreeMap;

public interface PostDownloadingProcessing {
    void postProcess(BufferedImage[] images, TreeMap<Integer, String> tableOfContents);

    void setBanner(String banner);
}
