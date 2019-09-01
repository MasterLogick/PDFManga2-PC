package net.ddns.logick;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

class ParseManager {
    private static HashMap<String, Parser> parsers = new HashMap<>();
    private static ReadMangaParser readMangaParser = new ReadMangaParser();
    static boolean isWork = false;

    static void addParser(Parser p, String... domains) {
        for (String domain :
                domains) {
            parsers.put(domain, p);
        }
    }

    static MangaData parse(String mangaMainPageURL, JTextArea logTextArea) {
        URI uri = URI.create(mangaMainPageURL);
        Parser parser = parsers.get(uri.getHost());
        if (parser != null) return parser.parse(uri, logTextArea);
        else {
            logTextArea.append("Unknown domain: " + uri.getHost() + "\n");
            return null;
        }
    }

    static void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        Parser parser = parsers.get(mangaMainPageURI.getHost());
        if (parser != null) {
            isWork = true;
            parser.download(mangaMainPageURI, from, to, divisionCounter, output, logTextArea, prefix, mainProgressBar, secondaryProgressBar);
        } else
            logTextArea.append("Unknown domain: " + mangaMainPageURI.getHost() + "\n");
    }

    static void cancel() {
        isWork = false;
    }

    static void download(ArrayList<String> imgURIs, int divisionCounter, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        if (!ParseManager.isWork) {
            Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
            return;
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(mainProgressBar.getValue() + "/" + mainProgressBar.getMaximum() + ": getting pages");
        secondaryProgressBar.setMaximum(imgURIs.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        ListIterator<String> iterator = imgURIs.listIterator();
        PDPage[] pages = new PDPage[imgURIs.size()];
        PDDocument[] docs = new PDDocument[divisionCounter];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = new PDDocument();
        }
        int size = imgURIs.size() / divisionCounter;
        final boolean[] fin = new boolean[Main.THREADS_COUNT];
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            int finalI = i;
            new Thread(() -> {
                int next;
                String s;
                while (true) {
                    synchronized (iterator) {
                        if (iterator.hasNext()) {
                            next = iterator.nextIndex();
                            s = iterator.next();
                        } else {
                            fin[finalI] = true;
                            return;
                        }
                    }
                    if (!ParseManager.isWork) {
                        Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BufferedImage bi = null;
                    try {
                        bi = ImageIO.read(new URL(s));
                    } catch (Exception e) {
                        e.printStackTrace();
                        logTextArea.append("Page getting error: " + e.getMessage() + "\n");
                    }
                    logTextArea.append("Image " + (next + 1) + " of " + imgURIs.size() + " is downloaded\n");
                    PDPage page = new PDPage();
                    page.setCropBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
                    page.setMediaBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
                    PDImageXObject pdImage;
                    try {
                        pdImage = LosslessFactory.createFromImage(docs[next / size], bi);
                        PDPageContentStream contents = new PDPageContentStream(docs[next / size], page);
                        contents.drawImage(pdImage, 0, 0);
                        contents.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        logTextArea.append("Image to page drawing error: " + e.getMessage() + "\n");
                    }
                    pages[next] = page;
                    secondaryProgressBar.setValue(iterator.nextIndex());
                    secondaryProgressBar.setString("Image " + (next + 1) + " of " + imgURIs.size() + " is downloaded\n");
                }
            }).start();
        }
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            if (!fin[i]) i--;
            System.out.print("");
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(mainProgressBar.getValue() + "/" + mainProgressBar.getMaximum() + ": adding pages to document");
        secondaryProgressBar.setMaximum(pages.length);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        for (int i = 0; i < pages.length; i++) {
            docs[i / size].addPage(pages[i]);
            logTextArea.append("Page " + (i + 1) + " of " + imgURIs.size() + " added to document\n");
            secondaryProgressBar.setValue(i);
            secondaryProgressBar.setString("Page " + (i + 1) + " of " + imgURIs.size() + " added to document");
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(mainProgressBar.getValue() + "/" + mainProgressBar.getMaximum() + ": writing PDF to disk");
        if (divisionCounter == 1) {
            try {
                output.createNewFile();
                docs[0].save(output);
                docs[0].close();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append("Filesystem error: " + e.getMessage() + "\n");
            }
            logTextArea.append("PDF written to disk\n");
        } else {
            secondaryProgressBar.setMaximum(docs.length);
            secondaryProgressBar.setValue(0);
            secondaryProgressBar.setString("");
            if (!output.exists()) {
                output.mkdir();
            }
            for (int i = 0; i < docs.length; i++) {
                File f = new File(output.getAbsolutePath() + File.separator + prefix + "-" + (i + 1) + "-part.pdf");
                try {
                    f.createNewFile();
                } catch (AccessControlException e) {
                    e.printStackTrace();
                    logTextArea.append("Filesystem access error: " + e.getMessage() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    logTextArea.append("Filesystem error: " + e.getMessage() + "\n");
                }
                try {
                    docs[i].save(f);
                    docs[i].close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logTextArea.append("Filesystem error: " + e.getMessage() + "\n");
                }
                secondaryProgressBar.setValue(i);
                secondaryProgressBar.setString("PDF " + (i + 1) + " written to disk");
                logTextArea.append("PDF" + (i + 1) + " written to disk\n");
                if (!ParseManager.isWork) {
                    Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                    for (int j = i; j > -1; j--) {
                        File file = new File(output.getAbsolutePath() + File.separator + prefix + "-" + (i + 1) + "-part.pdf");
                        try {
                            file.delete();
                        } catch (AccessControlException e) {
                            e.printStackTrace();
                            logTextArea.append("Filesystem access error: " + e.getMessage() + "\n");
                        }
                        logTextArea.append("File " + f.getName() + "deleted\n");
                    }
                    return;
                }
            }
        }
        isWork = false;
        mainProgressBar.setValue(0);
        mainProgressBar.setString("Completed");
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("Completed");
        System.gc();
    }

    static BufferedImage resize(BufferedImage src, int width, int height) {
        Image tmp = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return img;
    }
}
