package wanda.web.crawl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CrawlResult
  {
    private static final Logger LOG = LogManager.getLogger(CrawlResult.class);
    
    private final int pagesCount;
    private final long crawlDurationMs;
    private final long pdfGenerationDurationMs;
    private final long totalDurationMs;
    private final long mainPdfSize;
    private final String mainPdfPath;
    private final List<DownloadedPdf> downloadedPdfs;
    private final Map<String, Integer> skippedDomains;
    private final List<String> allowedDomains;
    private final long startTimestamp;
    private final long endTimestamp;
    private final List<PageMapping> pageMapping;

    public CrawlResult(int pagesCount, long crawlDurationMs, long pdfGenerationDurationMs, long totalDurationMs,
                       long mainPdfSize, String mainPdfPath, List<DownloadedPdf> downloadedPdfs, 
                       Map<String, Integer> skippedDomains, List<String> allowedDomains, 
                       long startTimestamp, long endTimestamp, List<PageMapping> pageMapping)
      {
        this.pagesCount = pagesCount;
        this.crawlDurationMs = crawlDurationMs;
        this.pdfGenerationDurationMs = pdfGenerationDurationMs;
        this.totalDurationMs = totalDurationMs;
        this.mainPdfSize = mainPdfSize;
        this.mainPdfPath = mainPdfPath;
        this.downloadedPdfs = downloadedPdfs;
        this.skippedDomains = skippedDomains;
        this.allowedDomains = allowedDomains;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.pageMapping = pageMapping;
      }

    public int getPagesCount()
      {
        return pagesCount;
      }

    public long getCrawlDurationMs()
      {
        return crawlDurationMs;
      }

    public long getPdfGenerationDurationMs()
      {
        return pdfGenerationDurationMs;
      }

    public long getTotalDurationMs()
      {
        return totalDurationMs;
      }

    public long getMainPdfSize()
      {
        return mainPdfSize;
      }

    public String getMainPdfPath()
      {
        return mainPdfPath;
      }

    public List<DownloadedPdf> getDownloadedPdfs()
      {
        return downloadedPdfs;
      }

    public Map<String, Integer> getSkippedDomains()
      {
        return skippedDomains;
      }

    public List<String> getAllowedDomains()
      {
        return allowedDomains;
      }

    public long getStartTimestamp()
      {
        return startTimestamp;
      }

    public long getEndTimestamp()
      {
        return endTimestamp;
      }

    public List<PageMapping> getPageMapping()
      {
        return pageMapping;
      }

    public long getTotalPdfSize()
      {
        long total = mainPdfSize;
        for (DownloadedPdf pdf : downloadedPdfs)
          total += pdf.size();
        return total;
      }

    public long getTotalPdfDownloadDurationMs()
      {
        long total = 0;
        for (DownloadedPdf pdf : downloadedPdfs)
          total += pdf.durationMs();
        return total;
      }

    public void writeToJsonFile(String jsonOutputPath) throws IOException
      {
        Map<String, Object> json = new LinkedHashMap<>();
        
        // Crawl metadata
        Map<String, Object> crawl = new LinkedHashMap<>();
        crawl.put("startTimestamp", startTimestamp);
        crawl.put("endTimestamp", endTimestamp);
        crawl.put("startTime", java.time.Instant.ofEpochMilli(startTimestamp).toString());
        crawl.put("endTime", java.time.Instant.ofEpochMilli(endTimestamp).toString());
        crawl.put("durationMs", totalDurationMs);
        crawl.put("crawlDurationMs", crawlDurationMs);
        crawl.put("pdfGenerationDurationMs", pdfGenerationDurationMs);
        json.put("crawl", crawl);
        
        // Main PDF
        Map<String, Object> mainPdf = new LinkedHashMap<>();
        mainPdf.put("path", mainPdfPath);
        mainPdf.put("sizeBytes", mainPdfSize);
        mainPdf.put("pageCount", pagesCount);
        json.put("mainPdf", mainPdf);
        
        // Page mapping
        json.put("pageMapping", pageMapping);
        
        // Downloaded PDFs
        json.put("downloadedPdfs", downloadedPdfs);
        
        // Performance metrics
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("totalPagesProcessed", pagesCount);
        performance.put("totalPdfsDownloaded", downloadedPdfs.size());
        performance.put("totalPdfSizeBytes", getTotalPdfSize());
        performance.put("totalPdfDownloadDurationMs", getTotalPdfDownloadDurationMs());
        json.put("performance", performance);
        
        // Allowed domains
        json.put("allowedDomains", allowedDomains);
        
        // Skipped domains
        json.put("skippedDomains", skippedDomains);
        
        // Write to file with pretty printing
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(jsonOutputPath))
          {
            gson.toJson(json, writer);
          }
        
        LOG.info("Metadata saved to {}", jsonOutputPath);
      }

    public static class PageMapping
      {
        private final String url;
        private final String title;
        private final int pdfPageStart;
        private final int pdfPageCount;

        public PageMapping(String url, String title, int pdfPageStart, int pdfPageCount)
          {
            this.url = url;
            this.title = title;
            this.pdfPageStart = pdfPageStart;
            this.pdfPageCount = pdfPageCount;
          }

        public String url()
          {
            return url;
          }

        public String title()
          {
            return title;
          }

        public int pdfPageStart()
          {
            return pdfPageStart;
          }

        public int pdfPageCount()
          {
            return pdfPageCount;
          }
      }

    public static class DownloadedPdf
      {
        private final String url;
        private final String path;
        private final String title;
        private final long size;
        private final long durationMs;

        public DownloadedPdf(String url, String path, String title, long size, long durationMs)
          {
            this.url = url;
            this.path = path;
            this.title = title;
            this.size = size;
            this.durationMs = durationMs;
          }

        public String url()
          {
            return url;
          }

        public String path()
          {
            return path;
          }

        public String title()
          {
            return title;
          }

        public long size()
          {
            return size;
          }

        public long durationMs()
          {
            return durationMs;
          }
      }
  }