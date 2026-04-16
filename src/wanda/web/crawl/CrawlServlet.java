package wanda.web.crawl;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tilda.utils.HttpStatus;
import tilda.utils.ParseUtil;

/**
 * Example servlet demonstrating web crawling with file-based caching.
 * 
 * Usage: /crawl?url=https://example.com&depth=1&allowExternal=false&refresh=false
 */
public class CrawlServlet extends HttpServlet
  {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogManager.getLogger(CrawlServlet.class);
    private static final long MAX_CACHE_AGE = 7 * 24 * 60 * 60 * 1000L; // 7 days

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
      {
        String url = req.getParameter("url");
        int depth = ParseUtil.parseInteger(req.getParameter("depth"), 1);
        boolean allowExternal = ParseUtil.parseBoolean(req.getParameter("allowExternal"), false);
        boolean forceRefresh = ParseUtil.parseBoolean(req.getParameter("refresh"), false);

        if (url == null || url.isEmpty())
          {
            resp.sendError(HttpStatus.BadRequest._Code, "Missing 'url' parameter");
            return;
          }

        String cacheDir = getServletContext().getInitParameter("crawlCacheDir");
        if (cacheDir == null)
          cacheDir = System.getProperty("java.io.tmpdir") + "/crawl-cache";

        new File(cacheDir).mkdirs();

        String fileName = CrawlCache.getPath(url, depth, allowExternal);
        String pdfPath = cacheDir + "/" + fileName;

        try
          {
            if (!forceRefresh && CrawlCache.exists(pdfPath))
              {
                if (!CrawlCache.isStale(pdfPath, MAX_CACHE_AGE))
                  {
                    LOG.info("Serving cached crawl: {}", pdfPath);
                    sendFile(resp, new File(pdfPath), "application/pdf", fileName);
                    return;
                  }
                else
                  {
                    LOG.info("Cache stale, re-crawling: {}", url);
                    new File(pdfPath).delete();
                  }
              }

            LOG.info("Crawling: {} (depth={}, external={})", url, depth, allowExternal);
            WebCrawler.crawl(url, depth, allowExternal, pdfPath, new String[0]);

            sendFile(resp, new File(pdfPath), "application/pdf", fileName);
          }
        catch (Exception e)
          {
            LOG.error("Failed to crawl {}: {}", url, e.getMessage(), e);
            resp.sendError(HttpStatus.InternalServerError._Code, "Crawl failed: " + e.getMessage());
          }
      }

    private static void sendFile(HttpServletResponse resp, File f, String contentType, String fileName) throws Exception
      {
        if (!f.exists())
          throw new IOException("File not found: " + f.getAbsolutePath());
        
        resp.setContentType(contentType);
        resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        org.apache.commons.io.FileUtils.copyFile(f, resp.getOutputStream());
      }
  }
