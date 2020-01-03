package net.ddns.logick;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ReadMangaParser implements Parser {

    @Override
    public ImageIcon getCoverImage(Document mainPage) {
        Element imageElement = mainPage.selectFirst("img[itemprop=\"image\"]");
        if (imageElement == null) {
            Main.LOG.error(Language.get("message.error.cover_image_getting"));
            return null;
        }
        String coverPath = imageElement.attr("src");
        ImageIcon cover = null;
        try {
            cover = new ImageIcon(ParseManager.resize(ImageIO.read(new URL(coverPath)), 156, 218));
        } catch (IOException e) {
            Main.LOG.error(String.format(Language.get("message.error.cover_image_getting")), e);
        }
        return cover;
    }

    @Override
    public String getHtmlEncodedData(Document mainPage) {
        Element infoElem = mainPage.selectFirst("div.subject-meta");
        if (infoElem == null) {
            Main.LOG.error(Language.get("message.error.title_info_getting"));
            return null;
        }
        Elements ps = infoElem.select("p");
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<h2>").append(mainPage.selectFirst("span.name").text()).append("</h2><br>");
        for (String s : ps.eachText()) {
            html.append(s).append("<br>");
        }
        html.append("</html>");
        return html.toString();
    }

    @Override
    public int getChaptersCount(Document mainPage) {
        Element chapters = mainPage.selectFirst("table.table-hover");
        if (chapters == null) {
            Main.LOG.error(Language.get("message.error.no_chapters"));
            return 0;
        }
        return chapters.select("a").size();
    }

    @Override
    public List<String> getChaptersLocations(Document mainPage) {
        Element tableHoverElement = mainPage.selectFirst("table.table-hover");
        if (tableHoverElement == null) {
            Main.LOG.error(Language.get("message.error.chapters_locations_getting"));
            return null;
        }
        Elements refs = tableHoverElement.select("a");
        return Util.reverseList(refs.eachAttr("href"));
    }

    @Override
    public List<String> getChaptersNames(Document mainPage) {
        Element tableHoverElement = mainPage.selectFirst("table.table-hover");
        if (tableHoverElement == null) {
            Main.LOG.error(Language.get("message.error.chapters_locations_getting"));
            return null;
        }
        Elements refs = tableHoverElement.select("a");
        return Util.reverseList(refs.eachText());
    }

    @Override
    public List<String> getChapterImagesLocations(Document chapterPage) {
        String html = chapterPage.toString();
        String sub = html.substring(html.indexOf("rm_h.init( [['','") + "rm_h.init( [['','".length());
        sub = sub.substring(0, sub.indexOf("]], 0, false);"));
        sub = sub.substring(0, sub.lastIndexOf("\""));
        String[] uris = sub.split("\",[0-9]+,[0-9]+],\\['','");
        for (int i = 0; i < uris.length; i++) {
            uris[i] = uris[i].replace("',\"", "");
        }
        return Arrays.asList(uris);
    }

    @Override
    public void preparePostProcessor(PostDownloadingProcessing postProcessor) {
        postProcessor.setBanner(Language.get("banner.remanga"));
    }
}
