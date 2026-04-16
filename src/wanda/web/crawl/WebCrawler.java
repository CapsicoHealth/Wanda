package wanda.web.crawl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import tilda.utils.DateTimeUtil;
import tilda.utils.DurationUtil;
import tilda.utils.NumberFormatUtil;

public class WebCrawler
  {
    private static final Logger LOG              = LogManager.getLogger(WebCrawler.class);
    private static final int    TIMEOUT_MS       = 10000;
    private static final int    REQUEST_DELAY_MS = 1500;
    private static final String USER_AGENT       = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static CrawlResult crawl(String url, int depth, boolean allowExternal, String pdfPath, String[] allowedDomains)
    throws IOException
      {
        long startMs = System.currentTimeMillis();
        
        File outFile = new File(pdfPath);
        if (outFile.exists())
          {
            int i = pdfPath.lastIndexOf('.');
            String dt = DateTimeUtil.printDate(DateTimeUtil.nowLocalDate());
            pdfPath = pdfPath.substring(0, i) + "." + dt + pdfPath.substring(i);
            outFile = new File(pdfPath);
            if (outFile.exists())
              LOG.info("Overwriting '" + pdfPath + "' with a new crawl.");
            else
              LOG.info("Creating new version of crawl for '" + pdfPath + "'.");
          }

        String baseHost = CrawlFilter.extractHost(url);
        Set<String> allowedDomainsSet = allowedDomains != null ? Set.of(allowedDomains) : Set.of();
        List<String> allowedDomainsList = allowedDomains != null ? List.of(allowedDomains) : List.of();
        
        long crawlStartMs = System.currentTimeMillis();
        CrawlStats stats = crawlPages(url, depth, allowExternal, baseHost, pdfPath, allowedDomainsSet);
        long crawlDurationMs = System.currentTimeMillis() - crawlStartMs;

        if (stats.pages.isEmpty())
          {
            LOG.error("No pages were extracted from crawl. PDF will be empty. Check extraction logic.");
            throw new IOException("No content extracted from " + url);
          }

        LOG.info("Starting PDF generation for {} pages...", stats.pages.size());
        long pdfStartMs = System.currentTimeMillis();
        List<CrawlResult.PageMapping> pageMapping;
        
        // Extract just the filename from the path
        String fileName = pdfPath.substring(pdfPath.lastIndexOf('\\') + 1);
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        
        try (PDDocument doc = new PDDocument())
          {
            LOG.info("Writing pages to PDF...");
            pageMapping = PdfWriter.writePages(doc, stats.pages, url, depth, startMs, 
                                              crawlDurationMs, fileName, stats.downloadedPdfs);
            LOG.info("Saving PDF to {}...", outFile.getAbsolutePath());
            doc.save(outFile);
            LOG.info("PDF saved successfully");
          }
        long pdfDurationMs = System.currentTimeMillis() - pdfStartMs;
        
        long endMs = System.currentTimeMillis();
        long totalDurationMs = endMs - startMs;
        long fileSize = outFile.length();
        
        LOG.info("Crawled {} pages in {}, generated PDF ({}) in {}, total: {}", 
                 stats.pages.size(), 
                 DurationUtil.printDurationConciseFromMs(crawlDurationMs),
                 NumberFormatUtil.printDataSize(fileSize),
                 DurationUtil.printDurationConciseFromMs(pdfDurationMs),
                 DurationUtil.printDurationConciseFromMs(totalDurationMs));

        String jsonPath = pdfPath.substring(0, pdfPath.lastIndexOf('.')) + ".json";
        
        CrawlResult result = new CrawlResult(stats.pages.size(), crawlDurationMs, pdfDurationMs, totalDurationMs,
                               fileSize, pdfPath, stats.downloadedPdfs, stats.skippedDomains, 
                               allowedDomainsList, startMs, endMs, pageMapping);
        
        // Write JSON metadata
        result.writeToJsonFile(jsonPath);
        
        return result;
      }

    private static CrawlStats crawlPages(String startUrl, int maxDepth, boolean allowExternal, String baseHost, String pdfPath, Set<String> allowedDomains)
      {
        CrawlStats stats = new CrawlStats();
        Set<String> visited = new HashSet<>();
        Queue<UrlDepth> queue = new LinkedList<>();
        java.util.Map<String, String> cookies = new java.util.HashMap<>();

        try
          {
            String domain = baseHost;
            LOG.info("Establishing session with {}", domain);
            org.jsoup.Connection.Response response = Jsoup.connect("https://" + domain)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .execute();
            
            cookies.putAll(response.cookies());
            LOG.info("Session established with {} cookies (status: {})", cookies.size(), response.statusCode());
          }
        catch (Exception e)
          {
            LOG.info("Proceeding without session cookies ({})", e.getMessage());
          }

        queue.add(new UrlDepth(startUrl, 0));
        visited.add(normalizeUrl(startUrl));

        while (!queue.isEmpty())
          {
            UrlDepth current = queue.poll();

            try
              {
                LOG.info("Waiting {} ms before crawling {}...", REQUEST_DELAY_MS, current.url);
                Thread.sleep(REQUEST_DELAY_MS);

                Document doc = Jsoup.connect(current.url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .cookies(cookies)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://" + baseHost + "/")
                    .followRedirects(true)
                    .get();

                PageContent page = extractContent(doc, current.url);
                if (page != null && page.contentElement() != null && !page.contentElement().text().isEmpty())
                  {
                    stats.pages.add(page);
                    LOG.info("Extracted page: {} (title: '{}', {} chars, {} images)", 
                             current.url, 
                             page.title(), 
                             page.contentElement().text().length(),
                             page.imageUrls().size());
                  }
                else
                  {
                    LOG.warn("No content extracted from {} (title: '{}', empty: {})", 
                             current.url,
                             page != null ? page.title() : "null",
                             page == null || page.contentElement() == null || page.contentElement().text().isEmpty());
                  }

                Elements links = doc.select("a[href]");
                int pdfCount = 0;
                for (Element link : links)
                  {
                    String href = link.absUrl("href");
                    String normalized = normalizeUrl(href);

                    if (visited.contains(normalized))
                      continue;

                    if (href.toLowerCase().endsWith(".pdf"))
                      {
                        visited.add(normalized);
                        pdfCount++;
                        downloadPdf(href, pdfPath, stats);
                      }
                    else if (current.depth < maxDepth)
                      {
                        String linkHost = CrawlFilter.extractHost(href);
                        boolean isExternal = !linkHost.equalsIgnoreCase(baseHost);
                        boolean isAllowed = false;
                        
                        if (isExternal && !allowExternal)
                          {
                            // Check if domain is in allowed list (with or without www)
                            isAllowed = allowedDomains.contains(linkHost) || 
                                       allowedDomains.contains(linkHost.replaceFirst("^www\\.", "")) ||
                                       allowedDomains.stream().anyMatch(d -> linkHost.equals("www." + d));
                            
                            if (!isAllowed)
                              {
                                stats.skippedDomains.merge(linkHost, 1, Integer::sum);
                                continue;
                              }
                            else
                              {
                                LOG.info("Following allowed external domain: {} -> {}", linkHost, href);
                              }
                          }
                        
                        if (CrawlFilter.isContentLink(link, baseHost, allowExternal || isAllowed))
                          {
                            visited.add(normalized);
                            queue.add(new UrlDepth(href, current.depth + 1));
                            LOG.debug("Queued link: {} (depth {})", href, current.depth + 1);
                          }
                        else
                          {
                            LOG.debug("Filtered out by CrawlFilter: {}", href);
                          }
                      }
                  }
                
                if (pdfCount > 0)
                  LOG.info("Found {} PDF link(s) on {}", pdfCount, current.url);
              }
            catch (Exception e)
              {
                LOG.warn("Failed to crawl {}: {}", current.url, e.getMessage());
              }
          }

        LOG.info("Crawling completed: {} pages collected", stats.pages.size());
        return stats;
      }

    private static class CrawlStats
      {
        List<PageContent> pages = new ArrayList<>();
        List<CrawlResult.DownloadedPdf> downloadedPdfs = new ArrayList<>();
        Map<String, Integer> skippedDomains = new HashMap<>();
      }

    private static void downloadPdf(String pdfUrl, String mainPdfPath, CrawlStats stats)
      {
        try
          {
            LOG.info("Waiting {} ms before downloading {}...", REQUEST_DELAY_MS, pdfUrl);
            Thread.sleep(REQUEST_DELAY_MS);

            String pdfFileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
            if (pdfFileName.toLowerCase().endsWith(".pdf"))
              pdfFileName = pdfFileName.substring(0, pdfFileName.length() - 4);

            String basePath = mainPdfPath.substring(0, mainPdfPath.length() - 4);
            String outputPath = basePath + "." + sanitizeFileName(pdfFileName) + ".pdf";

            File outFile = new File(outputPath);
            if (outFile.exists())
              {
                LOG.debug("PDF already exists: {}", outputPath);
                return;
              }

            long startMs = System.currentTimeMillis();
            
            HttpURLConnection conn = (HttpURLConnection) new URI(pdfUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/pdf,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outFile))
              {
                is.transferTo(fos);
              }

            long durationMs = System.currentTimeMillis() - startMs;
            long fileSize = outFile.length();
            double throughputMBps = (fileSize / 1024.0 / 1024.0) / (durationMs / 1000.0);
            
            // Extract PDF title
            String title = pdfFileName;
            try (PDDocument pdfDoc = org.apache.pdfbox.Loader.loadPDF(outFile))
              {
                if (pdfDoc.getDocumentInformation() != null && pdfDoc.getDocumentInformation().getTitle() != null)
                  title = pdfDoc.getDocumentInformation().getTitle();
              }
            catch (Exception e)
              {
                LOG.debug("Could not extract PDF title from {}: {}", outputPath, e.getMessage());
              }
            
            stats.downloadedPdfs.add(new CrawlResult.DownloadedPdf(pdfUrl, outputPath, title, fileSize, durationMs));
            
            LOG.info("Downloaded PDF: {} ({} in {}, at {}/s)", 
                     outputPath,
                     NumberFormatUtil.printDataSize(fileSize),
                     DurationUtil.printDurationConciseFromMs(durationMs),
                     NumberFormatUtil.printDataSize((long)(throughputMBps * 1024 * 1024)));
          }
        catch (Exception e)
          {
            LOG.warn("Failed to download PDF {}: {}", pdfUrl, e.getMessage());
          }
      }

    private static String sanitizeFileName(String name)
      {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
      }

    private static PageContent extractContent(Document doc, String url)
      {
        String title = doc.title();

        Element main = doc.selectFirst("article, main, [role=main], .content, .post, #content");
        Element contentArea;

        if (main != null)
          {
            main.select("nav, header, footer, aside, .sidebar, .menu, .ad, .advertisement, script, style, noscript").remove();
            contentArea = main;
          }
        else
          {
            doc.select("nav, header, footer, aside, .sidebar, .menu, .ad, .advertisement, script, style, noscript").remove();
            contentArea = doc.body();
          }

        List<String> imageUrls = new ArrayList<>();
        if (contentArea != null)
          {
            Elements images = contentArea.select("img[src]");
            for (Element img : images)
              {
                String src = img.absUrl("src");
                if (!src.isEmpty() && !src.contains("data:") && !isAdImage(src))
                  imageUrls.add(src);
              }
          }

        return new PageContent(url, title, contentArea, imageUrls);
      }

    private static boolean isAdImage(String src)
      {
        String lower = src.toLowerCase();
        return lower.contains("ad") || lower.contains("banner") || lower.contains("sponsor") ||
        lower.contains("pixel") || lower.contains("track") || lower.contains("analytics");
      }

    private static String normalizeUrl(String url)
      {
        if (url == null)
          return "";
        // Remove anchor (#section) and query (?param=value) parts
        String normalized = url.split("#")[0].split("\\?")[0];
        if (normalized.endsWith("/"))
          normalized = normalized.substring(0, normalized.length() - 1);
        return normalized.toLowerCase();
      }

    private record UrlDepth(String url, int depth) { }
  }

