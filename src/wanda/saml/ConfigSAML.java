
package wanda.saml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

import net.shibboleth.shared.xml.impl.BasicParserPool;
import wanda.web.config.SSOConfig;
import wanda.web.config.Wanda;


public class ConfigSAML
  {
    protected static final Logger LOG = LogManager.getLogger(ConfigSAML.class.getName());

    public static void init(String configId)
    throws Exception
      {
        InitializationService.initialize();

        File metadataFile = null;
        SSOConfig conf = Wanda.getSsoConfig(configId);
        if (conf != null || conf._configFile != null)
          metadataFile = new File(conf._configFile);

        if (metadataFile == null)
          {
            LOG.error("No sso configuration can be found in the wanda.config.json file with the id '" + configId + "'.");
            return;
          }

        if (metadataFile.exists() == false)
          {
            LOG.error("The sso configuration '" + configId + "' in the wanda.config.json defines the file '" + metadataFile.getCanonicalPath() + "' which cannot be found.");
            return;
          }

        // Create and configure the ParserPool
        BasicParserPool parserPool = new BasicParserPool();
        parserPool.setNamespaceAware(true);
        parserPool.initialize();

        // Load the metadata XML
        FilesystemMetadataResolver metadataResolver = new FilesystemMetadataResolver(metadataFile);
        metadataResolver.setParserPool(parserPool);
        metadataResolver.setId("metadataResolver");
        metadataResolver.initialize();

        System.out.println("Metadata File Exists: " + metadataFile.exists());
        System.out.println("Metadata Resolver Initialized: " + metadataResolver.isInitialized());

        // Get the EntityDescriptor from the metadata
        Iterator<EntityDescriptor> entityDescriptors = metadataResolver.iterator();
        while (entityDescriptors.hasNext() == true)
          {
            EntityDescriptor entityDescriptor = entityDescriptors.next();
            System.out.println("Resolved Entity ID: " + entityDescriptor.getEntityID());
            IDPSSODescriptor idpSSODescriptor = entityDescriptor.getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
            if (idpSSODescriptor != null)
              {
                String ssoServiceUrl = idpSSODescriptor.getSingleSignOnServices().get(0).getLocation();
                System.out.println("SSO URL: " + ssoServiceUrl);
              }
          }
      }

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

    public static SAML2Client buildSaml2Client(String ssoConfigId, String callbackUrl)
    throws Exception
      {
        LOG.debug("Initializing SAML2 client for '" + ssoConfigId + "'");
        SSOConfig conf = Wanda.getSsoConfig(ssoConfigId);
        if (conf == null)
          {
            LOG.error("Cannot initializing SAML2 client for '" + ssoConfigId + "' because it wasn't fonund in the Wanda configuration information.");
            return null;
          }

        SAML2Configuration config = new SAML2Configuration();
        config.setIdentityProviderMetadataResourceFilepath(conf._configFile);
        config.setServiceProviderEntityId(conf._entityId);
        config.setKeystoreResourceFilepath(conf._keyStorePath);
        config.setKeystorePassword(conf._keyStorePswd);
        config.setPrivateKeyPassword(conf._keyStorePswd);
        config.setKeyStoreAlias("pac4j");
        config.setForceServiceProviderMetadataGeneration(true);
        // config.setServiceProviderMetadataResourceFilepath("file:/absolute/path/to/sp-metadata.xml");
        config.setMaximumAuthenticationLifetime(3600);
        config.setAuthnRequestBindingType("POST");
//        config.setAuthnRequestBindingType(SAMLConstants.SAML2_POST_BINDING_URI);
        config.setAuthnRequestBindingType(SAMLConstants.SAML2_REDIRECT_BINDING_URI);


        SAML2Client saml2Client = new SAML2Client(config);
        saml2Client.setCallbackUrl(callbackUrl);
        saml2Client.init();
        LOG.debug("Completed SAML2 client for '" + ssoConfigId + "'");
        return saml2Client;
      }

    public static void main(String[] args)
    throws Exception
      {
        Wanda.autoInit();
        LOG.info("\n\n\n\n\n\n\n=====================================\n   Init\n=====================================");
        init("ALV");
        LOG.info("\n\n\n\n\n\n\n=====================================\n   buildSaml2Client\n=====================================");
        if (buildSaml2Client("ALV", "/sso") != null)
          LOG.info("SAML2Client created successfully.");
        else
          LOG.error("Failed to create SAML2Client.");
        LOG.info("\n\n\n\n\n\n\n=====================================\n   DONE!!!\n=====================================");
      }
  }
