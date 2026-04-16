# Web Crawler

A simple web crawler that extracts content from web pages and compiles them into a text-based PDF document.

## Usage

```java
String[] allowedDomains = new String[] { "clinicaltrials.gov", "www.accessdata.fda.gov" };

CrawlResult result = WebCrawler.crawl(
    "https://example.com/article", 
    2, 
    false, 
    "C:/output/crawled.pdf",
    allowedDomains
);

// Access results
System.out.println("Crawled " + result.getPagesCount() + " pages");
System.out.println("Downloaded " + result.getDownloadedPdfs().size() + " PDFs");
System.out.println("Skipped " + result.getSkippedDomains().size() + " domains");
```

### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `url` | String | Starting URL to crawl |
| `depth` | int | Maximum link depth to follow (0 = start page only) |
| `allowExternal` | boolean | If true, follows links to external sites |
| `pdfPath` | String | Full path for output PDF (must not exist) |
| `allowedDomains` | String[] | Additional domains allowed when external=false |

### Return Value

Returns a `CrawlResult` object containing:
- Total pages crawled
- Crawl and PDF generation durations
- Main PDF path and size
- List of downloaded PDFs with sizes and durations
- Map of skipped domains with link counts

## Classes

- **WebCrawler** - Main crawler implementation with static `crawl()` method
- **CrawlResult** - Result object with statistics and lists of downloaded content
- **CrawlFilter** - Link filtering logic (skips ads, navigation, menus)
- **PdfWriter** - PDF generation using PDFBox 3.x
- **PageContent** - Simple record holding URL, title, text, and images
- **CrawlCache** - Utility methods for checking if crawls already exist
- **CrawlServlet** - Example servlet with caching (extends HttpServlet)

## PDF Structure

The generated PDF has the following structure:

1. **Introduction** (1 page) - Crawl metadata with a clickable link to jump to the Table of Contents
2. **Dependent PDFs** (variable, if applicable) - List of PDFs found and downloaded during the crawl
3. **Content Pages** (variable) - All crawled web pages with preserved formatting
4. **Table of Contents** (variable) - Complete list of pages with accurate page numbers

This structure ensures all page numbers are accurate and provides easy navigation via the clickable TOC link.

## Servlet Usage

Deploy `CrawlServlet` and access via:
```
/crawl?url=https://example.com/article&depth=1&allowExternal=false&refresh=false
```

Parameters:
- `url` - URL to crawl (required)
- `depth` - Link depth (default: 1)
- `allowExternal` - Follow external links (default: false)
- `refresh` - Force re-crawl even if cached (default: false)

Configure cache directory in `web.xml`:
```xml
<context-param>
    <param-name>crawlCacheDir</param-name>
    <param-value>/var/cache/crawls</param-value>
</context-param>
```

## Caching Strategy

PDFs act as a file-based cache. Use `CrawlCache` utilities to check for existing crawls:

```java
String pdfPath = "C:/cache/example.pdf";

// Check if crawl exists
if (CrawlCache.exists(pdfPath)) {
    // Check if stale (older than 7 days)
    long sevenDays = 7 * 24 * 60 * 60 * 1000L;
    if (CrawlCache.isStale(pdfPath, sevenDays)) {
        // Re-crawl
        WebCrawlerC4J.crawl(url, depth, allowExternal, pdfPath);
    }
}

// Generate consistent path from URL
String path = CrawlCache.getPath("https://example.com", 2, false);
```

## Features

- BFS (Breadth-First Search) crawling with configurable depth
- Intelligent link filtering (prioritizes content links over navigation/ads)
- Domain whitelist for selective external link following (auto-matches with/without www)
- Tracks and reports skipped domains for whitelist management
- Extracts main content area, removes boilerplate
- Embeds content images (filters out ad/tracking images)
- Downloads linked PDFs (saved as `<base_name>.<pdf_file_name>.pdf`)
- Rich PDF formatting:
  - Crawl Information section with metadata table
  - Table of Contents at the beginning with page numbers
  - Dependent PDFs section with download details
  - Headers (H1-H6) with appropriate sizing
  - Bold, italic, and styled text
  - Bulleted and numbered lists
  - Tables with pipe separators
  - Clickable hyperlinks (blue, underlined)
  - Proper paragraph spacing
  - Page breaks between sections and crawled pages
- Comprehensive statistics tracking (pages, PDFs, sizes, durations)
- JSON metadata export with:
  - PDF page mapping (which URLs correspond to which PDF pages)
    - Note: JSON stores 0-based page indices (page 0 = first page)
    - PDF Table of Contents displays 1-based page numbers (page 1 = first page)
  - Downloaded PDFs with titles and metadata
  - Performance metrics
  - Allowed and skipped domains
  - Timestamps for crawl start/end
