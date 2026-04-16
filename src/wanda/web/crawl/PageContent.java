package wanda.web.crawl;

import java.util.List;

import org.jsoup.nodes.Element;

public record PageContent(String url, String title, Element contentElement, List<String> imageUrls) { }
