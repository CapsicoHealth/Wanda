package wanda.servlets.helpers;

import java.util.List;

import com.google.gson.Gson;

public class PayPalPreOrder
  {
    public String     id;
    public String     status;
    public List<Link> links;

    public boolean isCreated()
      {
        return "CREATED".equalsIgnoreCase(status);
      }

    public String getApproveHref()
      {
        return getHrefByRel("approve");
      }

    public String getCaptureHref()
      {
        return getHrefByRel("capture");
      }

    public String getSelfHref()
      {
        return getHrefByRel("self");
      }

    private String getHrefByRel(String rel)
      {
        if (links == null)
          return null;
        for (Link l : links)
          {
            if (rel.equalsIgnoreCase(l.rel))
              return l.href;
          }
        return null;
      }

    public static class Link
      {
        public String href;
        public String rel;
        public String method;
      }

    public String toJsonString()
      {
        return new Gson().toJson(this);
      }
  }
