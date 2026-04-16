package wanda.web.crawl;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class PdfWriter
  {
    private static final Logger LOG = LogManager.getLogger(PdfWriter.class);
    private static final float MARGIN = 50;
    private static final float TITLE_FONT_SIZE = 16;
    private static final float H1_FONT_SIZE = 14;
    private static final float H2_FONT_SIZE = 13;
    private static final float H3_FONT_SIZE = 12;
    private static final float BODY_FONT_SIZE = 10;
    private static final float LINE_SPACING = 1.5f;
    private static final float PARAGRAPH_SPACING = 8f;
    private static final float LIST_INDENT = 20f;
    private static final float MAX_IMAGE_WIDTH = 400;
    private static final float MAX_IMAGE_HEIGHT = 300;

    private static PDFont regularFont;
    private static PDFont boldFont;
    private static PDFont italicFont;
    
    public static List<CrawlResult.PageMapping> writePages(PDDocument doc, java.util.List<PageContent> pages,
                                                            String crawlUrl, int depth, long startTimestamp, 
                                                            long crawlDurationMs, String pdfFileName,
                                                            List<CrawlResult.DownloadedPdf> downloadedPdfs) throws IOException
      {
        regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        List<CrawlResult.PageMapping> pageMapping = new ArrayList<>();
        
        // Step 1: Generate introduction (placeholder for now, will add TOC link later)
        LOG.info("Generating introduction section...");
        int introPageIndex = doc.getNumberOfPages();
        generateIntroductionSection(doc, crawlUrl, depth, startTimestamp, crawlDurationMs, pdfFileName, -1);
        
        // Step 2: Generate Dependent PDFs section (if applicable)
        if (downloadedPdfs != null && !downloadedPdfs.isEmpty())
          {
            LOG.info("Generating Dependent PDFs section...");
            generateDependentPdfsSection(doc, downloadedPdfs);
          }
        int introSectionPages = doc.getNumberOfPages();
        
        // Step 3: Generate all content pages
        int pageNum = 0;
        for (PageContent page : pages)
          {
            pageNum++;
            int startPage = doc.getNumberOfPages();
            
            LOG.info("Processing page {}/{}: {} ({} chars, {} images)", 
                     pageNum, pages.size(), page.url(), 
                     page.contentElement() != null ? page.contentElement().text().length() : 0, 
                     page.imageUrls().size());
            
            addPage(doc, page);
            
            int endPage = doc.getNumberOfPages();
            int pdfPageCount = endPage - startPage;
            
            pageMapping.add(new CrawlResult.PageMapping(page.url(), page.title(), startPage, pdfPageCount));
          }
        
        // Step 4: Generate Table of Contents at the end
        LOG.info("Generating Table of Contents section...");
        int tocStartPage = doc.getNumberOfPages();
        generateTableOfContentsSection(doc, pageMapping);
        
        // Step 5: Now go back and add the TOC link to the introduction page
        addTocLinkToIntroPage(doc, introPageIndex, tocStartPage);
        
        int totalPages = doc.getNumberOfPages();
        LOG.info("PDF generation complete: {} intro/appendix page(s), {} content pages, {} TOC pages, {} total", 
                 introSectionPages, pageMapping.size(), totalPages - tocStartPage, totalPages);
        
        return pageMapping;
      }

    private static void generateIntroductionSection(PDDocument doc, String crawlUrl, int depth,
                                                    long startTimestamp, long crawlDurationMs, String pdfFileName, int tocPageNumber) throws IOException
      {
        float pageWidth = PDRectangle.LETTER.getWidth();
        float pageHeight = PDRectangle.LETTER.getHeight();
        float textWidth = pageWidth - 2 * MARGIN;

        PDPage currentPage = new PDPage(PDRectangle.LETTER);
        doc.addPage(currentPage);
        PDPageContentStream stream = new PDPageContentStream(doc, currentPage);
        float y = pageHeight - MARGIN;

        // Title
        y = writeSectionHeader(stream, "Web Crawl Documentation", MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING;
        
        // Explanatory message
        String crawlTime = java.time.Instant.ofEpochMilli(startTimestamp).toString();
        String duration = tilda.utils.DurationUtil.printDurationConciseFromMs(crawlDurationMs);
        
        String message = String.format(
            "This document was created by crawling %s with a depth of %d on %s. " +
            "The crawl completed in %s. This file was originally named '%s'.",
            crawlUrl, depth, crawlTime, duration, pdfFileName
        );
        
        y = writeWrappedText(stream, message, regularFont, BODY_FONT_SIZE, MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING * 2;
        
        // Note about TOC (link will be added later as annotation)
        String tocMessage = ">> Find the Table of Contents at the end of this document";
        float tocY = y;
        stream.beginText();
        stream.setFont(boldFont, BODY_FONT_SIZE + 1);
        stream.setNonStrokingColor(Color.BLUE);
        stream.newLineAtOffset(MARGIN, y);
        stream.showText(tocMessage);
        stream.endText();
        y -= (BODY_FONT_SIZE + 1) * LINE_SPACING;
        y -= PARAGRAPH_SPACING * 2;
        
        // Crawl details table
        String[][] tableData = {
            {"Master URL:", crawlUrl},
            {"Crawl Depth:", String.valueOf(depth)},
            {"Crawl Date:", crawlTime},
            {"Duration:", duration},
            {"PDF File:", pdfFileName}
        };
        
        y = writeTable(stream, tableData, MARGIN, y, textWidth);
        
        stream.close();
      }

    private static void generateTableOfContentsSection(PDDocument doc, List<CrawlResult.PageMapping> pageMapping) throws IOException
      {
        float pageWidth = PDRectangle.LETTER.getWidth();
        float pageHeight = PDRectangle.LETTER.getHeight();
        float textWidth = pageWidth - 2 * MARGIN;

        PDPage currentPage = new PDPage(PDRectangle.LETTER);
        doc.addPage(currentPage);
        PDPageContentStream stream = new PDPageContentStream(doc, currentPage);
        float y = pageHeight - MARGIN;

        // Table of Contents header
        y = writeSectionHeader(stream, "Table of Contents", MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING;

        String message = String.format("This crawl extracted %d page%s from the target website%s.",
            pageMapping.size(),
            pageMapping.size() == 1 ? "" : "s",
            pageMapping.size() == 1 ? "" : ", listed below");
        
        y = writeWrappedText(stream, message, regularFont, BODY_FONT_SIZE, MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING * 1.5f;

        // List all pages with their page numbers
        for (CrawlResult.PageMapping pm : pageMapping)
          {
            if (y < MARGIN + 30)
              {
                stream.close();
                currentPage = new PDPage(PDRectangle.LETTER);
                doc.addPage(currentPage);
                stream = new PDPageContentStream(doc, currentPage);
                y = pageHeight - MARGIN;
              }

            // Display 1-based page numbers (PDFBox uses 0-based internally)
            String entry = String.format("Page %d: %s", pm.pdfPageStart() + 1, 
                                        pm.title() != null && !pm.title().isEmpty() ? pm.title() : pm.url());
            String[] lines = wrapText(entry, regularFont, BODY_FONT_SIZE, textWidth - 20);
            
            for (int i = 0; i < lines.length; i++)
              {
                if (y < MARGIN + 20)
                  {
                    stream.close();
                    currentPage = new PDPage(PDRectangle.LETTER);
                    doc.addPage(currentPage);
                    stream = new PDPageContentStream(doc, currentPage);
                    y = pageHeight - MARGIN;
                  }
                
                stream.beginText();
                stream.setFont(i == 0 ? regularFont : regularFont, BODY_FONT_SIZE);
                stream.setNonStrokingColor(Color.BLACK);
                stream.newLineAtOffset(MARGIN + (i == 0 ? 0 : 20), y);
                stream.showText(sanitizeText(lines[i], regularFont));
                stream.endText();
                y -= BODY_FONT_SIZE * LINE_SPACING;
              }
            
            // Add URL in smaller text if different from title
            if (pm.title() != null && !pm.title().isEmpty() && !pm.title().equals(pm.url()))
              {
                if (y < MARGIN + 20)
                  {
                    stream.close();
                    currentPage = new PDPage(PDRectangle.LETTER);
                    doc.addPage(currentPage);
                    stream = new PDPageContentStream(doc, currentPage);
                    y = pageHeight - MARGIN;
                  }
                
                String urlText = "   " + pm.url();
                String[] urlLines = wrapText(urlText, regularFont, BODY_FONT_SIZE - 1, textWidth - 20);
                for (String line : urlLines)
                  {
                    if (y < MARGIN + 20)
                      {
                        stream.close();
                        currentPage = new PDPage(PDRectangle.LETTER);
                        doc.addPage(currentPage);
                        stream = new PDPageContentStream(doc, currentPage);
                        y = pageHeight - MARGIN;
                      }
                    
                    stream.beginText();
                    stream.setFont(regularFont, BODY_FONT_SIZE - 1);
                    stream.setNonStrokingColor(new Color(100, 100, 100));
                    stream.newLineAtOffset(MARGIN + 20, y);
                    stream.showText(sanitizeText(line, regularFont));
                    stream.endText();
                    y -= (BODY_FONT_SIZE - 1) * LINE_SPACING;
                  }
              }
            
            y -= 4;
          }

        stream.close();
      }

    private static void generateDependentPdfsSection(PDDocument doc, List<CrawlResult.DownloadedPdf> downloadedPdfs) throws IOException
      {
        float pageWidth = PDRectangle.LETTER.getWidth();
        float pageHeight = PDRectangle.LETTER.getHeight();
        float textWidth = pageWidth - 2 * MARGIN;

        PDPage currentPage = new PDPage(PDRectangle.LETTER);
        doc.addPage(currentPage);
        PDPageContentStream stream = new PDPageContentStream(doc, currentPage);
        float y = pageHeight - MARGIN;

        // Dependent PDFs header
        y = writeSectionHeader(stream, "Dependent PDFs", MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING;
        
        String message = String.format("The following %d PDF document%s found and downloaded during the crawl:",
            downloadedPdfs.size(),
            downloadedPdfs.size() == 1 ? " was" : "s were");
        
        y = writeWrappedText(stream, message, regularFont, BODY_FONT_SIZE, MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING * 1.5f;
        
        for (int i = 0; i < downloadedPdfs.size(); i++)
          {
            CrawlResult.DownloadedPdf pdf = downloadedPdfs.get(i);
            
            if (y < MARGIN + 80)
              {
                stream.close();
                currentPage = new PDPage(PDRectangle.LETTER);
                doc.addPage(currentPage);
                stream = new PDPageContentStream(doc, currentPage);
                y = pageHeight - MARGIN;
              }
            
            // PDF entry number
            stream.beginText();
            stream.setFont(boldFont, BODY_FONT_SIZE);
            stream.setNonStrokingColor(Color.BLACK);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(String.format("PDF %d:", i + 1));
            stream.endText();
            y -= BODY_FONT_SIZE * LINE_SPACING + 4;
            
            // PDF details table
            String fileName = pdf.path().substring(Math.max(pdf.path().lastIndexOf('\\'), pdf.path().lastIndexOf('/')) + 1);
            String durationStr = tilda.utils.DurationUtil.printDurationConciseFromMs(pdf.durationMs());
            String sizeStr = tilda.utils.NumberFormatUtil.printDataSize(pdf.size());
            
            String[][] pdfTable = {
                {"Source URL:", pdf.url()},
                {"Document Title:", pdf.title()},
                {"Saved As:", fileName},
                {"Download Time:", durationStr},
                {"File Size:", sizeStr}
            };
            
            y = writeTable(stream, pdfTable, MARGIN + 10, y, textWidth - 10);
            y -= PARAGRAPH_SPACING;
          }

        stream.close();
      }

    private static void addTocLinkToIntroPage(PDDocument doc, int introPageIndex, int tocPageIndex) throws IOException
      {
        PDPage introPage = doc.getPage(introPageIndex);
        PDPage tocPage = doc.getPage(tocPageIndex);
        
        // Create a link action that goes to the TOC page
        org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo action = 
            new org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo();
        
        // Use XYZ destination to preserve the current zoom level
        org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination dest = 
            new org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination();
        dest.setPage(tocPage);
        dest.setLeft(0);  // Left position on the page
        dest.setTop((int) (PDRectangle.LETTER.getHeight() - 50)); // Near top of page
        dest.setZoom(0); // 0 means "retain current zoom level"
        action.setDestination(dest);
        
        // Create link annotation
        org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink link = 
            new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink();
        link.setAction(action);
        
        // Position the link over the TOC text (approximate location)
        float pageHeight = PDRectangle.LETTER.getHeight();
        float linkY = pageHeight - MARGIN - TITLE_FONT_SIZE * LINE_SPACING - PARAGRAPH_SPACING 
                      - BODY_FONT_SIZE * LINE_SPACING * 3 - PARAGRAPH_SPACING * 2;
        float linkHeight = (BODY_FONT_SIZE + 1) * LINE_SPACING;
        float linkWidth = 450; // Approximate width of the text
        
        org.apache.pdfbox.pdmodel.common.PDRectangle rect = new org.apache.pdfbox.pdmodel.common.PDRectangle();
        rect.setLowerLeftX(MARGIN);
        rect.setLowerLeftY(linkY - linkHeight);
        rect.setUpperRightX(MARGIN + linkWidth);
        rect.setUpperRightY(linkY);
        link.setRectangle(rect);
        
        // Make the link border invisible
        org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary borderStyle = 
            new org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary();
        borderStyle.setWidth(0);
        link.setBorderStyle(borderStyle);
        
        // Add the link to the page
        introPage.getAnnotations().add(link);
        
        LOG.debug("Added TOC link from page {} to page {}", introPageIndex + 1, tocPageIndex + 1);
      }

    private static float writeSectionHeader(PDPageContentStream stream, String title, float x, float y, float maxWidth) throws IOException
      {
        stream.beginText();
        stream.setFont(boldFont, TITLE_FONT_SIZE);
        stream.setNonStrokingColor(Color.BLACK);
        stream.newLineAtOffset(x, y);
        stream.showText(title);
        stream.endText();
        
        y -= TITLE_FONT_SIZE * LINE_SPACING + 5;
        
        stream.setStrokingColor(0.7f, 0.7f, 0.7f);
        stream.moveTo(x, y);
        stream.lineTo(x + maxWidth, y);
        stream.stroke();
        
        return y - PARAGRAPH_SPACING;
      }

    private static float writeWrappedText(PDPageContentStream stream, String text, PDFont font, float fontSize,
                                          float x, float y, float maxWidth) throws IOException
      {
        String[] lines = wrapText(text, font, fontSize, maxWidth);
        for (String line : lines)
          {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(Color.BLACK);
            stream.newLineAtOffset(x, y);
            stream.showText(sanitizeText(line, font));
            stream.endText();
            y -= fontSize * LINE_SPACING;
          }
        return y;
      }

    private static float writeTable(PDPageContentStream stream, String[][] data, float x, float y, float maxWidth) throws IOException
      {
        float colWidth = maxWidth * 0.25f;
        float valueWidth = maxWidth - colWidth;
        
        for (String[] row : data)
          {
            // Label (bold)
            stream.beginText();
            stream.setFont(boldFont, BODY_FONT_SIZE);
            stream.setNonStrokingColor(Color.BLACK);
            stream.newLineAtOffset(x, y);
            stream.showText(sanitizeText(row[0], boldFont));
            stream.endText();
            
            // Value (regular, wrapped)
            String[] valueLines = wrapText(row[1], regularFont, BODY_FONT_SIZE, valueWidth);
            float valueY = y;
            for (String line : valueLines)
              {
                stream.beginText();
                stream.setFont(regularFont, BODY_FONT_SIZE);
                stream.setNonStrokingColor(Color.BLACK);
                stream.newLineAtOffset(x + colWidth, valueY);
                stream.showText(sanitizeText(line, regularFont));
                stream.endText();
                valueY -= BODY_FONT_SIZE * LINE_SPACING;
              }
            
            y = Math.min(y - BODY_FONT_SIZE * LINE_SPACING, valueY);
            y -= 3;
          }
        
        return y;
      }

    private static void addPage(PDDocument doc, PageContent content) throws IOException
      {
        float pageWidth = PDRectangle.LETTER.getWidth();
        float pageHeight = PDRectangle.LETTER.getHeight();
        float textWidth = pageWidth - 2 * MARGIN;

        PDPage pdfPage = new PDPage(PDRectangle.LETTER);
        doc.addPage(pdfPage);

        PDPageContentStream stream = new PDPageContentStream(doc, pdfPage);
        float y = pageHeight - MARGIN;

        // Write title
        y = writeTitle(stream, content.title(), content.url(), MARGIN, y, textWidth);
        y -= PARAGRAPH_SPACING * 2;

        // Process HTML content
        if (content.contentElement() != null)
          {
            Context ctx = new Context(doc, stream, pdfPage, y, textWidth, pageWidth, pageHeight);
            processElement(content.contentElement(), ctx, false);
            y = ctx.y;
            stream = ctx.stream;
            pdfPage = ctx.currentPage;
          }

        stream.close();
      }

    private static float writeTitle(PDPageContentStream stream, String title, String url, float x, float y, float maxWidth) throws IOException
      {
        String displayTitle = (title != null && !title.isEmpty()) ? title : url;
        y = writeText(stream, displayTitle, boldFont, TITLE_FONT_SIZE, x, y, maxWidth, Color.BLACK);
        y -= 10;
        
        stream.setStrokingColor(0.7f, 0.7f, 0.7f);
        stream.moveTo(x, y);
        stream.lineTo(x + maxWidth, y);
        stream.stroke();
        
        return y - 5;
      }

    private static class Context
      {
        PDDocument doc;
        PDPageContentStream stream;
        PDPage currentPage;
        float y;
        float textWidth;
        float pageWidth;
        float pageHeight;
        float indent = 0;

        Context(PDDocument doc, PDPageContentStream stream, PDPage currentPage, float y, float textWidth, float pageWidth, float pageHeight)
          {
            this.doc = doc;
            this.stream = stream;
            this.currentPage = currentPage;
            this.y = y;
            this.textWidth = textWidth;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
          }

        void checkNewPage() throws IOException
          {
            if (y < MARGIN + 50)
              {
                stream.close();
                currentPage = new PDPage(PDRectangle.LETTER);
                doc.addPage(currentPage);
                stream = new PDPageContentStream(doc, currentPage);
                y = pageHeight - MARGIN;
              }
          }
      }

    private static void processElement(Element element, Context ctx, boolean inList) throws IOException
      {
        String tagName = element.tagName().toLowerCase();

        switch (tagName)
          {
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
              ctx.checkNewPage();
              float fontSize = tagName.equals("h1") ? H1_FONT_SIZE : tagName.equals("h2") ? H2_FONT_SIZE : H3_FONT_SIZE;
              ctx.y -= PARAGRAPH_SPACING;
              ctx.y = writeText(ctx.stream, element.text(), boldFont, fontSize, MARGIN + ctx.indent, ctx.y, ctx.textWidth - ctx.indent, Color.BLACK);
              ctx.y -= PARAGRAPH_SPACING;
              break;

            case "p":
              ctx.checkNewPage();
              ctx.y = processParagraph(element, ctx);
              ctx.y -= PARAGRAPH_SPACING;
              break;

            case "ul":
            case "ol":
              ctx.checkNewPage();
              ctx.y -= PARAGRAPH_SPACING / 2;
              float oldIndent = ctx.indent;
              ctx.indent += LIST_INDENT;
              Elements listItems = element.children();
              int itemNum = 1;
              for (Element li : listItems)
                {
                  if (li.tagName().equals("li"))
                    {
                      ctx.checkNewPage();
                      String bullet = tagName.equals("ul") ? "• " : itemNum + ". ";
                      ctx.y = writeText(ctx.stream, bullet + li.text(), regularFont, BODY_FONT_SIZE, 
                                       MARGIN + ctx.indent - LIST_INDENT + 5, ctx.y, ctx.textWidth - ctx.indent + LIST_INDENT - 5, Color.BLACK);
                      ctx.y -= 4;
                      itemNum++;
                    }
                }
              ctx.indent = oldIndent;
              ctx.y -= PARAGRAPH_SPACING / 2;
              break;

            case "table":
              ctx.checkNewPage();
              ctx.y = processTable(element, ctx);
              ctx.y -= PARAGRAPH_SPACING;
              break;

            case "br":
              ctx.y -= BODY_FONT_SIZE * LINE_SPACING;
              break;

            case "img":
              // Images are handled separately
              break;

            default:
              // For divs and other containers, process children
              for (Element child : element.children())
                processElement(child, ctx, inList);
              break;
          }
      }

    private static float processParagraph(Element para, Context ctx) throws IOException
      {
        StringBuilder text = new StringBuilder();
        List<TextRun> runs = new ArrayList<>();
        
        extractTextRuns(para, runs);
        
        if (runs.isEmpty())
          return ctx.y;

        float x = MARGIN + ctx.indent;
        float remainingWidth = ctx.textWidth - ctx.indent;
        
        for (TextRun run : runs)
          {
            float startY = ctx.y;
            ctx.y = writeText(ctx.stream, run.text, run.font, BODY_FONT_SIZE, x, ctx.y, remainingWidth, run.color);
            
            // Add clickable link annotation if this is a link
            if (run.linkUrl != null && !run.linkUrl.isEmpty())
              {
                try
                  {
                    addLinkAnnotation(ctx.doc, ctx.currentPage, run.linkUrl, x, startY, remainingWidth, BODY_FONT_SIZE);
                  }
                catch (Exception e)
                  {
                    LOG.debug("Failed to add link annotation: {}", e.getMessage());
                  }
              }
          }
        
        return ctx.y;
      }

    private static void addLinkAnnotation(PDDocument doc, PDPage page, String url, float x, float y, float width, float height) throws IOException
      {
        org.apache.pdfbox.pdmodel.interactive.action.PDActionURI action = new org.apache.pdfbox.pdmodel.interactive.action.PDActionURI();
        action.setURI(url);
        
        org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink link = new org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink();
        link.setAction(action);
        
        // Create rectangle for the link area (approximate)
        org.apache.pdfbox.pdmodel.common.PDRectangle rect = new org.apache.pdfbox.pdmodel.common.PDRectangle();
        rect.setLowerLeftX(x);
        rect.setLowerLeftY(y - height);
        rect.setUpperRightX(x + width);
        rect.setUpperRightY(y + height);
        link.setRectangle(rect);
        
        // Make the link invisible (no border)
        org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary borderStyle = 
            new org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary();
        borderStyle.setWidth(0);
        link.setBorderStyle(borderStyle);
        
        page.getAnnotations().add(link);
      }

    private static void extractTextRuns(Element element, List<TextRun> runs)
      {
        for (Node node : element.childNodes())
          {
            if (node instanceof TextNode)
              {
                String text = ((TextNode) node).text().trim();
                if (!text.isEmpty())
                  runs.add(new TextRun(text + " ", regularFont, Color.BLACK, null));
              }
            else if (node instanceof Element)
              {
                Element child = (Element) node;
                String tag = child.tagName().toLowerCase();
                String text = child.text().trim();
                
                if (text.isEmpty())
                  continue;

                switch (tag)
                  {
                    case "strong":
                    case "b":
                      runs.add(new TextRun(text + " ", boldFont, Color.BLACK, null));
                      break;
                    case "em":
                    case "i":
                      runs.add(new TextRun(text + " ", italicFont, Color.BLACK, null));
                      break;
                    case "a":
                      String href = child.absUrl("href");
                      runs.add(new TextRun(text + " ", regularFont, Color.BLUE, href.isEmpty() ? null : href));
                      break;
                    case "code":
                      runs.add(new TextRun(text + " ", regularFont, new Color(100, 100, 100), null));
                      break;
                    default:
                      extractTextRuns(child, runs);
                      break;
                  }
              }
          }
      }

    private static class TextRun
      {
        String text;
        PDFont font;
        Color color;
        String linkUrl;

        TextRun(String text, PDFont font, Color color, String linkUrl)
          {
            this.text = text;
            this.font = font;
            this.color = color;
            this.linkUrl = linkUrl;
          }
      }

    private static float processTable(Element table, Context ctx) throws IOException
      {
        Elements rows = table.select("tr");
        
        for (Element row : rows)
          {
            ctx.checkNewPage();
            Elements cells = row.select("th, td");
            StringBuilder rowText = new StringBuilder();
            
            for (int i = 0; i < cells.size(); i++)
              {
                if (i > 0)
                  rowText.append(" | ");
                rowText.append(cells.get(i).text());
              }
            
            PDFont font = row.select("th").size() > 0 ? boldFont : regularFont;
            ctx.y = writeText(ctx.stream, rowText.toString(), font, BODY_FONT_SIZE, 
                            MARGIN + ctx.indent, ctx.y, ctx.textWidth - ctx.indent, Color.BLACK);
            ctx.y -= 4;
          }
        
        return ctx.y;
      }

    private static float writeText(PDPageContentStream stream, String text, PDFont font, float fontSize, 
                                   float x, float y, float maxWidth, Color color) throws IOException
      {
        String[] lines = wrapText(text, font, fontSize, maxWidth);
        
        for (String line : lines)
          {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, y);
            stream.showText(sanitizeText(line, font));
            stream.endText();
            y -= fontSize * LINE_SPACING;
          }
        
        return y;
      }

    private static String[] wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException
      {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words)
          {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(sanitizeText(testLine, font)) / 1000 * fontSize;

            if (width > maxWidth && currentLine.length() > 0)
              {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
              }
            else
              {
                currentLine = new StringBuilder(testLine);
              }
          }

        if (currentLine.length() > 0)
          lines.add(currentLine.toString());

        return lines.toArray(new String[0]);
      }

    private static String sanitizeText(String text, PDFont font)
      {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray())
          {
            try
              {
                font.encode(String.valueOf(c));
                sb.append(c);
              }
            catch (Exception e)
              {
                sb.append(' ');
              }
          }
        return sb.toString();
      }

    private static PDImageXObject loadImage(PDDocument doc, String imageUrl) throws Exception
      {
        java.net.URLConnection conn = new URI(imageUrl).toURL().openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        try (InputStream is = conn.getInputStream())
          {
            byte[] bytes = is.readAllBytes();
            return PDImageXObject.createFromByteArray(doc, bytes, imageUrl);
          }
      }

    private static float[] scaleImage(float width, float height)
      {
        float scale = Math.min(1f, Math.min(MAX_IMAGE_WIDTH / width, MAX_IMAGE_HEIGHT / height));
        return new float[] { width * scale, height * scale };
      }
  }
