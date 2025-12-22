package wanda.servlets.helpers;

import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;
import tilda.utils.EncryptionUtil;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONUtil;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.TenantUser_Data;
import wanda.data.TenantUser_Factory;
import wanda.data.TenantView_Data;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SessionUtil;
import wanda.web.config.EulaActivation;
import wanda.web.config.Wanda;

public class EulaHelper
  {
    protected static final Logger LOG = LogManager.getLogger(LoginHelper.class.getName());


    /**
     * Handles the EULA Process. Typically, it proceeds as follows:
     * <UL>
     * <LI>Check if the user needs EULA</LI>
     * <LI>Get the EULA information and return to the client</LI>
     * <LI>Client displays the EULA and gets the user's acceptance</LI>
     * <LI>Client sends back the acceptance token which we validate</LI>
     * <LI>We mark the user and tenantUser as having accepted the EULA</LI>
     * </UL>
     * To implement this logic we do the following:
     * <UL>
     * <LI>We check first if we are already in the process of accepting a EULA since it's the simplest check. We see
     * if the request includes the 'eulaToken' attribute. If so, we check it against the session value set up in the
     * prior step chronologically. If it matches, we mark the user and tenantUser as having accepted the EULA and return
     * true. Otherwise, we throw an error.</LI>
     * <LI>Next, we check if the user needs a EULA. If not, we return true to indicate the process is complete.</LI>
     * <LI>If the user needs a EULA, we generate a new token, store it in the session and return the EULA information to the client
     * so that the user can be prompted to accept it.
     * </LI>
     * </UL>
     * 
     * @param out
     * @param req
     * @param C
     * @param TenantUserRefnum
     * @param eulaUrl
     * @param U
     * @return
     * @throws Exception
     */
    public static boolean doEula(RequestUtil req, ResponseUtil res, Connection C, User_Data U, TenantView_Data TV)
    throws Exception
      {
        LOG.debug("Eula testing");

        // Let's check first if a EULA process is already underway.
        String eulaCode = req.getSessionString(SessionUtil.Attributes.EULA_CODE.toString());
        if (TextUtil.isNullOrEmpty(eulaCode) == false) // in the process of doing a EULA
          {
            LOG.debug("Eula Login");
            String eulaToken = req.getParamString("eulaToken", false);
            int accept = req.getParamInt("accept", false);

            if (eulaCode.equals(eulaToken) == false)
              {
                req.addError("eulaToken", "is Invalid");
                req.throwIfErrors();
              }
            if (accept != 1)
              {
                req.addError("accept", "You must accept the EULA before continuing.");
                req.throwIfErrors();
              }

            clearUserForEula(C, req, U, TV, true);
            return true; // All good!
          }

        // Next, let's check if the user needs a EULA. This could be because it's their first time logging in, or
        // the system configuration requires a EULA to be re-signed based on a timeout configuration value.
        String eulaUrl = needsEula(C, U, TV);
        if (TextUtil.isNullOrEmpty(eulaUrl) == true)
          {
            LOG.debug("Eula not needed");
            clearUserForEula(C, req, U, TV, true);
            return true; // No EULA needed, all good!
          }


        // Generate a secret token to validate the EULA acceptance step.
        LOG.debug("Eula required");
        eulaCode = EncryptionUtil.getToken(18, true);
        req.setSessionString(SessionUtil.Attributes.EULA_CODE.toString(), eulaCode);
        // force a client-side EULA acceptance.
        PrintWriter out = res.setContentType(ResponseUtil.ContentType.JSON);
        JSONUtil.startOK(out, '{');
        // JSONUtil.print(Out, "appData", true, U.getAppDataJson(Email + "@@" + Pswd));
        JSONUtil.print(out, "eulaUrl", true, eulaUrl);
        JSONUtil.print(out, "eulaToken", false, eulaCode);
        if (TV != null)
         JSONUtil.print(out, "tenantUserRefnum", false, TV.getTenantUserRefnum());
        JSONUtil.end(out, '}');
        return false;
      }


    /**
     * If the user comes from a promo code, we check if the promo code has an EULA URL and if the last EULA
     * was signed more than the renewal days defined in the promo code. If so, we return the EULA URL to be
     * displayed to the user.<BR>
     * If the user doesn't come from a promo code, we check if there is a default EULA defined in the
     * Wanda configuration and if the last EULA was signed more than the renewal days defined there.
     * 
     * @param C
     * @param EA
     * @return
     * @throws Exception
     */
    protected static String needsEula(Connection C, User_Data U, TenantView_Data TV)
    throws Exception
      {
        String promo = U.getPromoCode();
        if (TextUtil.isNullOrEmpty(promo) == false)
          {
            Promo_Data p = Promo_Factory.lookupByCode(promo);
            // Promo record must exist, and Eula URL must be defined, and either the user never signed a EULA or the last EULA is older than the renewal days.
            if (p.read(C) == true && p.isNullEulaUrl() == false && (U.isNullLastEula() == true || p.getEulaRenewalDays() > 0 && DateTimeUtil.computeDaysToNow(U.getLastEula()) > p.getEulaRenewalDays()))
              return p.getEulaUrl();
          }

        // Let's check the global scope
        EulaActivation EA = Wanda.getEula(TV == null ? null : TV.getName());
        if (EA != null)
          {
            int days = DateTimeUtil.computeDaysToNow(U.getLastEula());
            if (days < 0 || days > EA._renewalDays)
              return EA._eulaUrl;
          }

        return null;
      }


    protected static void clearUserForEula(Connection C, RequestUtil Req, User_Data U, TenantView_Data TV, boolean refreshTS)
    throws Exception
      {
        if (refreshTS == true)
          {
            U.setLastEulaNow();
            if (TV != null)
              {
                TenantUser_Data TU = TenantUser_Factory.lookupByUserTenant(U.getRefnum(), TV.getRefnum());
                if (TU.read(C) == false)
                  throw new Exception("Cannot find TenantUser record");
                TU.setLastEulaNow();
                if (TU.write(C) == false)
                  throw new Exception("Cannot update TenantUser refnum " + TU.getRefnum());
              }
          }
        Req.removeSessionAttribute(SessionUtil.Attributes.EULA_CODE.toString());
        Req.setSessionInt(SessionUtil.Attributes.EULA_CLEAR.toString(), 1);
        LOG.debug("Eula Login completed");
      }


  }
