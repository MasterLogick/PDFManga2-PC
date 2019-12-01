package net.ddns.logick;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ParseManager {
    static RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(Main.TIMEOUT).setConnectTimeout(Main.TIMEOUT).build();
    static CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
    static boolean isWork = false;
    private static HashMap<String, Parser> parsers = new HashMap<>();

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
            logTextArea.append(String.format(Language.get("message.unknown_domain") + "\n", uri.getHost()));
            return null;
        }
    }

    static void toZipFiles(URI mangaMainPageURI, int from, int to, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        try {
            if (!isConnectedToNet(mangaMainPageURI.getHost())) {
                JOptionPane.showMessageDialog(null, Language.get("message.no_connection") + "\n");
                logTextArea.append(Language.get("message.no_connection") + "\n");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parser parser = parsers.get(mangaMainPageURI.getHost());
        if (parser != null) {
            isWork = true;
            parser.toZipFiles(mangaMainPageURI, from, to, output, logTextArea, prefix, mainProgressBar, secondaryProgressBar);
        } else
            logTextArea.append(String.format(Language.get("message.unknown_domain") + "\n", mangaMainPageURI.getHost()));
    }

    static void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        try {
            if (!isConnectedToNet(mangaMainPageURI.getHost())) {
                JOptionPane.showMessageDialog(null, Language.get("message.no_connection") + "\n");
                logTextArea.append(Language.get("message.no_connection") + "\n");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parser parser = parsers.get(mangaMainPageURI.getHost());
        if (parser != null) {
            isWork = true;
            parser.download(mangaMainPageURI, from, to, divisionCounter, output, logTextArea, prefix, mainProgressBar, secondaryProgressBar);
        } else
            logTextArea.append(String.format(Language.get("message.unknown_domain") + "\n", mangaMainPageURI.getHost()));
    }

    private static boolean isConnectedToNet(String host) throws IOException {
        return InetAddress.getByName(host).isReachable(Main.TIMEOUT);
    }

    static void cancel() {
        isWork = false;
    }

    static void download(ArrayList<String> imgURIs, int divisionCounter, File output, JTextArea logTextArea, String prefix, TreeMap<Integer, String> tableOfContents, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        if (!ParseManager.isWork) {
            Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
            return;
        }

        httpclient.start();
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.getting_pages"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(imgURIs.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        ListIterator<String> iterator = imgURIs.listIterator();
        PDPage[] pages = new PDPage[imgURIs.size()];
        PDDocument[] docs = new PDDocument[divisionCounter];
        PDDocumentOutline[] outlines = new PDDocumentOutline[divisionCounter];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = new PDDocument();
            outlines[i] = new PDDocumentOutline();
            docs[i].getDocumentCatalog().setDocumentOutline(outlines[i]);
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
                    int retries = 0;
                    while (retries < Main.MAX_RETRIES && bi == null)
                        try {
                            HttpGet httpGet = new HttpGet(s);
                            Future<HttpResponse> response = httpclient.execute(httpGet, null);
                            bi = ImageIO.read(response.get().getEntity().getContent());
                        } catch (Exception e) {
                            retries++;
                            try {
                                if (retries == 1 && !isConnectedToNet(URI.create(s).getHost())) {
                                    JOptionPane.showMessageDialog(null, Language.get("message.no_connection"));
                                    logTextArea.append(Language.get("message.no_connection") + "\n");
                                    return;
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            e.printStackTrace();
                            logTextArea.append(String.format(Language.get("message.page_getting_error") + "\n", e.getMessage(), s, retries, Main.MAX_RETRIES));
                        }
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
                        logTextArea.append(String.format(Language.get("message.image_drawing_error") + "\n", e.getMessage()));
                    }
                    pages[next] = page;
                    logTextArea.append(String.format(Language.get("message.image_downloaded") + "\n", next + 1, imgURIs.size(), finalI));
                    secondaryProgressBar.setValue(iterator.nextIndex());
                    secondaryProgressBar.setString(String.format(Language.get("message.image_downloaded"), next + 1, imgURIs.size(), finalI));
                }
            }).start();
        }
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            if (!fin[i]) i--;
            System.out.print("");
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.page_adding") + "\n", mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(pages.length);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        for (int i = 0; i < pages.length; i++) {
            docs[i / size].addPage(pages[i]);
            logTextArea.append(String.format(Language.get("message.page_added") + "\n", i + 1, imgURIs.size()));
            secondaryProgressBar.setValue(i);
            secondaryProgressBar.setString(String.format(Language.get("message.page_added"), i + 1, imgURIs.size()));
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
        }
        AtomicInteger counter = new AtomicInteger();
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.creating_contents") + "\n", mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(tableOfContents.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        tableOfContents.forEach((integer, s) -> {
            PDOutlineItem outline = new PDOutlineItem();
            outline.setTitle(s);
            PDPageXYZDestination dest = new PDPageXYZDestination();
            dest.setTop((int) pages[integer].getCropBox().getHeight());
            dest.setLeft(0);
            dest.setPage(pages[integer]);
            outline.setDestination(dest);
            outlines[integer / size].addLast(outline);
            logTextArea.append(String.format(Language.get("message.outline") + "\n", counter.incrementAndGet(), tableOfContents.size()));
            secondaryProgressBar.setValue(counter.get());
            secondaryProgressBar.setString(String.format(Language.get("message.outline"), counter.get(), tableOfContents.size()));
        });
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.writing"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        if (divisionCounter == 1) {
            try {
                output.createNewFile();
                docs[0].save(output);
                docs[0].close();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.filesystem_error") + "\n", e.getMessage()));
            }
            logTextArea.append(Language.get("message.written_file") + "\n");
        } else {
            secondaryProgressBar.setMaximum(docs.length);
            secondaryProgressBar.setValue(0);
            secondaryProgressBar.setString("");
            if (!output.exists()) {
                output.mkdir();
            }
            for (int i = 0; i < docs.length; i++) {
                File f = new File(output.getAbsolutePath() + File.separator + String.format(Language.get("file.name"), prefix, i + 1));
                try {
                    f.createNewFile();
                } catch (AccessControlException e) {
                    e.printStackTrace();
                    logTextArea.append(Language.get("message.filesystem_access_error") + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    docs[i].save(f);
                    docs[i].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                secondaryProgressBar.setValue(i);
                secondaryProgressBar.setString(String.format(Language.get("message.written_part"), i + 1));
                logTextArea.append(String.format(Language.get("message.written_part") + "\n", i + 1));
                if (!ParseManager.isWork) {
                    Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                    for (int j = i; j > -1; j--) {
                        File file = new File(output.getAbsolutePath() + File.separator + String.format(Language.get("file.name"), prefix, i + 1));
                        try {
                            file.delete();
                        } catch (AccessControlException e) {
                            e.printStackTrace();
                            logTextArea.append(Language.get("message.filesystem_access_error") + "\n");
                        }
                        logTextArea.append(String.format(Language.get("message.file_deleted") + "\n", f.getName()));
                    }
                    return;
                }
            }
        }
        isWork = false;
        mainProgressBar.setValue(0);
        mainProgressBar.setString(Language.get("progress.completed"));
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("Completed");
        System.gc();
    }

    public static void download(ArrayList<String> imgURIs, int divisionCounter, File output, JTextArea logTextArea, String prefix, TreeMap<Integer, String> tableOfContents, String banner, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        if (!ParseManager.isWork) {
            Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
            return;
        }

        httpclient.start();
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.getting_pages"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(imgURIs.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        ListIterator<String> iterator = imgURIs.listIterator();
        PDPage[] pages = new PDPage[imgURIs.size()];
        PDDocument[] docs = new PDDocument[divisionCounter];
        PDDocumentOutline[] outlines = new PDDocumentOutline[divisionCounter];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = new PDDocument();
            outlines[i] = new PDDocumentOutline();
            docs[i].getDocumentCatalog().setDocumentOutline(outlines[i]);
        }
        int size = imgURIs.size() / divisionCounter;
        final boolean[] fin = new boolean[Main.THREADS_COUNT];
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        float alpha = 0.4f;
        graphicsState.setStrokingAlphaConstant(alpha);
        PDFont font = null;
        try {
            font = PDType0Font.load(docs[0], Main.class.getResourceAsStream("times.ttf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        AtomicInteger counter = new AtomicInteger();
        int pos = 2;
        float fontSize = 14f;
        float width = 0;
        try {
            width = font.getStringWidth(banner) / 1000 * fontSize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            int finalI = i;
            PDFont finalFont = font;
            PDFont finalFont1 = font;
            AtomicInteger finalCounter = counter;
            float finalWidth = width;
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
                    int retries = 0;
                    while (retries < Main.MAX_RETRIES && bi == null)
                        try {
                            HttpGet httpGet = new HttpGet(s);
                            Future<HttpResponse> response = httpclient.execute(httpGet, null);
                            bi = ImageIO.read(response.get().getEntity().getContent());
                        } catch (Exception e) {
                            retries++;
                            try {
                                if (retries == 1 && !isConnectedToNet(URI.create(s).getHost())) {
                                    JOptionPane.showMessageDialog(null, Language.get("message.no_connection"));
                                    logTextArea.append(Language.get("message.no_connection") + "\n");
                                    return;
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            e.printStackTrace();
                            logTextArea.append(String.format(Language.get("message.page_getting_error") + "\n", e.getMessage(), s, retries, Main.MAX_RETRIES));
                        }
                    PDPage page = new PDPage();
                    page.setCropBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
                    page.setMediaBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
                    PDImageXObject pdImage;
                    try {
                        pdImage = LosslessFactory.createFromImage(docs[next / size], bi);
                        PDPageContentStream contents = new PDPageContentStream(docs[next / size], page);
                        contents.drawImage(pdImage, 0, 0);
                        contents.setGraphicsStateParameters(graphicsState);
                        contents.setFont(finalFont, fontSize);
                        contents.setNonStrokingColor(Color.WHITE);
//                        System.out.println(finalFont1.getStringWidth(banner) / 100);
                        contents.addRect(pos - 1, pos - 1, finalWidth + 3, fontSize + 1);
                        contents.fillAndStrokeEvenOdd();
//                        contents.setStrokingColor(new Color(255, 255, 255));
                        contents.beginText();
                        contents.setNonStrokingColor(Color.BLACK);
                        contents.newLineAtOffset(pos, pos + 2);
                        contents.showText(banner);
                        contents.endText();
                        contents.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        logTextArea.append(String.format(Language.get("message.image_drawing_error") + "\n", e.getMessage()));
                    }
                    pages[next] = page;
                    logTextArea.append(String.format(Language.get("message.image_downloaded") + "\n", next + 1, imgURIs.size(), finalI));
                    secondaryProgressBar.setValue(finalCounter.incrementAndGet());
                    secondaryProgressBar.setString(String.format(Language.get("message.image_downloaded"), next + 1, imgURIs.size(), finalI));
                }
            }).start();
        }
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            if (!fin[i]) i--;
            System.out.print("");
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.page_adding") + "\n", mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(pages.length);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        for (int i = 0; i < pages.length; i++) {
            docs[i / size].addPage(pages[i]);
            logTextArea.append(String.format(Language.get("message.page_added") + "\n", i + 1, imgURIs.size()));
            secondaryProgressBar.setValue(i);
            secondaryProgressBar.setString(String.format(Language.get("message.page_added"), i + 1, imgURIs.size()));
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
        }
        counter = new AtomicInteger();
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.creating_contents") + "\n", mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(tableOfContents.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        AtomicInteger finalCounter1 = counter;
        tableOfContents.forEach((integer, s) -> {
            PDOutlineItem outline = new PDOutlineItem();
            outline.setTitle(s);
            PDPageXYZDestination dest = new PDPageXYZDestination();
            dest.setTop((int) pages[integer].getCropBox().getHeight());
            dest.setLeft(0);
            dest.setPage(pages[integer]);
            outline.setDestination(dest);
            outlines[integer / size].addLast(outline);
            logTextArea.append(String.format(Language.get("message.outline") + "\n", finalCounter1.incrementAndGet(), tableOfContents.size()));
            secondaryProgressBar.setValue(finalCounter1.get());
            secondaryProgressBar.setString(String.format(Language.get("message.outline"), finalCounter1.get(), tableOfContents.size()));
        });
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.writing"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        if (divisionCounter == 1) {
            try {
                output.createNewFile();
                docs[0].save(output);
                docs[0].close();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.filesystem_error") + "\n", e.getMessage()));
            }
            logTextArea.append(Language.get("message.written_file") + "\n");
        } else {
            secondaryProgressBar.setMaximum(docs.length);
            secondaryProgressBar.setValue(0);
            secondaryProgressBar.setString("");
            if (!output.exists()) {
                output.mkdir();
            }
            for (int i = 0; i < docs.length; i++) {
                File f = new File(output.getAbsolutePath() + File.separator + String.format(Language.get("file.name"), prefix, i + 1));
                try {
                    f.createNewFile();
                } catch (AccessControlException e) {
                    e.printStackTrace();
                    logTextArea.append(Language.get("message.filesystem_access_error") + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    docs[i].save(f);
                    docs[i].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                secondaryProgressBar.setValue(i);
                secondaryProgressBar.setString(String.format(Language.get("message.written_part"), i + 1));
                logTextArea.append(String.format(Language.get("message.written_part") + "\n", i + 1));
                if (!ParseManager.isWork) {
                    Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                    for (int j = i; j >= 0; j--) {
                        File file = new File(output.getAbsolutePath() + File.separator + String.format(Language.get("file.name"), prefix, i + 1));
                        try {
                            file.delete();
                        } catch (AccessControlException e) {
                            e.printStackTrace();
                            logTextArea.append(Language.get("message.filesystem_access_error") + "\n");
                        }
                        logTextArea.append(String.format(Language.get("message.file_deleted") + "\n", f.getName()));
                    }
                    return;
                }
            }
        }
        isWork = false;
        mainProgressBar.setValue(0);
        mainProgressBar.setString(Language.get("progress.completed"));
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

    public static void toZipFiles(ArrayList<String> imgURIs, File output, JTextArea logTextArea, String prefix, TreeMap<Integer, String> tableOfContents, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        if (!ParseManager.isWork) {
            Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
            return;
        }
        httpclient.start();
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.getting_pages"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(imgURIs.size());
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        BufferedImage[] images = new BufferedImage[imgURIs.size()];
        ListIterator<String> iterator = imgURIs.listIterator(0);
        final boolean[] fin = new boolean[Main.THREADS_COUNT];
        AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            int finalI = i;
            new Thread(() -> {
                int next;
                String s;
                while (true) {
                    synchronized (iterator) {
                        if (iterator.hasNext()) {
                            next = iterator.nextIndex();
//                            System.out.println(next);
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
                    int retries = 0;
                    while (retries < Main.MAX_RETRIES && bi == null)
                        try {
                            HttpGet httpGet = new HttpGet(s);
                            Future<HttpResponse> response = httpclient.execute(httpGet, null);
                            bi = ImageIO.read(response.get().getEntity().getContent());
//                            System.out.println(bi == null);
                        } catch (Exception e) {
                            retries++;
                            try {
                                if (retries == 1 && !isConnectedToNet(URI.create(s).getHost())) {
                                    JOptionPane.showMessageDialog(null, Language.get("message.no_connection"));
                                    logTextArea.append(Language.get("message.no_connection") + "\n");
                                    return;
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            e.printStackTrace();
                            logTextArea.append(String.format(Language.get("message.page_getting_error") + "\n", e.getMessage(), s, retries, Main.MAX_RETRIES));
                        }
//                    if (next - 1 < 0) System.out.println(next + " " + s);
                    images[next] = bi;
                    logTextArea.append(String.format(Language.get("message.image_downloaded") + "\n", next + 1, imgURIs.size(), finalI));
                    secondaryProgressBar.setValue(counter.incrementAndGet());
                    secondaryProgressBar.setString(String.format(Language.get("message.image_downloaded"), next + 1, imgURIs.size(), finalI));
                }
            }).start();
        }
        for (int i = 0; i < Main.THREADS_COUNT; i++) {
            if (!fin[i]) i--;
            System.out.print("");
        }
        try {

        } catch (Exception e) {
        }
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.page_adding") + "\n", mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(images.length);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        Map.Entry<Integer, String>[] table = new Map.Entry[tableOfContents.size()];
        table = tableOfContents.entrySet().toArray(table);
        for (int i = 0; i < tableOfContents.size(); i++) {
            try {
                File f = new File(output.getAbsolutePath() + File.separator + prefix + "-part-" + (i + 1) + ".zip");
                Files.deleteIfExists(Paths.get(f.getPath()));
                f.createNewFile();
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));
                int endIndex = ((i + 1 < table.length) ? table[i + 1].getKey() : images.length);
                for (int j = table[i].getKey(); j < endIndex; j++) {
                    zos.putNextEntry(new ZipEntry((j - table[i].getKey()) + ".png"));
                    ImageIO.write(images[j], "png", zos);
                    if (!ParseManager.isWork) {
                        Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                        return;
                    }
                    logTextArea.append(String.format(Language.get("message.image_added") + "\n", j + 1, i + 1));
                    secondaryProgressBar.setValue(j);
                    secondaryProgressBar.setString(String.format(Language.get("message.image_added"), j + 1, i + 1));
                    if (!ParseManager.isWork) {
                        Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                        return;
                    }
                }
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isWork = false;
        mainProgressBar.setValue(0);
        mainProgressBar.setString(Language.get("progress.completed"));
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("Completed");
        System.gc();
       /* for (int i = 0; i < images.length; i++) {

            logTextArea.append(String.format(Language.get("message.page_added") + "\n", i + 1, imgURIs.size()));
            secondaryProgressBar.setValue(i);
            secondaryProgressBar.setString(String.format(Language.get("message.page_added"), i + 1, imgURIs.size()));
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
        }
        AtomicInteger counter = new AtomicInteger();*/

    }
}
