package wanda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import wanda.web.SystemMailSender;

public class TestMail
  {
    protected static final Logger LOG = LogManager.getLogger(TestMail.class.getName());

    public static void main(String[] args)
      {
        LOG.info("START...");
        SystemMailSender.sendMail(new String[] {"ldh@capsicohealth.com"}, null, null, "Test System Email", "Blah blah blah", true, true);
        LOG.info("DONE!!!");
      }
  }
