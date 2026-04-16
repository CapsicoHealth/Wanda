package wanda.web.crawl;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

public class CrawlFilter
  {
    private static final Set<String> NAV_CLASSES = Set.of(
        "nav", "navbar", "menu", "sidebar", "footer", "header", "ad", "ads", "advertisement",
        "banner", "social", "share", "comment", "comments", "related", "widget", "popup"
    );

    private static final Set<String> NAV_IDS = Set.of(
        "nav", "navbar", "menu", "sidebar", "footer", "header", "ad", "ads", "advertisement"
    );

    private static final Pattern CONTENT_LINK_PATTERN = Pattern.compile(
        "(?i)(next|previous|prev|continue|read\\s*more|more|page|chapter|part|article|\\d+)"
    );

    private static final Pattern AD_LINK_PATTERN = Pattern.compile(
        "(?i)(click|sponsor|promo|advert|banner|track|analytics|doubleclick|googlesyndication)"
    );

    private static final Set<String> SKIP_EXTENSIONS = Set.of(
        ".css", ".js", ".pdf", ".zip", ".exe", ".mp3", ".mp4", ".avi", ".mov"
    );

    public static boolean isContentLink(Element link, String baseHost, boolean allowExternal)
      {
        String href = link.absUrl("href");
        if (href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:") || href.startsWith("mailto:"))
          return false;

        String lowerHref = href.toLowerCase();
        for (String ext : SKIP_EXTENSIONS)
          if (lowerHref.endsWith(ext))
            return false;

        if (AD_LINK_PATTERN.matcher(href).find())
          return false;

        if (isInNavigationContext(link))
          return false;

        if (!allowExternal)
          {
            try
              {
                String linkHost = new URI(href).getHost();
                if (linkHost != null && !linkHost.equalsIgnoreCase(baseHost))
                  return false;
              }
            catch (Exception e)
              {
                return false;
              }
          }

        String linkText = link.text().trim();
        String rel = link.attr("rel");

        if (rel.contains("next") || rel.contains("prev"))
          return true;

        if (CONTENT_LINK_PATTERN.matcher(linkText).find())
          return true;

        Element parent = link.parent();
        if (parent != null)
          {
            String parentTag = parent.tagName();
            if (parentTag.equals("article") || parentTag.equals("main") || parentTag.equals("section"))
              return true;
          }

        return isInContentArea(link);
      }

    private static boolean isInNavigationContext(Element element)
      {
        Element current = element;
        int depth = 0;
        while (current != null && depth < 10)
          {
            String tag = current.tagName();
            if (tag.equals("nav") || tag.equals("header") || tag.equals("footer") || tag.equals("aside"))
              return true;

            String classes = current.className().toLowerCase();
            String id = current.id().toLowerCase();

            for (String navClass : NAV_CLASSES)
              if (classes.contains(navClass))
                return true;

            for (String navId : NAV_IDS)
              if (id.contains(navId))
                return true;

            current = current.parent();
            depth++;
          }
        return false;
      }

    private static boolean isInContentArea(Element element)
      {
        Element current = element;
        int depth = 0;
        while (current != null && depth < 10)
          {
            String tag = current.tagName();
            if (tag.equals("article") || tag.equals("main"))
              return true;

            String classes = current.className().toLowerCase();
            String id = current.id().toLowerCase();
            if (classes.contains("content") || classes.contains("article") || classes.contains("post") ||
                id.contains("content") || id.contains("article") || id.contains("post"))
              return true;

            current = current.parent();
            depth++;
          }
        return false;
      }

    public static String extractHost(String url)
      {
        try
          {
            return new URI(url).getHost();
          }
        catch (Exception e)
          {
            return "";
          }
      }
  }
