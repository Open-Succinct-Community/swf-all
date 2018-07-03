package com.venky.swf.plugins.mail.configuration;

import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.mail.db.model.Mail;
import com.venky.swf.plugins.mail.db.model.SentMail;
import com.venky.swf.sql.Select;

import javax.xml.crypto.Data;
import java.util.List;

public class AppInstaller implements Installer{

  public void install() {
      List<SentMail> mails = new Select().from(SentMail.class).execute();
      for (SentMail mail:mails){
          mail.destroy();
          Mail copy = Database.getTable(Mail.class).newRecord();
          copy.getRawRecord().load(mail.getRawRecord());
          copy.save();
      }
  }
}

