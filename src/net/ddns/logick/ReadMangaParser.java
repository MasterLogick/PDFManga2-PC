package net.ddns.logick;

import org.apache.http.client.fluent.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class ReadMangaParser implements Parser {
    @Override
    public MangaData parse(URI mangaMainPageURI, JTextArea logTextArea) {
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.main_page_getting_error") + "\n", e.getMessage()));
        }
        String coverPath = mainPage.selectFirst("img[itemprop=\"image\"]").attr("src");
        ImageIcon cover = null;
        try {
            cover = new ImageIcon(ParseManager.resize(ImageIO.read(new URL(coverPath)), 156, 218));
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append(String.format(Language.get("message.cover_image_getting_error") + "\n", e.getMessage()));
        }
        Element infoElem = mainPage.selectFirst("div.subject-meta");
        Elements ps = infoElem.select("p");
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<h2>").append(mainPage.selectFirst("span.name").text()).append("</h2><br>");
        for (String s : ps.eachText()) {
            html.append(s).append("<br>");
        }
        html.append("</html>");
        Element chapters = mainPage.selectFirst("table.table-hover");
        if (chapters == null) {
            logTextArea.append(Language.get("message.do-not-have-chapters") + "\n");
            return null;
        }
        return new MangaData(cover, html.toString(), mangaMainPageURI.getPath().substring(mangaMainPageURI.getPath().lastIndexOf("/") + 1) + ".pdf", mangaMainPageURI.getPath().substring(mangaMainPageURI.getPath().lastIndexOf("/") + 1), chapters.select("a").size());
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
        Elements refs = mainPage.selectFirst("table.table-hover").select("a");
        List<String> chapters = refs.eachAttr("href");
        List<String> titles = refs.eachText();
        from = chapters.size() - from + 1;
        to = chapters.size() - to + 1;
        ListIterator<String> iterator = chapters.listIterator(from);
        String sitePrefix = mangaMainPageURI.getScheme() + "://" + mangaMainPageURI.getHost();
        ArrayList<String> imgURIs = new ArrayList<>();
        int counter = 0;
//        int i = 0;
        TreeMap<Integer, String> tableOfContentsReversed = new TreeMap<>();
        for (int i = 0; iterator.previousIndex() >= to - 1; ) {
            try {
                List ret = getImgRefs(sitePrefix + iterator.previous() + "?mtr=1");
                imgURIs.addAll(ret);
                tableOfContentsReversed.put(counter, titles.get(i++));
                counter += ret.size();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.chapter_page_parsing_error") + "\n", e.getMessage()));
            }

            logTextArea.append(String.format(Language.get("message.chapter_page_parsed") + "\n", from - iterator.nextIndex()));
            secondaryProgressBar.setValue(from - iterator.nextIndex());
            secondaryProgressBar.setString(String.format(Language.get("message.chapter_page_of_parsed"), from - iterator.nextIndex(), from - to + 1));
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
        TreeMap<Integer, String> tableOfContents = new TreeMap<>();
        Map.Entry<Integer, String>[] entries = new Map.Entry[tableOfContentsReversed.size()];
        entries = tableOfContentsReversed.entrySet().toArray(entries);
        for (int j = 0; j < entries.length; j++) {
            tableOfContents.put(entries[j].getKey(), entries[entries.length - 1 - j].getValue());
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
        Elements refs = mainPage.selectFirst("table.table-hover").select("a");
        List<String> chapters = refs.eachAttr("href");
        List<String> titles = refs.eachText();
        from = chapters.size() - from + 1;
        to = chapters.size() - to + 1;
        ListIterator<String> iterator = chapters.listIterator(from);
        String sitePrefix = mangaMainPageURI.getScheme() + "://" + mangaMainPageURI.getHost();
        ArrayList<String> imgURIs = new ArrayList<>();
        int counter = 0;
        int i = 0;
        TreeMap<Integer, String> tableOfContentsReversed = new TreeMap<>();
        while (iterator.previousIndex() >= to - 1) {
            try {
                List ret = getImgRefs(sitePrefix + iterator.previous() + "?mtr=1");
                imgURIs.addAll(ret);
                tableOfContentsReversed.put(counter, titles.get(i++));
//                System.out.println(counter + " " + titles.get(i++));
                counter += ret.size();
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append(String.format(Language.get("message.chapter_page_parsing_error") + "\n", e.getMessage()));
            }

            logTextArea.append(String.format(Language.get("message.chapter_page_parsed") + "\n", from - iterator.nextIndex()));
            secondaryProgressBar.setValue(from - iterator.nextIndex());
            secondaryProgressBar.setString(String.format(Language.get("message.chapter_page_of_parsed"), from - iterator.nextIndex(), from - to + 1));
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
        TreeMap<Integer, String> tableOfContents = new TreeMap<>();
        Map.Entry<Integer, String>[] entries = new Map.Entry[tableOfContentsReversed.size()];
        entries = tableOfContentsReversed.entrySet().toArray(entries);
        for (int j = 0; j < entries.length; j++) {
            tableOfContents.put(entries[j].getKey(), entries[entries.length - 1 - j].getValue());
        }
        ParseManager.download(imgURIs, divisionCounter, output, logTextArea, prefix, tableOfContents, mainProgressBar, secondaryProgressBar);
    }

    private List<String> getImgRefs(String chapterURI) throws IOException {
        String html = Request.Get(chapterURI).execute().returnContent().asString();
        String sub = html.substring(html.indexOf("rm_h.init( [['','") + "rm_h.init( [['','".length());
        sub = sub.substring(0, sub.indexOf("]], 0, false);"));
        sub = sub.substring(0, sub.lastIndexOf("\""));
        String[] uris = sub.split("\",[0-9]+,[0-9]+],\\['','");
        for (int i = 0; i < uris.length; i++) {
            uris[i] = uris[i].replace("',\"", "");
        }
        return Arrays.asList(uris);
    }
}
