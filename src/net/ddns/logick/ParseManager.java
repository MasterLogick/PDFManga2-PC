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
                mainPage = Jsoup.connect(mangaMainPageURL).get();
            } catch (IOException e) {
                e.printStackTrace();
//todo make better capturing
            }
            if (mainPage == null) {
                Main.LOG.error(/*todo main page downloading and parsing error message*/"");
            }
            ImageIcon cover = parser.getCoverImage(mainPage);
            String htmlEncodedInfo = parser.getHtmlEncodedData(mainPage);
            String defaultFilename = getDefaultFilename(mangaMainPageURL);
            String defaultFilePrefix = getDefaultFilePrefix(mangaMainPageURL);
            int chaptersCount = parser.getChaptersCount(mainPage);
            return new MangaData(cover, htmlEncodedInfo, defaultFilename, defaultFilePrefix, chaptersCount);
        } else {
            Main.LOG.error(String.format(Language.get("message.unknown_domain"), uri.getHost()));
            return MangaData.EMPTY;
        }
    }

    public static int getChaptersCount(String mangaMainPageURL, Document mainPage) {
        URI uri = URI.create(mangaMainPageURL);
        Parser parser = parsers.get(uri.getHost());
        if (parser != null) {
            try {
                mainPage = Jsoup.connect(mangaMainPageURL).get();
            } catch (IOException e) {
                e.printStackTrace();
//todo make better capturing
            }
            if (mainPage == null) {
                Main.LOG.error(/*todo main page downloading and parsing error message*/"");
            }
            return parser.getChaptersCount(mainPage);

        } else {
            Main.LOG.error(String.format(Language.get("message.unknown_domain"), uri.getHost()));
            return 0;
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
        try {
            if (!isConnectedToNet(mangaMainPageURI.getHost())) {
                JOptionPane.showMessageDialog(null, Language.get("message.no_connection") + "\n");
                Main.LOG.error(Language.get("message.no_connection"));
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Parser parser = parsers.get(mangaMainPageURI.getHost());
        if (parser != null) {
            List<String> chaptersLocations = null;
            List<String> chaptersNames = null;
            try {
                Document mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
                chaptersLocations = parser.getChaptersLocations(mainPage);
                chaptersNames = parser.getChaptersNames(mainPage);
            } catch (IOException e) {
                e.printStackTrace();
//todo make better capturing
            }
            if (chaptersLocations == null) {
                Main.LOG.error(/*todo main page info parsing error message*/"");
                Main.cancelOnProgressBar();
                return;
            }
            URI resolver = URI.create(mangaMainPageURI.getScheme() + "://" + mangaMainPageURI.getHost());
            ListIterator<String> chaptersLocationsIterator = chaptersLocations.listIterator(from - 1);
            ListIterator<String> chapterNamesIterator = chaptersNames.listIterator(from - 1);
            TreeMap<Integer, String> tableOfContents = new TreeMap<>();
            ArrayList<String> imagesLocations = new ArrayList<>();
            int currentLocatedImages = 0;
            while (chaptersLocationsIterator.nextIndex() < to) {
                try {
                    List<String> chapterImagesLocations = parser.getChapterImagesLocations(Jsoup.connect(resolver.resolve(chaptersLocationsIterator.next()).toString()).get());
                    imagesLocations.addAll(chapterImagesLocations);
                    tableOfContents.put(currentLocatedImages, chapterNamesIterator.next());
                    currentLocatedImages += chapterImagesLocations.size();
                } catch (IOException e) {
                    e.printStackTrace();//todo make better capturing
                }
            }
            httpclient.start();
            BufferedImage[] images = new BufferedImage[imagesLocations.size()];
            ListIterator<String> imagesLocationsIterator = imagesLocations.listIterator();
            DownloaderThread[] downloaderPull = new DownloaderThread[Main.THREADS_COUNT];
            int threadsCount = Main.THREADS_COUNT;
            for (int i = 0; i < downloaderPull.length; i++) {
                downloaderPull[i] = new DownloaderThread(i, imagesLocationsIterator, images);
                downloaderPull[i].start();
                Main.LOG.debug(String.format(Language.get("message.thread_started"), i));
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
        } else
            Main.LOG.error(String.format(Language.get("message.unknown_domain"), mangaMainPageURI.getHost()));
    }

    public static boolean isConnectedToNet(String host) throws IOException {
        return InetAddress.getByName(host).isReachable(Main.TIMEOUT);
    }

    static void cancel() {
        Main.currentDownloadingThread.interrupt();
        Main.cancelOnProgressBar();
        Main.LOG.debug(Language.get("message.canceled"));
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
