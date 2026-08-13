/* ===========================================================================
 * Copyright (C) 2024 CapsicoHealth Inc.
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

package wanda.web;

import java.util.Arrays;
import java.util.List;

import tilda.db.Connection;
import tilda.utils.json.JSONPrinter;
import wanda.data.User_Data;

public interface ContentDefinitionService
  {
    public static class ContentDefinition
      {
        public String code;
        public String title;
        public String description;
        public String category;
        public String thumbnailUri;

        public static JSONPrinter toJsonPrinter(ContentDefinition[] contents)
        throws Exception
          {
            return toJsonPrinter(Arrays.asList(contents));
          }
        public static JSONPrinter toJsonPrinter(List<ContentDefinition> contents)
        throws Exception
          {
            JSONPrinter json = new JSONPrinter(true);
            for (ContentDefinition p : contents)
              {
                json.addArrayElementStart();
                json.addElement("code", p.code);
                json.addElement("title", p.title);
                json.addElement("description", p.description);
                json.addElement("category", p.category);
                json.addElement("thumbnailUri", p.thumbnailUri);
                json.addArrayElementClose();
              }
            return json;
          }

        public String toString()
          {
            return "code: " + code + "; title: " + title + "; description: " + description + "; category: " + category + "; thumbnailUri: " + thumbnailUri + ";";
          }
      }

    public ContentDefinition[] getContents(Connection C, User_Data U)
    throws Exception;

  }
