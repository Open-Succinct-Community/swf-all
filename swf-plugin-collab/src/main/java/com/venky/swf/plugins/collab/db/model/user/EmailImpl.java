package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.mail.core.MailerFactory;
import com.venky.swf.routing.Config;
import org.codemonkey.simplejavamail.Email;

import javax.mail.Message.RecipientType;
import java.util.logging.Level;

public class EmailImpl<T extends Model & com.venky.swf.plugins.collab.db.model.user.Email> extends ModelImpl<T> {
    public EmailImpl(){
        super();
    }
    public EmailImpl(T email) {
        super(email);
    }


    public void resendOtp(){
        sendOtp(false);
    }
    public void sendOtp(){
        sendOtp(true);
    }
    public void sendOtp(boolean generateFresh) {
        T email = getProxy();

        if (generateFresh || ObjectUtil.isVoid(email.getLastOtp())){
            email.setLastOtp(OtpEnabled.generateOTP());
        }
        email.setValidated(false);
        email.save();

        Email monkeyMail = new Email();
        String emailId = Config.instance().getProperty("swf.sendmail.user");
        String userName = Config.instance().getProperty("swf.sendmail.user.name");

        if (ObjectUtil.isVoid(emailId)) {
            Config.instance().getLogger(getModelClass().getSimpleName()).log(Level.WARNING, "Plugin not configured :swf.sendmail.user");
        }else {
            monkeyMail.setFromAddress(userName, emailId);
            monkeyMail.addRecipient(emailId, emailId, RecipientType.TO);
            monkeyMail.setSubject("Your verification code.");
            monkeyMail.setText("Your verification code is : " + email.getLastOtp());
            MailerFactory.instance().getMailer(MailerFactory.PROTOCOL_SMTP).sendMail(monkeyMail);
        }
    }

    public void validateOtp(String otp) {
        T email = getProxy();
        if (!ObjectUtil.equals(email.getLastOtp(), otp)) {
            email.setValidated(false);
        } else {
            email.setValidated(true);
            email.setLastOtp(null);
        }
        email.save();
    }
}
