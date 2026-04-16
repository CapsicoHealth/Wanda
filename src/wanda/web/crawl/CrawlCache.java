package wanda.web.crawl;

import java.io.File;

public class CrawlCache
  {
    public static boolean exists(String pdfPath)
      {
        return new File(pdfPath).exists();
      }

    public static long getAge(String pdfPath)
      {
        File f = new File(pdfPath);
        if (!f.exists())
          return -1;
        return System.currentTimeMillis() - f.lastModified();
      }

    public static boolean isStale(String pdfPath, long maxAgeMillis)
      {
        long age = getAge(pdfPath);
        return age < 0 || age > maxAgeMillis;
      }

    public static String getPath(String url, int depth, boolean allowExternal)
      {
        String sanitized = url.replaceAll("[^a-zA-Z0-9]", "_");
        if (sanitized.length() > 100)
          sanitized = sanitized.substring(0, 100);
        return sanitized + "_d" + depth + (allowExternal ? "_ext" : "") + ".pdf";
      }
  }
