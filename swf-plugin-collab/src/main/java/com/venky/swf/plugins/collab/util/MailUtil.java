package com.venky.swf.plugins.collab.util;

import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.mail.core.MailerFactory;
import com.venky.swf.plugins.mail.db.model.Mail;
import com.venky.swf.routing.Config;
import org.codemonkey.simplejavamail.Email;

import javax.mail.Message.RecipientType;

public class MailUtil {
    private static MailUtil instance = null;
    private MailUtil(){

    }
    public static MailUtil getInstance(){
        if (instance != null){
            return instance;
        }
        synchronized (MailUtil.class){
            if (instance == null){
                instance = new MailUtil();
            }
        }
        return instance;
    }
    public void sendMail(String toEmailId,
                         String subject, String text){
        TaskManager.instance().executeAsync(new MailTask(toEmailId,toEmailId,subject,text),true);
    }

    public static class MailTask implements Task {
        private String toUserName, toEmailId, subject, text ;
        public MailTask(String toUserName, String toEmailId,
                        String subject, String text){
            this.toUserName = toUserName;
            this.toEmailId = toEmailId;
            this.subject = subject;
            this.text = text;
        }
        public MailTask(){

        }


        @Override
        public void execute() {
            Email monkeyMail = new Email();
            String emailId = Config.instance().getProperty("swf.sendmail.user");
            String protocol = Config.instance().getProperty("swf.sendmail.protocol");
            if (ObjectUtil.isVoid(emailId) || ObjectUtil.isVoid(protocol)) {
                throw new RuntimeException("Plugin not configured :swf.sendmail.user, swf.sendmail.protocol");
            }else {
                String userName = Config.instance().getProperty("swf.sendmail.user.name",emailId);
                Mail mail = Database.getTable(Mail.class).newRecord();
                mail.setEmail(toEmailId);
                mail.setSubject(subject);
                mail.setBody(new StringReader(text));
                mail.save();

                monkeyMail.setFromAddress(userName, emailId);
                monkeyMail.addRecipient(toEmailId, toUserName, RecipientType.TO);
                monkeyMail.setSubject(subject);
                monkeyMail.setText(text);
                MailerFactory.instance().getMailer(protocol).sendMail(monkeyMail);
            }
        }

    }
}
