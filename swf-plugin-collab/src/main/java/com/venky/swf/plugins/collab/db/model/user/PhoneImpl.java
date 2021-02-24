package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.collab.util.sms.SMSProviderFactory;

public class PhoneImpl<T extends Model & Phone> extends ModelImpl<T> {
    public PhoneImpl(){
        super();
    }
    public PhoneImpl(T phone){
        super(phone);
    }

    public void resendOtp() {
        sendOtp(false);
    }

    public void sendOtp() {
        sendOtp(true);
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

    public void validateOtp(String otp) {
        T userPhone = getProxy();
        userPhone.setValidated(false);

        if (!ObjectUtil.isVoid(userPhone.getLastOtp())) {
            if (ObjectUtil.equals(userPhone.getLastOtp(), otp)){
                userPhone.setValidated(true);
                userPhone.setLastOtp(null);
            }
        }

        save();
    }
    public void validateOtp(){
        validateOtp(getProxy().getOtp());
    }

    private String otp = null;
    public String getOtp(){
        return this.otp;
    }
    public void setOtp(String otp){
        this.otp = otp;
    }
}
