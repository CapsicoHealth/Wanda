# Web Crawling Service - Next Steps

## The Problem

JavaScript-rendered pages (like `clinicaltrials.gov/study/NCT0397248`) return empty content when crawled with JSoup because the content is rendered client-side. A solution that can execute JavaScript is needed.

---

## Options Summary

| Option | Monthly Cost | Cold Start | Dev Time | Language | License |
|--------|-------------|------------|----------|----------|---------|
| Python + Playwright (Cloud Run) | $0-5 | 2-3s | 2-3 days | Python | Apache 2.0 |
| Crawlee (Node.js) | $0-5 | 3-4s | 3-5 days | JavaScript | Apache 2.0 |
| Rust + Headless Chrome | $0-3 | 1-2s | 1-2 weeks | Rust | MIT/Apache |
| HtmlUnit (Java) | N/A | N/A | 1-2 days | Java | Apache 2.0 |
| Hybrid Sidecar | $0-5 | 2-3s | 3-4 days | Python + Java | Apache 2.0 |

---

## Option 1: Python + Playwright on GCP Cloud Run (RECOMMENDED)

### Why This Option?

1. **Cost Efficiency**
   - Scale to zero = no idle costs
   - Free tier covers most small/medium usage
   - ~$3-10/month for significant usage

2. **Development Speed**
   - FastAPI = 1-2 days to build full service
   - Playwright Python = mature, simple API
   - Many PDF libraries available

3. **Scalability**
   - Cloud Run auto-scales (0 to thousands)
   - No infrastructure management
   - Regional/global deployment

4. **Maintenance**
   - Minimal code to maintain
   - Google handles infrastructure
   - Simple updates

### Tech Stack

- **Framework**: FastAPI (Apache 2.0) - async, fast
- **Browser**: Playwright (Apache 2.0) - modern, reliable
- **PDF**: ReportLab (BSD) or WeasyPrint (BSD)
- **Storage**: google-cloud-storage (Apache 2.0)
- **Container**: ~300-400 MB Docker image

### Cost Analysis (GCP Cloud Run)

```
Pricing Model: Pay-per-request (scale to zero)
- CPU: 1 vCPU 
- Memory: 2 GB (for browser)
- Execution time: ~5-10 seconds per page

Example: 100 crawls/day (avg 5 pages each)
- Requests: 500 page renders/day
- CPU time: 500 x 7 sec = ~1 hour/day
- Cost: ~$0.05-0.10/day (~$3/month)

Free tier: 2 million requests/month, 360,000 GB-seconds/month
Result: Likely FREE for moderate usage
```

### Architecture

```
Java Servlet (Tomcat)
    |
    v
Cloud Run Service (Python/FastAPI)
    |
    v
Playwright (Headless Chromium)
    |
    v
Google Cloud Storage (PDFs, JSON)
    |
    v
Java retrieves files via signed URLs
```

### Deployment Model

- Cloud Run instance runs on internal network only (not public)
- Accessed only by Tomcat-based servlet logic
- Local development option on localhost:8081

### Development Workflow

1. Add features, fix bugs locally
2. Build Docker image
3. Deploy locally for testing
4. Test
5. Deploy to prod (GCP Cloud Run)

---

## Option 2: Crawlee (Node.js)

### What is Crawlee?
Open-source web scraping/crawling framework by Apify (Apache 2.0)

### Pros
- Built-in queue, retries, rate limiting
- Well-documented
- Active community
- Cloud Run compatible

### Cons
- Node.js (different from Java)
- Larger runtime footprint than Python
- Learning curve for Crawlee API

---

## Option 3: Rust + Headless Chrome

### Tech Stack
- Actix-web (Apache 2.0/MIT) - fastest web framework
- chromiumoxide (MIT) or fantoccini (Apache 2.0)
- printpdf (MIT)

### Pros
- Blazing fast (2-3x faster than Python)
- Smallest memory footprint (~150-250 MB container)
- Lowest execution costs
- Best cold start time

### Cons
- Steeper learning curve
- Longer development time
- Less mature browser automation ecosystem
- More complex debugging

---

## Option 4: HtmlUnit (Java - Stay In-Process)

### What is HtmlUnit?
Headless browser for Java that executes JavaScript without needing an actual browser.

### Size and Dependencies
- Core library: ~2.7 MB
- Total with dependencies: ~12-15 MB
- License: Apache 2.0

### Pros
- Pure Java, no external service needed
- No network calls between services
- Simpler deployment

### Cons
- JavaScript engine not perfect (doesn't support all modern JS)
- May not work with complex React/Vue/Angular apps
- Higher memory footprint per request (50-100 MB)
- Development has slowed

---

## Option 5: Hybrid - Java + Python Sidecar

### Architecture
```
Java Servlet 
    |
    +--> JSoup (first attempt)
    |
    +--> Python Sidecar (if JSoup returns empty)
             |
             v
         Playwright (render JS)
             |
             v
         Return rendered HTML to Java
```

### Pros
- Minimal rewrite (keep Java logic)
- Only use sidecar when JSoup fails
- Most gradual migration path

### Cons
- Two services to manage
- Network latency between services

---

## Recommendation

**Option 1 (Python + Playwright on Cloud Run)** is recommended because:

1. Best balance of cost, performance, and development speed
2. Handles all modern JavaScript frameworks
3. Scales automatically with zero idle costs
4. Clean separation of concerns
5. Easy to maintain and update
6. All Apache 2.0 compatible licenses

---

## Next Steps

1. Create Python FastAPI service with Playwright
2. Set up local development environment (Docker)
3. Create deployment scripts for Cloud Run
4. Add Java client wrapper in Wanda to call the service
5. Implement GCS storage and signed URL retrieval
