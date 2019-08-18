package org.ddns.logick;

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
            logTextArea.append("Main page getting error: " + e.getMessage() + "\n");
        }
        String coverPath = mainPage.selectFirst("img[itemprop=\"image\"]").attr("src");
        ImageIcon cover = null;
        try {
            cover = new ImageIcon(ParseManager.resize(ImageIO.read(new URL(coverPath)), 156, 218));
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append("Cover image getting error: " + e.getMessage() + "\n");
        }
        Element infoElem = mainPage.selectFirst("div.subject-meta");
        Elements ps = infoElem.select("p");
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<h2>").append(mainPage.selectFirst("span.name").text()).append("</h2><br>");
        Iterator<String> iterator = ps.eachText().iterator();
        while (iterator.hasNext()) {
            html.append(iterator.next()).append("<br>");
        }
        html.append("</html>");
        return new MangaData(cover, html.toString(), mangaMainPageURI.getPath().substring(mangaMainPageURI.getPath().lastIndexOf("/") + 1) + ".pdf", mainPage.selectFirst("table.table-hover").select("a").size());
    }

    @Override
    public void download(URI mangaMainPageURI, int from, int to, int divisionCounter, File output, JTextArea logTextArea, String prefix, JProgressBar mainProgressBar, JProgressBar secondaryProgressBar) {
        mainProgressBar.setValue(mainProgressBar.getValue() + 1);
        mainProgressBar.setString(mainProgressBar.getValue() + "/" + mainProgressBar.getMaximum() + ": parsing chapters main pages");
        secondaryProgressBar.setMaximum(to - from + 1);
        secondaryProgressBar.setValue(0);
        secondaryProgressBar.setString("");
        Document mainPage = null;
        try {
            mainPage = Jsoup.connect(mangaMainPageURI.toString()).get();
        } catch (IOException e) {
            e.printStackTrace();
            logTextArea.append("Main page getting error: " + e.getMessage() + "\n");
        }
        List<String> chapters = mainPage.selectFirst("table.table-hover").select("a").eachAttr("href");
        from = chapters.size() - from + 1;
        to = chapters.size() - to + 1;
        ListIterator<String> iterator = chapters.listIterator(from);
        String sitePrefix = mangaMainPageURI.getScheme() + "://" + mangaMainPageURI.getHost();
        ArrayList<String> imgURIs = new ArrayList<>();
        while (iterator.previousIndex() >= to - 1) {
            try {
                imgURIs.addAll(getImgRefs(sitePrefix + iterator.previous() + "?mtr=1"));
            } catch (IOException e) {
                e.printStackTrace();
                logTextArea.append("Chapter page parsing error: " + e.getMessage() + "\n");
            }
            logTextArea.append("Chapter page " + (from - iterator.nextIndex()) + " parsed\n");
            secondaryProgressBar.setValue(from - iterator.nextIndex());
            secondaryProgressBar.setString((from - iterator.nextIndex()) + " of " + (from - to + 1) + " chapters parsed");
            if (!ParseManager.isWork) {
                mainProgressBar.setValue(0);
                mainProgressBar.setString("Canceled");
                secondaryProgressBar.setValue(0);
                secondaryProgressBar.setString("Canceled");
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ParseManager.download(imgURIs, divisionCounter, output, logTextArea, prefix, mainProgressBar, secondaryProgressBar);
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
