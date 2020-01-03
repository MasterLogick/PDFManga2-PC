package net.ddns.logick;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ToZipPostProcessing implements PostDownloadingProcessing {
    private File output;
    private String prefix;

    public ToZipPostProcessing(File output, String prefix) {
        this.output = output;
        this.prefix = prefix;
    }

    @Override
    public void postProcess(BufferedImage[] images, TreeMap<Integer, String> tableOfContents) {
        Main.increaseAndUpdateMainProgressBarState(Language.get("message.status.push_to_disk"));
        Main.initialiseSecondaryProgressBar(images.length, "");
        Map.Entry<Integer, String>[] table = new Map.Entry[tableOfContents.size()];
        table = tableOfContents.entrySet().toArray(table);
        for (int i = 0; i < tableOfContents.size(); i++) {
            try {
                File f = new File(output.getAbsolutePath() + File.separator + prefix + "-part-" + (i + 1) + ".zip");
                Files.deleteIfExists(Paths.get(f.getPath()));
                Files.createFile(Paths.get(f.getPath()));
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));
                int endIndex = ((i + 1 < table.length) ? table[i + 1].getKey() : images.length);
                for (int j = table[i].getKey(); j < endIndex; j++) {
                    zos.putNextEntry(new ZipEntry((j - table[i].getKey()) + ".png"));
                    ImageIO.write(images[j], "png", zos);
                    if (Thread.interrupted()) {
                        return;
                    }
                    Main.LOG.info(String.format(Language.get("message.info.image_added"), j + 1, i + 1));
                    Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.info.image_added"), j + 1, i + 1));
                    if (Thread.interrupted()) {
                        return;
                    }
                }
                zos.close();
            } catch (IOException e) {
                Main.LOG.error(Language.get("message.error.filesystem"), e);
            }
        }
        Main.completedOnProgressBar();
        System.gc();
    }

    @Override
    public void setBanner(String banner) {
        //this method must have empty body because zip images shouldn't have banners
    }
}
