package wanda.saml;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.ServiceName;
import org.pac4j.saml.metadata.SAML2InMemoryMetadataGenerator;

public class SAMLCustomMetadataGenerator extends SAML2InMemoryMetadataGenerator
  {

    private final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

    public SAMLCustomMetadataGenerator(String ssoConfigId)
      {
        super();
        this._ssoConfigId = ssoConfigId;
      }

    protected String _ssoConfigId;

    @Override
    protected SPSSODescriptor buildSPSSODescriptor()
      {
        SPSSODescriptor spDescriptor = super.buildSPSSODescriptor();

        AttributeConsumingService acs = buildAttributeConsumingService();
        spDescriptor.getAttributeConsumingServices().add(acs);

        return spDescriptor;
      }

    private AttributeConsumingService buildAttributeConsumingService()
      {
        AttributeConsumingService acs = (AttributeConsumingService) builderFactory
        .getBuilder(AttributeConsumingService.DEFAULT_ELEMENT_NAME)
        .buildObject(AttributeConsumingService.DEFAULT_ELEMENT_NAME);

        acs.setIndex(1);

        ServiceName serviceName = (ServiceName) builderFactory
        .getBuilder(ServiceName.DEFAULT_ELEMENT_NAME)
        .buildObject(ServiceName.DEFAULT_ELEMENT_NAME);
        serviceName.setValue("Capsico-" + _ssoConfigId + " SP");
        serviceName.setXMLLang("en");
        acs.getNames().add(serviceName);
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/identity/claims/tenantid", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/identity/claims/objectidentifier", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/identity/claims/displayname", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/identity/claims/identityprovider", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/claims/authnmethodsreferences", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", true));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", true));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", true));
        acs.getRequestedAttributes().add(createRequestedAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name", false));
        acs.getRequestedAttributes().add(createRequestedAttribute("orgId", false, false));

        return acs;
      }

    private RequestedAttribute createRequestedAttribute(String name, boolean isRequired)
      {
        return createRequestedAttribute(name, isRequired, true);
      }

    private RequestedAttribute createRequestedAttribute(String name, boolean isRequired, boolean uri)
      {
        RequestedAttribute ra = (RequestedAttribute) builderFactory
        .getBuilder(RequestedAttribute.DEFAULT_ELEMENT_NAME)
        .buildObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        ra.setName(name);
        ra.setNameFormat(uri == true ? "urn:oasis:names:tc:SAML:2.0:attrname-format:uri" : "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
        ra.setIsRequired(isRequired);
        return ra;
      }
  }
