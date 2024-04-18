package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.collab.util.sms.SMSProviderFactory;

public class PhoneImpl<T extends Model & Phone> extends OtpEnabledImpl<T> {
    public PhoneImpl(){
        super();
    }
    public PhoneImpl(T phone){
        super(phone);
    }

    public void sendOtp(boolean generateFresh) {
        T userPhone = getProxy();

        if (userPhone.getReflector().isVoid(userPhone.getLastOtp()) || generateFresh){
            userPhone.setLastOtp(OtpEnabled.generateOTP());
            generateFresh = true;
        }

        SMSProviderFactory.getInstance().getDefaultProvider().sendOtp(userPhone.getPhoneNumber(),userPhone.getLastOtp(),generateFresh);
        userPhone.setValidated(false);
        userPhone.save();
    }

}
