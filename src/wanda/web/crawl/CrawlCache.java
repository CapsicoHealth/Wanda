/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
