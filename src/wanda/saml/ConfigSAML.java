
package wanda.saml;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.pac4j.core.client.Client;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.jee.http.adapter.JEEHttpActionAdapter;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.credentials.SAML2Credentials;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tilda.db.Connection;
import tilda.utils.FileUtil;
import tilda.utils.TextUtil;
import wanda.web.config.SSOConfig;
import wanda.web.config.Wanda;


public class ConfigSAML
  {
    public static final Logger              LOG           = LogManager.getLogger(ConfigSAML.class.getName());

    private static Map<String, SAML2Client> _saml2Configs = new HashMap<String, SAML2Client>();

    private ConfigSAML()
      throws Exception
      {
      }

    public static SAML2Client getInstance(String ssoId)
    throws ServletException
      {
        LOG.debug("Registered configs: " + _saml2Configs.keySet());
        SAML2Client client = _saml2Configs.get(ssoId);
        if (client == null)
          {
            LOG.debug("Config not found for ssoId: " + ssoId + ". Synchronizing the map now.");
            synchronized (_saml2Configs)
              {
                client = _saml2Configs.get(ssoId);
                if (client == null)
                  { // Second check (with locking)
                    LOG.debug("Config still not found for ssoId: " + ssoId + ". Let's create it.");
                    try
                      {
                        InitializationService.initialize();
                        client = ConfigSAML.buildSaml2Client(ssoId);
                        _saml2Configs.put(ssoId, client);
                      }
                    catch (Exception e)
                      {
                        throw new ServletException("Failed to initialize SAML2 configuration", e);
                      }
                  }
                else
                  LOG.debug("Config for ssoId: " + ssoId + " found.");
              }
          }
        else
          LOG.debug("Config for ssoId: " + ssoId + " found.");

        return client;
      }

    protected static SAML2Client buildSaml2Client(String ssoConfigId)
    throws Exception
      {
        LOG.debug("Initializing SAML2 config for '" + ssoConfigId + "'");
        SSOConfig conf = Wanda.getSsoConfig(ssoConfigId);
        if (conf == null)
          {
            LOG.error("Cannot initializing SAML2 client for '" + ssoConfigId + "' because it wasn't fonund in the Wanda configuration information.");
            return null;
          }

        InitializationService.initialize();
        SAML2Configuration config = new SAML2Configuration();
        config.setIdentityProviderMetadataResourceFilepath(conf._identityProviderConfigFile);
        config.setIdentityProviderEntityId(conf._identityProviderEntityId);
        config.setKeystoreResourceFilepath(conf._keyStorePath);
        config.setKeystorePassword(conf._keyStorePswd);
        config.setPrivateKeyPassword(conf._keyStorePswd);
        config.setKeyStoreAlias("pac4j");
        // config.setUseNameQualifier(true); // this breaks the redirect to the SSO partner login page.
        // config.setForceServiceProviderMetadataGeneration(true);
        config.setServiceProviderMetadataResourceFilepath(conf._serviceProviderConfigFile);
        // config.setServiceProviderMetadataResourceFilepath("file:/absolute/path/to/sp-metadata.xml");
        config.setMaximumAuthenticationLifetime(3600);
        // config.setAuthnRequestBindingType(SAMLConstants.SAML2_POST_BINDING_URI); // REDIRECT protocol is preferred
        config.setAuthnRequestBindingType(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        config.setWantsAssertionsSigned(false); // or true if your IdP signs assertions
        // Set index to activate attribute consuming service in metadata
        config.setAttributeConsumingServiceIndex(1);
        config.setMetadataGenerator(new SAMLCustomMetadataGenerator(ssoConfigId));

        LOG.debug("Completed SAML2 config for '" + ssoConfigId + "'"); // \n" + metadata + "\n");

        String callbackUrl = Wanda.getHostName() + Wanda.getAppPath() + conf._redirectUrl;
        SAML2Client samlClient = new SAML2Client(config);
        samlClient.setCallbackUrl(callbackUrl); // safe to call per request
        samlClient.init();
        LOG.debug("Completed SAML2 client for '" + ssoConfigId + "' and callbackUrl '" + callbackUrl + "'."); // \n" + metadata + "\n");

        return samlClient;
      }

    public static void processRedirect(HttpServletRequest req, HttpServletResponse res, String ssoId, String returnUrl)
    throws Exception
      {
        LOG.debug("Getting the SAML2Client for a redirect with callback URL: " + returnUrl);
        SessionStore sessionStore = new JEESessionStore();
        JEEContext context = new JEEContext(req, res);
        CallContext callContext = new CallContext(context, sessionStore);
        sessionStore.set(context, "returnUrl", returnUrl);

        SAML2Client samlClient = ConfigSAML.getInstance(ssoId);
        Optional<RedirectionAction> action = samlClient.getRedirectionAction(callContext);
        if (action.isPresent())
          {
            JEEHttpActionAdapter.INSTANCE.adapt(action.get(), context);
            LOG.debug("SAML2Client SSO Redirect.");
          }
        else
          {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "No redirection action available.");
          }
      }

    public static SAMLUserProfile processCallback(HttpServletRequest req, HttpServletResponse res, Connection C, String ssoId)
    throws Exception
      {
        ConfigSAML.LOG.debug("Processing SAML callback...");
        JEEContext ctx = new JEEContext(req, res);
        SessionStore sessionStore = new JEESessionStore();
        CallContext callContext = new CallContext(ctx, sessionStore);

        SAML2Client samlClient = ConfigSAML.getInstance(ssoId);
        SAML2Credentials credentials = (SAML2Credentials) samlClient.getCredentials(callContext).get();

        Map<String, String> M = processAttributes(credentials);
        String email = M.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        String nameLast = M.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
        String nameFirst = M.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        String orgId = M.get("orgId");

        if (TextUtil.isNullOrEmpty(email) == true)
          {
            LOG.error("Missing email in SAML response.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email in SAML response.");
            return null;
          }
        if (TextUtil.isNullOrEmpty(orgId) == true)
          {
            LOG.error("Missing orgid in SAML response.");
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email in SAML response.");
            return null;
          }
        String returnUrl= (String) sessionStore.get(ctx, "returnUrl").orElse(null);

        return new SAMLUserProfile(email, email, nameFirst, nameLast, orgId, returnUrl);

        /*
         * // Get user profile from the IdP using those credentials
         * Optional<UserProfile> profileOptional = client.getUserProfile(callContext, credentials);
         * if (profileOptional.isEmpty())
         * {
         * LOG.error("No user profile found for the given credentials.");
         * res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No user profile found for the given credentials.");
         * return null;
         * }
         * UserProfile profile = profileOptional.get();
         * LOG.debug("UserProfile class: {}", profile.getClass().getName());
         * LOG.debug("User profile ID: {}", profile.getId());
         * LOG.debug("User profile user name: {}", profile.getUsername());
         * LOG.debug("User profile type id: {}", profile.getTypedId());
         * LOG.debug("User profile linked id: {}", profile.getLinkedId());
         * LOG.debug("User client name: {}", profile.getClientName());
         * LOG.debug("User profile attributes: {}", profile.getAttributes());
         * LOG.debug("User profile roles: {}", profile.getRoles());
         * if (profile instanceof SAML2Profile samlProfile)
         * {
         * samlProfile.getAttributes().forEach((key, value) -> {
         * LOG.debug("Attribute: {} = {}", key, value);
         * });
         * LOG.debug("Roles: {}", samlProfile.getRoles());
         * 
         * // You can also try this if you know expected keys:
         * LOG.debug("email: {}", samlProfile.getAttribute("email"));
         * LOG.debug("givenName: {}", samlProfile.getAttribute("givenName"));
         * }
         * 
         * // Save profile in session
         * sessionStore.set(ctx, Pac4jConstants.USER_PROFILES, List.of(profile));
         */

        // Redirect to the original URL
        // return (String) sessionStore.get(ctx, Pac4jConstants.REQUESTED_URL).orElse("");
      }

    protected static Map<String, String> processAttributes(SAML2Credentials credentials)
      {
        Map<String, String> M = new HashMap<String, String>();
        Object context = credentials.getContext();
        if (context != null)
          {
            try
              {
                java.lang.reflect.Method getMessageContextMethod = context.getClass().getMethod("getMessageContext");
                Object messageContext = getMessageContextMethod.invoke(context);

                java.lang.reflect.Method getMessageMethod = messageContext.getClass().getMethod("getMessage");
                Object message = getMessageMethod.invoke(messageContext);

                if (message instanceof org.opensaml.saml.saml2.core.Response samlResponse)
                  {
                    for (Assertion assertion : samlResponse.getAssertions())
                      {
                        for (AttributeStatement stmt : assertion.getAttributeStatements())
                          {
                            for (Attribute attr : stmt.getAttributes())
                              {
                                String name = attr.getName();
                                List<String> values = new ArrayList<>();
                                for (XMLObject xmlObj : attr.getAttributeValues())
                                  {
                                    String val = xmlObj.getDOM() != null
                                    ? xmlObj.getDOM().getTextContent()
                                    : xmlObj.toString();
                                    values.add(val);
                                  }
                                LOG.debug("Raw SAML Attribute: {} = {}", name, values);
                                if (values.size() > 0)
                                  M.put(name, values.get(0));
                              }
                          }
                      }
                  }
                else
                  {
                    LOG.warn("Expected SAML Response but got: {}", message != null ? message.getClass() : "null");
                  }

              }
            catch (Exception e)
              {
                LOG.error("Error reflecting into SAML credentials context: {}", e.toString(), e);
              }
          }
        else
          {
            LOG.warn("SAML2Credentials.getContext() returned null.");
          }

        return M;
      }


    /*
     * public static void init(String configId)
     * throws Exception
     * {
     * InitializationService.initialize();
     * 
     * File metadataFile = null;
     * SSOConfig conf = Wanda.getSsoConfig(configId);
     * if (conf != null || conf._configFile != null)
     * metadataFile = new File(conf._configFile);
     * 
     * if (metadataFile == null)
     * {
     * LOG.error("No sso configuration can be found in the wanda.config.json file with the id '" + configId + "'.");
     * return;
     * }
     * 
     * if (metadataFile.exists() == false)
     * {
     * LOG.error("The sso configuration '" + configId + "' in the wanda.config.json defines the file '" + metadataFile.getCanonicalPath() + "' which cannot be found.");
     * return;
     * }
     * 
     * // Create and configure the ParserPool
     * BasicParserPool parserPool = new BasicParserPool();
     * parserPool.setNamespaceAware(true);
     * parserPool.initialize();
     * 
     * // Load the metadata XML
     * FilesystemMetadataResolver metadataResolver = new FilesystemMetadataResolver(metadataFile);
     * metadataResolver.setParserPool(parserPool);
     * metadataResolver.setId("metadataResolver");
     * metadataResolver.initialize();
     * 
     * System.out.println("Metadata File Exists: " + metadataFile.exists());
     * System.out.println("Metadata Resolver Initialized: " + metadataResolver.isInitialized());
     * 
     * // Get the EntityDescriptor from the metadata
     * Iterator<EntityDescriptor> entityDescriptors = metadataResolver.iterator();
     * while (entityDescriptors.hasNext() == true)
     * {
     * EntityDescriptor entityDescriptor = entityDescriptors.next();
     * System.out.println("Resolved Entity ID: " + entityDescriptor.getEntityID());
     * IDPSSODescriptor idpSSODescriptor = entityDescriptor.getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
     * if (idpSSODescriptor != null)
     * {
     * String ssoServiceUrl = idpSSODescriptor.getSingleSignOnServices().get(0).getLocation();
     * System.out.println("SSO URL: " + ssoServiceUrl);
     * }
     * }
     * }
     */

    /*
     * public static SAML2Client prepareServiceProviderMetaData(String ssoConfigId)
     * throws Exception
     * {
     * SSOConfig conf = Wanda.getSsoConfig(ssoConfigId);
     * if (conf == null)
     * return null;
     * 
     * // Configure the SAML2 client
     * SAML2Configuration config = new SAML2Configuration();
     * config.setIdentityProviderMetadataResourceFilepath(conf._configFile);
     * config.setServiceProviderEntityId(conf._entityId);
     * config.setKeystoreResourceFilepath(conf._keyStorePath);
     * config.setKeystorePassword(conf._keyStorePswd);
     * config.setPrivateKeyPassword(conf._keyStorePswd);
     * config.setKeyStoreAlias("pac4j");
     * config.setAuthnRequestBindingType(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
     * 
     * SAML2Client saml2Client = new SAML2Client(config);
     * saml2Client.init();
     * 
     * // Generate SP metadata
     * String spMetadata = saml2Client.getServiceProviderMetadataResolver().getMetadata();
     * // Save SP metadata to a file
     * try (FileWriter writer = new FileWriter("xxx?xxx^xxx!sp-metadata.xml"))
     * {
     * writer.write(spMetadata);
     * System.out.println("SP metadata generated and saved to sp-metadata.xml");
     * }
     * catch (IOException e)
     * {
     * e.printStackTrace();
     * }
     * 
     * return saml2Client;
     * }
     */


    public static void main(String[] args)
    throws Exception
      {
        final String ssoId = "ALV";
        Wanda.autoInit();
        LOG.info("\n\n\n\n\n\n\n=====================================\n   Init\n=====================================");
        SAML2Client saml2Client = ConfigSAML.getInstance(ssoId);
        LOG.info("SAML2Client created successfully.");
        // SSOConfig conf = Wanda.getSsoConfig(ssoId);
        // String callbackUrl = Wanda.getHostName() + Wanda.getAppPath() + conf._redirectUrl + "&returnUrl=" + URLEncoder.encode(returnUrl,
        // java.nio.charset.StandardCharsets.UTF_8.toString());
        // saml2Client.setCallbackUrl(callbackUrl);
        saml2Client.init();
        String metadata = saml2Client.getServiceProviderMetadataResolver().getMetadata();
        metadata = FileUtil.prettyPrintXML(metadata);
        PrintWriter out = FileUtil.getBufferedPrintWriter("C:\\Users\\LaurentHasson\\sp_capsico_ALV_metadata.xml", false);
        out.write(metadata);
        out.close();
        LOG.info("\n=====================================\n   DONE!!!\n=====================================\n\n\n\n");
      }

  }
