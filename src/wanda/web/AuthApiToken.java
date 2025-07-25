package wanda.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import tilda.utils.HttpStatus;
import tilda.utils.TextUtil;
import wanda.web.config.Wanda;
import wanda.web.exceptions.SimpleServletException;

public class AuthApiToken
  {
    protected static final Pattern _AUTH = Pattern.compile("Bearer\\s+([\\w-]+)\\s+([\\w-]+)");

    protected AuthApiToken(String partnerId)
      {
        _partnerId = partnerId;
      }

    public final String _partnerId;

    public static AuthApiToken getAuthToken(HttpServletRequest req)
    throws Exception
      {
        String authHeader = req.getHeader("Authorization");
        if (TextUtil.isNullOrEmpty(authHeader) == true)
          return null;

        Matcher m = _AUTH.matcher(authHeader);
        if (m.matches() == false)
          throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized request with an invalid Authorization header format: expecting 'Bearer <partnerId> <apiKey>' where <partnerId> and <apiKey> match [\\w-]+.");

        AuthApiToken t = new AuthApiToken(m.group(1));

        if (Wanda.validateApiKey(req, t._partnerId, m.group(2)) == false)
          throw new SimpleServletException(HttpStatus.Unauthorized, "Unauthorized request with an invalid partner ID and/or API Key");

        return t;
      }
  }
