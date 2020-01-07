package net.ddns.logick;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.*;

class ParseManager {
    private static final String EXTENSION = ".pdf";
    static RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(Main.TIMEOUT).setConnectTimeout(Main.TIMEOUT).build();
    static CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
    private static HashMap<String, Parser> parsers = new HashMap<>();


    static void addParser(Parser p, String... domains) {
        for (String domain :
                domains) {
            parsers.put(domain, p);
        }
    }

    static MangaData parse(String mangaMainPageURL) {
        URI uri = URI.create(mangaMainPageURL);
        Parser parser = parsers.get(uri.getHost());
        if (parser != null) {
            Document mainPage = null;
            try {
                mainPage = Jsoup.connect(mangaMainPageURL).timeout(Main.TIMEOUT).get();
            } catch (SocketTimeoutException e) {
                Main.LOG.error(Language.get("message.error.timeout"), e);
                return MangaData.EMPTY;
            } catch (IOException e) {
                Main.LOG.error(Language.get("message.error.main_page_getting"), e);
            }
            ImageIcon cover = parser.getCoverImage(mainPage);
            String htmlEncodedInfo = parser.getHtmlEncodedData(mainPage);
            String defaultFilename = getDefaultFilename(mangaMainPageURL);
            String defaultFilePrefix = getDefaultFilePrefix(mangaMainPageURL);
            int chaptersCount = parser.getChaptersCount(mainPage);
            return new MangaData(uri, cover, htmlEncodedInfo, defaultFilename, defaultFilePrefix, chaptersCount);
        } else {
            Main.LOG.error(String.format(Language.get("message.error.unknown_domain"), uri.getHost()));
            return MangaData.EMPTY;
        }
    }

    public static String getDefaultFilePrefix(String mangaMainPageURL) {
        String path = mangaMainPageURL;
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String getDefaultFilename(String mangaMainPageURL) {
        return getDefaultFilePrefix(mangaMainPageURL) + EXTENSION;
    }

    static void download(URI mangaMainPageURI, int from, int to, PostDownloadingProcessing postProcessor) {
        if (isDisconnectedFormNet(mangaMainPageURI.getHost())) {
            Main.LOG.error(Language.get("message.error.no_connection"));
            cancel();
            return;
        }
        Parser parser = parsers.get(mangaMainPageURI.getHost());
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).timeout(Main.TIMEOUT).get();
        } catch (SocketTimeoutException e) {
            Main.LOG.error(Language.get("message.error.timeout"), e);
            cancel();
        } catch (IOException e) {
            Main.LOG.error(Language.get("message.error.main_page_getting"), e);
            cancel();
        }
        Main.initialiseSecondaryProgressBar(to - from + 1, "");
        Main.increaseAndUpdateMainProgressBarState(Language.get("message.status.chapter_pages_parsing"));
        List<String> chaptersLocations = parser.getChaptersLocations(mainPage);
        List<String> chaptersNames = parser.getChaptersNames(mainPage);
        URI resolver = URI.create(mangaMainPageURI.getScheme() + "://" + mangaMainPageURI.getHost());
        ListIterator<String> chaptersLocationsIterator = chaptersLocations.listIterator(from - 1);
        ListIterator<String> chapterNamesIterator = chaptersNames.listIterator(from - 1);
        TreeMap<Integer, String> tableOfContents = new TreeMap<>();
        ArrayList<String> imagesLocations = new ArrayList<>();
        int currentLocatedImages = 0;
        while (chaptersLocationsIterator.nextIndex() < to) {
            try {
                List<String> chapterImagesLocations = parser.getChapterImagesLocations(Jsoup.connect(resolver.resolve(chaptersLocationsIterator.next()).toString()).timeout(Main.TIMEOUT).get());
                imagesLocations.addAll(chapterImagesLocations);
                tableOfContents.put(currentLocatedImages, chapterNamesIterator.next());
                currentLocatedImages += chapterImagesLocations.size();
                Main.LOG.info(String.format(Language.get("message.info.chapter_page_parsed"), chaptersLocationsIterator.nextIndex() - from));
            } catch (SocketTimeoutException e) {
                Main.LOG.error(Language.get("message.error.timeout"), e);
                cancel();
            } catch (IOException e) {
                Main.LOG.error(Language.get("message.error.chapter_page_getting"), e);
                cancel();
            }
        }
        httpclient.start();
        BufferedImage[] images = new BufferedImage[imagesLocations.size()];
        ListIterator<String> imagesLocationsIterator = imagesLocations.listIterator();
        DownloaderThread[] downloaderPull = new DownloaderThread[Main.THREADS_COUNT];
        int threadsCount = Main.THREADS_COUNT;
        Main.increaseAndUpdateMainProgressBarState(Language.get("message.status.downloading"));
        Main.initialiseSecondaryProgressBar(images.length, "");
        for (int i = 0; i < downloaderPull.length; i++) {
            downloaderPull[i] = new DownloaderThread(i, imagesLocationsIterator, images);
            downloaderPull[i].start();
            Main.LOG.debug(String.format(Language.get("message.info.thread_started"), i));
        }
        for (int i = 0; i < threadsCount; i++) {
            try {
                downloaderPull[i].join();
            } catch (InterruptedException e) {
                for (DownloaderThread t : downloaderPull) {
                    t.interrupt();
                }
                return;
            }
        }
        parser.preparePostProcessor(postProcessor);
        postProcessor.postProcess(images, tableOfContents);
    }

    public static boolean isDisconnectedFormNet(String host) {
        try {
            return !InetAddress.getByName(host).isReachable(Main.TIMEOUT);
        } catch (Exception e) {
            return true;
        }

    }

    static void cancel() {
        if (Main.currentDownloadingThread != null)
            Main.currentDownloadingThread.interrupt();
        Main.currentDownloadingThread = null;
        Main.cancelOnProgressBar();
        Main.LOG.debug(Language.get("message.info.canceled"));
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
