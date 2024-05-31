package wanda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.web.EMailSender;

public class TestMail
  {
    protected static final Logger LOG = LogManager.getLogger(TestMail.class.getName());

    public static void main(String[] args)
      {
        LOG.info("START...");
        EMailSender.sendMailSys(new String[] {"ldh@capsicohealth.com"}, null, null, "Test System Email", "Blah blah blah", true, true);
        EMailSender.sendMailUsr(new String[] {"ldh@capsicohealth.com"}, null, null, "Test User Email", "Blah blah blah", true, true);
        LOG.info("DONE!!!");
      }
  }
