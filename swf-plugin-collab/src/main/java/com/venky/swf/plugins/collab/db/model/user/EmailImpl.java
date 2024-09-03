package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.util.MailUtil;
import com.venky.swf.routing.Config;

import java.util.HashMap;
import java.util.Map;

public class EmailImpl<T extends Model & com.venky.swf.plugins.collab.db.model.user.Email> extends OtpEnabledImpl<T> {
    public EmailImpl(){
        super();
    }
    public EmailImpl(T email) {
        super(email);
    }



    public void sendOtp(boolean generateFresh) {
        T email = getProxy();
        long ttl = Config.instance().getLongProperty("swf.otp.ttl.minutes",10) * 60L * 1000L; //10 minutes.
        long now = System.currentTimeMillis();
        long expiry = email.getUpdatedAt() == null ? now + ttl : email.getUpdatedAt().getTime() + ttl;

        if (generateFresh || ObjectUtil.isVoid(email.getLastOtp()) || email.getUpdatedAt() == null || now > expiry){
            email.setLastOtp(OtpEnabled.generateOTP());
        }
        email.setValidated(false);
        email.save();

        Map<String,String> context  = new HashMap<>(){{
            put("SUBJECT","Your verification code.");
            put("TEXT","Your verification code is : " + email.getLastOtp());
            put("OTP", email.getLastOtp());
            put("EMAIL",email.getEmail());
        }};
        Registry.instance().callExtensions("before.send.email.otp",context);
        MailUtil.getInstance().sendMail(email.getEmail(),context.get("SUBJECT"),context.get("TEXT") );
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
