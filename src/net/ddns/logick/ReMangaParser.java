package net.ddns.logick;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ReMangaParser implements Parser {

    @Override
    public ImageIcon getCoverImage(Document mainPage) {
        Element imageElement = mainPage.selectFirst("meta[itemprop=image]");
        if (imageElement == null) {
            Main.LOG.error(/*todo main page image parsing error message*/"");
            return null;
        }
        String coverPath = imageElement.attr("content");
        ImageIcon cover = null;
        try {
            cover = new ImageIcon(ParseManager.resize(ImageIO.read(new URL(coverPath)), 156, 218));
        } catch (IOException e) {
            e.printStackTrace();
            Main.LOG.error(String.format(Language.get("message.cover_image_getting_error"), e.getMessage()), e);
        }
        return cover;
    }

    @Override
    public String getHtmlEncodedData(Document mainPage) {
        Element infoElement = mainPage.selectFirst("div.info");
        if (infoElement == null) {
            Main.LOG.error(/*todo main page info parsing error message*/"");
            return null;
        }
        String[] html = infoElement.wholeText().split("\n");
        Element alternativeNameElement = mainPage.selectFirst("meta[itemprop=alternativeHeadline]");
        if (alternativeNameElement == null) {
            Main.LOG.error(/*todo main page alternative name parsing error message*/"");
            return null;
        }
        StringBuilder sb = new StringBuilder("<html>" + alternativeNameElement.attr("content"));
        for (String s :
                html) {
            if (s.isEmpty()) continue;
            sb.append(s.trim()).append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    public int getChaptersCount(Document mainPage) {
        return mainPage.select("div.chapter").size();
    }

    @Override
    public List<String> getChaptersLocations(Document mainPage) {
        Elements chapters = mainPage.select("a.name");
        return Util.reverseList(chapters.eachAttr("href"));
    }

    @Override
    public List<String> getChaptersNames(Document mainPage) {
        Elements chapters = mainPage.select("a.name");
        return Util.reverseList(chapters.eachText());
    }

    @Override
    public List<String> getChapterImagesLocations(Document chapterPage) {
        return chapterPage.select("img.owl-lazy").eachAttr("data-src");
    }

    @Override
    public void preparePostProcessor(PostDownloadingProcessing postProcessor) {
        postProcessor.setBanner(Language.get("banner.remanga"));
    }

    private List<String> getImgRefs(String chapterURI, JTextArea logTextArea) {
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
