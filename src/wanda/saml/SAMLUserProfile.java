package wanda.saml;

public class SAMLUserProfile
  {
    public SAMLUserProfile(String id, String email, String nameFirst, String nameLast, String orgId, String returnUrl)
      {
        _id = id;
        _email = email;
        _nameFirst = nameFirst;
        _nameLast = nameLast;
        _orgId = orgId;
        _returnUrl = returnUrl;
      }

    public final String _id;
    public final String _email;
    public final String _nameFirst;
    public final String _nameLast;
    public final String _orgId;
    public final String _returnUrl;
  }
