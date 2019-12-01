package net.ddns.logick;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

public class ReMangaParser implements Parser {

    @Override
    public MangaData parse(URI mangaMainPageURI, JTextArea logTextArea) {
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.main_page_getting_error") + "\n", e.getMessage()));
        }
        String coverPath = mainPage.selectFirst("meta[itemprop=image]").attr("content");
        ImageIcon cover = null;
        try {
            cover = new ImageIcon(ParseManager.resize(ImageIO.read(new URL(coverPath)), 156, 218));
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.cover_image_getting_error") + "\n", e.getMessage()));
        }
        String[] html = mainPage.selectFirst("div.info").wholeText().split("\n");
        StringBuilder sb = new StringBuilder("<html>" + mainPage.selectFirst("meta[itemprop=alternativeHeadline]").attr("content"));
        for (String s :
                html) {
            if (s.isEmpty()) continue;
            sb.append(s.trim() + "<br>");
        }
        sb.append("</html>");
        String path = mangaMainPageURI.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String defaultFileName = path.substring(path.lastIndexOf("/") + 1) + ".pdf";
        int chapterCount = mainPage.select("div.chapter").size();
        return new MangaData(cover, sb.toString(), defaultFileName, path.substring(path.lastIndexOf("/") + 1), chapterCount);
    }

    @Override
    public void toZipFiles(URI mangaMainPageURI, int from, int to, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.parsing_chapters_main_pages"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(to - from + 1);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.main_page_getting_error") + "\n", e.getMessage()));
        }

        Elements chapters = mainPage.select("a.name");
        from = chapters.size() - from + 1;
        to = chapters.size() - to + 1;
        ListIterator<String> tableOfContentIterator = chapters.eachText().listIterator(from);
        List<String> chaptersPaths = chapters.eachAttr("href");
        ListIterator<String> chaptersPathsIterator = chaptersPaths.listIterator(from);
        TreeMap<Integer, String> tableOfContents = new TreeMap<>();
        ArrayList<String> imgURIs = new ArrayList<>();
        for (int i = 0; chaptersPathsIterator.previousIndex() >= to - 1; ) {
            try {
                List arr = getImgRefs(mangaMainPageURI.resolve(chaptersPathsIterator.previous()).toString(), logTextArea);
                imgURIs.addAll(arr);
                tableOfContents.put(i, tableOfContentIterator.previous());
                i += arr.size();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.chapter_page_parsing_error") + "\n", e.getMessage()));
            }
            logTextArea.append(String.format(Language.get("message.chapter_page_parsed") + "\n", from - chaptersPathsIterator.nextIndex()));
            secondaryProgressBar.setValue(from - chaptersPathsIterator.nextIndex());
            secondaryProgressBar.setString((from - chaptersPathsIterator.nextIndex()) + " of " + (from - to + 1) + " chapters parsed");
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ParseManager.toZipFiles(imgURIs, output, logTextArea, prefix, tableOfContents, mainProgressBar, secondaryProgressBar);
    }

    @Override
    public void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(String.format(Language.get("message.parsing_chapters_main_pages"), mainProgressBar.getValue(), mainProgressBar.getMaximum()));
        secondaryProgressBar.setMaximum(to - from + 1);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.main_page_getting_error") + "\n", e.getMessage()));
        }

        Elements chapters = mainPage.select("a.name");
        from = chapters.size() - from + 1;
        to = chapters.size() - to + 1;
        ListIterator<String> tableOfContentIterator = chapters.eachText().listIterator(from);
        List<String> chaptersPaths = chapters.eachAttr("href");
        ListIterator<String> chaptersPathsIterator = chaptersPaths.listIterator(from);
        TreeMap<Integer, String> tableOfContents = new TreeMap<>();
        ArrayList<String> imgURIs = new ArrayList<>();
        for (int i = 0; chaptersPathsIterator.previousIndex() >= to - 1; ) {
            try {
                List arr = getImgRefs(mangaMainPageURI.resolve(chaptersPathsIterator.previous()).toString(), logTextArea);
                imgURIs.addAll(arr);
                tableOfContents.put(i, tableOfContentIterator.previous());
                i += arr.size();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.chapter_page_parsing_error") + "\n", e.getMessage()));
            }
            logTextArea.append(String.format(Language.get("message.chapter_page_parsed") + "\n", from - chaptersPathsIterator.nextIndex()));
            secondaryProgressBar.setValue(from - chaptersPathsIterator.nextIndex());
            secondaryProgressBar.setString((from - chaptersPathsIterator.nextIndex()) + " of " + (from - to + 1) + " chapters parsed");
            if (!ParseManager.isWork) {
                Main.cancelOnProgressBar(mainProgressBar, secondaryProgressBar);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ParseManager.download(imgURIs, divisionCounter, output, logTextArea, prefix, tableOfContents, Language.get("banner.remanga"), mainProgressBar, secondaryProgressBar);
    }

    private List<String> getImgRefs(String chapterURI, JTextArea logTextArea) throws IOException {
        Document page = null;
        try {
            page = Jsoup.connect(chapterURI).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.main_page_getting_error") + "\n", e.getMessage()));
        }
        return page.select("img.owl-lazy").eachAttr("data-src");
    }
}
