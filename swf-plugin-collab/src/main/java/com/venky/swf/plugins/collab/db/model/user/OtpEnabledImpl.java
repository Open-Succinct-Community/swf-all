package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

public abstract class OtpEnabledImpl<T extends OtpEnabled & Model> extends ModelImpl<T> {
    public OtpEnabledImpl() {
    }

    public OtpEnabledImpl(T proxy) {
        super(proxy);
    }

    private String otp = null;
    public String getOtp(){
        return this.otp;
    }
    public void setOtp(String otp){
        this.otp = otp;
    }

    public void resendOtp(){
        sendOtp(Config.instance().getBooleanProperty("swf.otp.refresh.on.resend",false));
    }
    public void sendOtp(){
        sendOtp(true);
    }

    public void validateOtp(String otp) {
        T otpEnabled = getProxy();
        otpEnabled.setValidated(false);
        if (!ObjectUtil.isVoid(otpEnabled.getLastOtp()) ){
            if (ObjectUtil.equals(otpEnabled.getLastOtp(), otp)){
                otpEnabled.setValidated(true);
                otpEnabled.setLastOtp(null);
            }else {
                throw new RuntimeException("Incorrect Otp");
            }
        }else {
            throw new RuntimeException("Otp not generated");
        }
        otpEnabled.save();
    }

    public void validateOtp(){
        validateOtp(getProxy().getOtp());
    }

    public abstract void sendOtp(boolean generateFresh);
}
