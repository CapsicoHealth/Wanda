/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wanda.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.web.config.EmailConfigDetails;
import wanda.web.config.WebBasics;

import tilda.utils.MailUtil;
import tilda.utils.TextUtil;

public class EMailSender
  {
    protected static final Logger LOG = LogManager.getLogger(EMailSender.class.getName());

    /**
     * Sends an email using the System email configuration as per WebBasics.conf.json.
     * @param To The list of addresses to send to. If null or empty, the "defaultAdmins" configuration value is used.
     * @param Cc
     * @param Bcc
     * @param Subject
     * @param Message
     * @param Urgent
     * @param Confidential
     * @return
     */
    public static boolean sendMailSys(String[] To, String[] Cc, String[] Bcc, String Subject, String Message, boolean Urgent, boolean Confidential)
      {
        return sendMail(true, To, Cc, Bcc, Subject, Message, Urgent, Confidential);
      }

    /**
     * Sends an email using the User email configuration as per WebBasics.conf.json.
     * @param To The list of addresses to send to. If null or empty, the "defaultAdmins" configuration value is used.
     * @param Cc
     * @param Bcc
     * @param Subject
     * @param Message
     * @param Urgent
     * @param Confidential
     * @return
     */
    public static boolean sendMailUsr(String[] To, String[] Cc, String[] Bcc, String Subject, String Message, boolean Urgent, boolean Confidential)
      {
        return sendMail(false, To, Cc, Bcc, Subject, Message, Urgent, Confidential);
      }

    protected static boolean sendMail(boolean system, String[] To, String[] Cc, String[] Bcc, String Subject, String Message, boolean Urgent, boolean Confidential)
      {
        EmailConfigDetails emailConfig = system == true ? WebBasics.getEmailSettingsSys() : WebBasics.getEmailSettingsUsr();
        if (emailConfig == null)
          {
            LOG.debug("Email component is not operational. No SMTP config set");
            return false;
          }
        String SmtpInfo = emailConfig._smtp;
        String Password = emailConfig._pswd;
        String From = emailConfig._userId;
        return MailUtil.send(SmtpInfo, From, Password, TextUtil.isNullOrEmpty(To) == false ? To : emailConfig._defaultAdmins, Cc, Bcc, Subject, Message, Urgent, Confidential);
      }
  }
