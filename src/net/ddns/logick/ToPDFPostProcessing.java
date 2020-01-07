package net.ddns.logick;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ToPDFPostProcessing implements PostDownloadingProcessing {

    private int divisionCounter;
    private File output;
    private String prefix;
    private String banner;

    public ToPDFPostProcessing(int divisionCounter, File output, String prefix) {
        this.divisionCounter = divisionCounter;
        this.output = output;
        this.prefix = prefix;
    }

    @Override
    public void postProcess(BufferedImage[] images, TreeMap<Integer, String> tableOfContents) {
        PDDocument[] docs = new PDDocument[tableOfContents.size()];
        PDDocumentOutline[] outlines = new PDDocumentOutline[tableOfContents.size()];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = new PDDocument();
            outlines[i] = new PDDocumentOutline();
            docs[i].getDocumentCatalog().setDocumentOutline(outlines[i]);
        }
        createPages(images, docs, tableOfContents);
        addOutlines(outlines, tableOfContents);
        Main.increaseAndUpdateMainProgressBarState(Language.get("message.status.writing"));
        if (divisionCounter == 1) {
            try {
                Files.deleteIfExists(Paths.get(output.getPath()));
                Files.createFile(Paths.get(output.getPath()));
                docs[0].save(output);
                docs[0].close();
            } catch (IOException e) {
                Main.LOG.error(String.format(Language.get("message.error.filesystem")), e);
            } catch (SecurityException e) {
                Main.LOG.error(Language.get("message.error.filesystem_access"), e);
            }
            Main.LOG.info(Language.get("message.info.written_file"));
            Main.increaseAndUpdateSecondaryProgressBarState(Language.get("message.info.written_file"));
        } else {
            Main.initialiseSecondaryProgressBar(docs.length, "");
            if (!output.exists()) {
                output.mkdir();
            }
            for (int i = 0; i < docs.length; i++) {
                File f = new File(output.getAbsolutePath() + File.separator + String.format(Language.get("file.name"), prefix, i + 1));
                try {
                    Files.deleteIfExists(Paths.get(f.getPath()));
                    Files.createFile(Paths.get(f.getPath()));
                } catch (SecurityException e) {
                    Main.LOG.error(Language.get("message.error.filesystem_access"), e);
                } catch (IOException e) {
                    Main.LOG.error(Language.get("message.error.filesystem"), e);
                }
                try {
                    docs[i].save(f);
                    docs[i].close();
                } catch (IOException e) {
                    Main.LOG.error(Language.get("message.error.filesystem"), e);
                }
                Main.LOG.info(String.format(Language.get("message.info.written_part"), i + 1));
                Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.info.written_part"), i + 1));
                if (Main.isInterrupted) {
                    return;
                }
            }
        }
        Main.completedOnProgressBar();
        System.gc();
    }

    private void addOutlines(PDDocumentOutline[] outlines, TreeMap<Integer, String> tableOfContents) {
        Main.increaseAndUpdateMainProgressBarState(Language.get("message.status.creating_contents"));
        Main.initialiseSecondaryProgressBar(tableOfContents.size(), "");
        Iterator<Map.Entry<Integer, String>> tableOfContentsIterator = tableOfContents.entrySet().iterator();
        int currentDoc = 0;
        int countedPages = 0;
        int currentChapter = 0;
        while (tableOfContentsIterator.hasNext()) {
            Map.Entry<Integer, String> entry = tableOfContentsIterator.next();
            if (currentDoc != currentChapter * divisionCounter / tableOfContents.size()) {
                currentDoc++;
                countedPages = entry.getKey();
            }
            PDOutlineItem outline = new PDOutlineItem();
            outline.setTitle(entry.getValue());
            PDPageXYZDestination dest = new PDPageXYZDestination();
            dest.setLeft(0);
            dest.setLeft(0);
            dest.setPageNumber(entry.getKey() - countedPages);
            outline.setDestination(dest);
            outlines[currentDoc].addLast(outline);
            Main.LOG.info(String.format(Language.get("message.info.outline"), currentChapter + 1, tableOfContents.size()));
            Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.info.outline"), currentChapter + 1, tableOfContents.size()));
            currentChapter++;
        }
    }

    private void createPages(BufferedImage[] images, PDDocument[] docs, TreeMap<Integer, String> tableOfContents) {
        PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
        PDFont font = null;
        try {
//            font = PDType0Font.load(docs[0], Main.class.getResourceAsStream("times.ttf"));
            font = PDFontFactory.createDefaultFont();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (font == null) {
            Main.LOG.error(Language.get("message.error.font_loading"));
            banner = "";
        }
        int pos = 2;
        float fontSize = 14f;
        float width = 0;
        if (!banner.isEmpty())
            try {
                width = font.getStringWidth(banner) / 1000 * fontSize;
            } catch (IOException e) {
                Main.LOG.error(Language.get("message.error.font_width"), e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Main.LOG.error(Language.get("message.error.unsupported_banner_character"), e);
            }
        int currentDoc = 0;
        int currentChapter = 0;
        int nextChapter = tableOfContents.size() > 1 ? tableOfContents.ceilingKey(tableOfContents.firstKey() + 1) : Integer.MAX_VALUE;
        for (int i = 0; i < images.length; i++) {
            if (i == nextChapter) {
                currentChapter++;
                nextChapter = tableOfContents.size() > 1 ? tableOfContents.ceilingKey(i + 1) : currentChapter;
                currentDoc = currentChapter * divisionCounter / tableOfContents.size();
            }
            BufferedImage bi = images[i];
            PDPage page = new PDPage();
            page.setCropBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
            page.setMediaBox(new PDRectangle(bi.getWidth(), bi.getHeight()));
            PDImageXObject pdImage;
            try {
                pdImage = LosslessFactory.createFromImage(docs[currentDoc], bi);
                PDPageContentStream contents = new PDPageContentStream(docs[currentDoc], page);
                contents.drawImage(pdImage, 0, 0);
                if (!banner.isEmpty()) {
                    contents.setGraphicsStateParameters(graphicsState);
                    contents.setFont(font, fontSize);
                    contents.setNonStrokingColor(Color.WHITE);
                    contents.addRect(pos - 1, pos - 1, width + 3, fontSize + 1);
                    contents.fillAndStrokeEvenOdd();
                    contents.beginText();
                    contents.setNonStrokingColor(Color.BLACK);
                    contents.newLineAtOffset(pos, pos + 2);
                    contents.showText(banner);
                    contents.endText();
                }
                contents.close();
            } catch (IOException e) {
                Main.LOG.error(String.format(Language.get("message.error.image_drawing")), e);
            }
            docs[currentDoc].addPage(page);
            Main.LOG.info(String.format(Language.get("message.info.page_added"), i + 1, images.length));
            Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.info.page_added"), i + 1, images.length));
            if (Main.isInterrupted) {
                return;
            }
        }
    }


    @Override
    public void setBanner(String banner) {
        this.banner = banner;
    }
}
