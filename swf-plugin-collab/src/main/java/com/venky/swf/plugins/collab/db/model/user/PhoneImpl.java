package com.venky.swf.plugins.collab.db.model.user;

import com.venky.core.date.DateUtils;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.routing.Config;
import org.json.simple.JSONObject;

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

        String key = Config.instance().getProperty("swf.msg91.auth.key");
        String senderId = Config.instance().getProperty("swf.msg91.sender.id");
        String phoneNumber = userPhone.getPhoneNumber();

        if (userPhone.getReflector().isVoid(userPhone.getLastOtp()) || generateFresh){
            userPhone.setLastOtp(OtpEnabled.generateOTP());
        }

        if (!ObjectUtil.isVoid(key) && !ObjectUtil.isVoid(senderId)) {
            JSONObject params = new JSONObject();
            params.put("mobile", phoneNumber);
            params.put("authkey", key);

            String url = null;
            if (generateFresh) {
                url = "https://control.msg91.com/api/sendotp.php";
                params.put("otp_expiry", Config.instance().getIntProperty("swf.msg91.otp.expiry", 10)); //10 minutes
                params.put("sender", senderId);
                params.put("otp",userPhone.getLastOtp());
                params.put("otp_length", userPhone.getLastOtp().length());
            } else {
                url = "https://control.msg91.com/api/retryotp.php";
            }


            JSONObject output = new Call<JSONObject>().method(HttpMethod.POST).url(url).
                    inputFormat(InputFormat.FORM_FIELDS).input(params).getResponseAsJson();

            if (!ObjectUtil.equals(output.get("type"), "success")) {
                MultiException ex = new MultiException("OTP Send Failed");
                ex.add(new RuntimeException(output.toString()));
                throw ex;
            }
        }

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
}
