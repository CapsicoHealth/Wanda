
package wanda.saml;

import java.io.File;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.metadata.resolver.impl.FilesystemMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import wanda.web.config.Wanda;


public class ConfigSAML
  {
    protected static final Logger LOG = LogManager.getLogger(ConfigSAML.class.getName());

    public static void init(String configId)
    throws Exception
      {
        File metadataFile = null;
        if (Wanda.getLoginSystem() != null)
          {
            String configFileName = Wanda.getLoginSystem().getSsoConfigFile(configId);
            if (configFileName != null)
              metadataFile = new File(configFileName);
          }
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

        // Initialize OpenSAML
        InitializationService.initialize();

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


    public static void main(String[] args)
    throws Exception
      {
        init("ALV");
      }
  }
