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
