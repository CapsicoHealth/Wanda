package wanda.web.crawl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.utils.DurationUtil;
import tilda.utils.NumberFormatUtil;

public class WebCrawlerTest
  {
    private static final Logger LOG = LogManager.getLogger(WebCrawler.class);

    public static void main(String[] args) throws IOException
      {
        // Note: domains are matched with or without "www." prefix automatically
        String[] allowedDomains = new String[] { 
            "clinicaltrials.gov",
            "accessdata.fda.gov", 
            "fda.gov",
            "aacihealthcare.com", 
            "thelancet.com" 
        };
        
        CrawlResult result;
        
        LOG.info("\n\n\n\n#######################################################################\nStarting crawling...");
        result = WebCrawler.crawl(
            "https://www.drugs.com/lutetium-lu-177-dotatate.html",
            1,
            false,
            "C:\\projects\\crawler\\lutetium-lu-177-dotatate.pdf",
            allowedDomains
        );
        printReport(result);
        
//        LOG.info("\n\n\n\n#######################################################################\nStarting crawling...");
//        result = WebCrawler.crawl(
//            "https://clinicaltrials.gov/study/NCT0397248",
//            1,
//            false,
//            "C:\\projects\\crawler\\NCT0397248.pdf",
//            allowedDomains
//        );
//        printReport(result);

        LOG.info("Crawling completed.");
        
      }

    private static void printReport(CrawlResult result)
      {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CRAWL REPORT");
        System.out.println("=".repeat(80));
        
        System.out.println("\n--- SUMMARY ---");
        System.out.println("Total pages crawled:     " + result.getPagesCount());
        System.out.println("Total crawl time:        " + DurationUtil.printDurationConciseFromMs(result.getCrawlDurationMs()));
        System.out.println("PDF generation time:     " + DurationUtil.printDurationConciseFromMs(result.getPdfGenerationDurationMs()));
        System.out.println("Total time:              " + DurationUtil.printDurationConciseFromMs(result.getTotalDurationMs()));
        
        System.out.println("\n--- MAIN PDF ---");
        System.out.println("Path:  " + result.getMainPdfPath());
        System.out.println("Size:  " + NumberFormatUtil.printDataSize(result.getMainPdfSize()));
        
        if (!result.getDownloadedPdfs().isEmpty())
          {
            System.out.println("\n--- DOWNLOADED PDFs (" + result.getDownloadedPdfs().size() + ") ---");
            long totalPdfSize = 0;
            long totalPdfTime = 0;
            for (CrawlResult.DownloadedPdf pdf : result.getDownloadedPdfs())
              {
                System.out.println("  " + pdf.path());
                System.out.println("    URL:      " + pdf.url());
                System.out.println("    Size:     " + NumberFormatUtil.printDataSize(pdf.size()));
                System.out.println("    Duration: " + DurationUtil.printDurationConciseFromMs(pdf.durationMs()));
                totalPdfSize += pdf.size();
                totalPdfTime += pdf.durationMs();
              }
            System.out.println("  ---");
            System.out.println("  Total size:     " + NumberFormatUtil.printDataSize(totalPdfSize));
            System.out.println("  Total duration: " + DurationUtil.printDurationConciseFromMs(totalPdfTime));
          }
        
        System.out.println("\n--- OVERALL TOTALS ---");
        System.out.println("Total PDFs:  " + (1 + result.getDownloadedPdfs().size()) + " (" + NumberFormatUtil.printDataSize(result.getTotalPdfSize()) + ")");
        
        if (!result.getSkippedDomains().isEmpty())
          {
            System.out.println("\n--- SKIPPED DOMAINS (" + result.getSkippedDomains().size() + ") ---");
            result.getSkippedDomains().entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(e -> System.out.println("  " + e.getKey() + " (" + e.getValue() + " links)"));
            
            System.out.println("\nTo allow these domains, add them to the allowedDomains array:");
            System.out.println("  String[] allowedDomains = new String[] {");
            result.getSkippedDomains().keySet().stream()
                .sorted()
                .forEach(domain -> System.out.println("    \"" + domain + "\","));
            System.out.println("  };");
          }
        
        System.out.println("\n" + "=".repeat(80));
      }
  }
