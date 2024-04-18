package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.util.MailUtil;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class EmailImpl<T extends Model & com.venky.swf.plugins.collab.db.model.user.Email> extends OtpEnabledImpl<T> {
    public EmailImpl(){
        super();
    }
    public EmailImpl(T email) {
        super(email);
    }



    public void sendOtp(boolean generateFresh) {
        T email = getProxy();

        if (generateFresh || ObjectUtil.isVoid(email.getLastOtp())){
            email.setLastOtp(OtpEnabled.generateOTP());
        }
        email.setValidated(false);
        email.save();
        MailUtil.getInstance().sendMail(email.getEmail(),"Your verification code.", "Your verification code is : " + email.getLastOtp());
    }



    public String getDomain(){
        Email proxy = getProxy();
        String email = proxy.getEmail();
        if (!ObjectUtil.isVoid(email)) {
            Email.validate(email);
            String[] parts = email.split("@");
            if (parts.length != 2) {
                throw new RuntimeException("Email Validation failed!");
            }
            return parts[1];
        }
        throw new RuntimeException("Email Validation failed!");
    }


}
