# Tomcat Web Environment Refactoring & Media Optimization Summary

This document summarizes the architectural evaluations, performance diagnostics, and long-term scaling strategies discussed for optimizing an Apache Tomcat environment running on **Ubuntu 26.04 LTS** (4 CPU Cores, 8GB total RAM, 6GB JVM Heap).

---

## 1. Context Evaluation & Path Mapping
* **Multi-Context Architecture:** Splitting text assets (`/static`) from heavy media assets (`/media`) introduces **zero CPU or memory overhead** at the Tomcat routing layer.
* **Admin-Managed Configuration Handoff:** To avoid mixing infrastructure settings with build artifacts (WAR files), configuration parameter overrides can be elegantly separated from the `WEB-INF` structure.

---

## 2. Eliminating the Synchronized Classroom Media Bottleneck
During simultaneous user events (e.g., 100+ concurrent students clicking play at the exact same moment), standard Java thread allocations and heap memory handling for static files create heavy resource strain. 

### The Solution: Non-Blocking Connectors & Zero-Copy Linux Sendfile
By configuring Tomcat to leverage the native OS kernel, files larger than the **48 KiB** break-even threshold bypass Java user-space memory completely.

### Recommended `server.xml` Connector Layout
```xml
<Connector port="8443" 
           protocol="org.apache.coyote.http11.Http11Nio2Protocol" 
           acceptCount="150"
           maxThreads="150" 
           minSpareThreads="20" 
           connectionTimeout="20000" 
           compression="on"
           compressibleMimeType="text/json,application/json,text/javascript,application/javascript,text/html,text/plain,text/xml,text/css,text/csv"
           compressionMinSize="1024" 
           disableUploadTimeout="true" 
           enableLookups="false" 
           maxHttpHeaderSize="8192" 
           SSLEnabled="true" 
           scheme="https" 
           secure="true">

  <!-- Keep useSendfile=false here to allow GZIP compression for dynamic/static text context paths -->
  <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol"
                   compression="on" 
                   compressibleMimeType="text/json,application/json,text/javascript,application/javascript,text/html,text/plain,text/xml,text/css,text/csv"
                   compressionMinSize="1024"
                   useSendfile="false"/>

  <SSLHostConfig>
      <Certificate certificateFile="conf/cert.pem"
                   certificateKeyFile="conf/privkey.pem"
                   certificateChainFile="conf/chain.pem"
                   sslProtocol="TLS"
                   certificateVerification="none"
       />
    </SSLHostConfig>
</Connector>
```

### The `/media` Context Configuration Variant
To instruct the container to explicitly override the connector's sendfile limitation strictly for your media assets directory, use the isolated deployment descriptor method:
* **File Location:** `$CATALINA_BASE/conf/Catalina/localhost/media.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context crossContext="true" docBase="CapsicoWebMedia" reloadable="true" useHttpOnly="true">
    <JarScanner scanClassPath="false"/>
    <Valve className="org.apache.catalina.authenticator.SSLAuthenticator" disableProxyCaching="false"/>
    
    <!-- Programmatically overrides DefaultServlet properties on initialization -->
    <Parameter name="org.apache.catalina.servlets.DefaultServlet.sendfileSize" 
               value="48" 
               override="true"/>
</Context>
```

---

## 3. JVM Service Configuration Tuning (4 Cores / 8GB RAM Node)
To align with a heavily cached, conservative web application architecture without triggering the Linux Out-Of-Memory (OOM) Killer, replace the high-latency `ParallelGC` with the low-pause `G1GC` collector.

### Optimized `tomcat.service` Environment Block
```ini
Environment="CATALINA_OPTS=-Xms6G -Xmx6G -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```
* **Performance Impact:** Fixing boundaries at `6G` prevents runtime heap resizing overhead. The `AlwaysPreTouch` parameter guarantees native memory mappings clear out immediately at startup rather than during heavy execution spikes.

---

## 4. Future Scaling Target: Google Cloud Storage (GCS) Streaming Proxy
When transitioning assets away from local disks to a **private GCP bucket**, you can bypass public access risks and the computational complexity of Signed URLs using a high-efficiency Java Streaming Proxy approach.

### Architectural Breakdown
1. **Security:** Tomcat serves as the gatekeeper, validating standard application session states (`request.getSession()`).
2. **Memory Efficiency:** Using the GCS Client API library, bytes are piped out in standard 8KB chunks, maintaining a near-zero heap memory fingerprint during active playback.
3. **Pre-Compressed Brotli (`.br`) Strategy:**
   * **Brotli** beats GZIP file compression sizes by **15% to 25%** for large JSON/CSV datasets, with rapid native decryption inside modern client web browsers.
   * By pre-compressing text files into `.br` binaries *prior* to bucket upload, the 4-core Tomcat VM performs **zero compression calculations** at runtime. It passes raw bytes straight through while communicating proper headers down to the user client.

```java
// Logic inside an Authenticated Tomcat Proxy Servlet mapping
String acceptEncoding = request.getHeader("Accept-Encoding");

if (acceptEncoding != null && acceptEncoding.contains("br")) {
    response.setHeader("Content-Encoding", "br");
    response.setContentType("application/json"); 
    
    try (InputStream gcsStream = storage.reader(bucketName, "data.json.br").openStream()) {
        gcsStream.transferTo(response.getOutputStream()); // Native low-overhead byte pipe
    }
}
```
