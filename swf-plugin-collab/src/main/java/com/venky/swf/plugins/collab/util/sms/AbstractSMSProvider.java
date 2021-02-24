package com.venky.swf.plugins.collab.util.sms;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import com.venky.swf.routing.Config;

public abstract class AbstractSMSProvider implements SMSProvider{
    public String getName() {
        return name;
    }

    String name ;
    public AbstractSMSProvider(String providerName){
        this.name = providerName;
    }
    public String getAuthKey(){
        return Config.instance().getProperty(String.format("swf.%s.auth.key",getName()));
    }

    public String getSenderId(){
        return Config.instance().getProperty(String.format("swf.%s.sender.id",getName()));
    }

    public String getTemplateId(){
        return Config.instance().getProperty(String.format("swf.%s.otp.template.id",getName()));
    }
    public int getOtpExpiry(){
        return Config.instance().getIntProperty(String.format("swf.%s.otp.expiry",getName()), 10);
    }

    public boolean isEnabled(){
        return !ObjectUtil.isVoid(getAuthKey()) && !ObjectUtil.isVoid(getSenderId()) && !ObjectUtil.isVoid(getTemplateId());
    }

    public String sanitizePhoneNumber(String phoneNumber){
        return Phone.sanitizePhoneNumber(phoneNumber).substring(1);
    }
}
