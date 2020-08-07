package wanda.web;

public class JWTHelper
  {
/*
    private static final String ISSUER    = "CapsicoHealth Inc.";
    private static Algorithm    ALGORITHM = null;
    private static JWTVerifier  VERIFIER  = null;

    public static void autoInit()
    throws ServletException
      {
        try
          {
            ALGORITHM = Algorithm.HMAC256(WebBasics.getJWTSecret());
          }
        catch (IllegalArgumentException e)
          {
            e.printStackTrace();
            throw new ServletException();
          }
        catch (UnsupportedEncodingException e)
          {
            e.printStackTrace();
            throw new ServletException();
          }
        VERIFIER = JWT.require(ALGORITHM).withIssuer(ISSUER).build();
      }

    public static class JWTFactory
      {
        private String         signedToken = null;
        private static Builder jwtBuilder  = null;

        public JWTFactory()
          {
            jwtBuilder = JWT.create().withIssuer(ISSUER);
          }

        public void setSessionTenantUser(long tenantRefnum)
          {
            setStringClaim(SessionUtil.Attributes.TENANTUSERREFNUM.toString(), tenantRefnum);
          }

        public void setSessionUser(long userRefnum)
          {
            setStringClaim(SessionUtil.Attributes.USERREFNUM.toString(), userRefnum);
          }

        public void setStringClaim(String name, String value)
          {
            jwtBuilder.withClaim(name, value);
          }

        public void setStringClaim(String name, long value)
          {
            jwtBuilder.withClaim(name, value);
          }

        public String getSignedToken()
          {
            return signedToken;
          }

        public void createToken()
        throws Exception
          {
            ZonedDateTime createdDate = DateTimeUtil.nowUTC();
            ZonedDateTime expiresAt = createdDate.plusMinutes(WebBasics.getForceReLoginMins());
            jwtBuilder.withIssuedAt(Date.from(createdDate.toInstant()));
            jwtBuilder.withExpiresAt(Date.from(expiresAt.toInstant()));
            signedToken = jwtBuilder.sign(ALGORITHM);
          }
      }

    public static class JWTReader
      {
        private DecodedJWT _decodedJWT = null;

        public JWTReader(String token)
          {
            this._decodedJWT = VERIFIER.verify(token);
          }

        public Long getLongClaim(String key)
          {
            Claim c = this._decodedJWT.getClaim(key);
            return c == null ? SystemValues.EVIL_VALUE : c.asLong();
          }

        public String getStringClaim(String key)
          {
            Claim c = this._decodedJWT.getClaim(key);
            return c == null ? null : c.asString();
          }

        public Long getUserRefnum()
          {
            return getLongClaim(SessionUtil.Attributes.USERREFNUM.toString());
          }

        public Long getTenantUserRefnum()
          {
            return getLongClaim(SessionUtil.Attributes.TENANTUSERREFNUM.toString());
          }

      }
*/
  }
