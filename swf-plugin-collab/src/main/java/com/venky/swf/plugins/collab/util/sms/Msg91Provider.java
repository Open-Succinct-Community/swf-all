package com.venky.swf.plugins.collab.util.sms;

import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.routing.Config;
import org.json.simple.JSONObject;

public class Msg91Provider extends AbstractSMSProvider {
    public Msg91Provider(String name){
        super(name);
    }

    @Override
    public void sendOtp(String phoneNumber, String otp,boolean generateFresh) {
        if (!isEnabled()){
            return;
        }

        JSONObject params = new JSONObject();
        params.put("mobile", sanitizePhoneNumber(phoneNumber)); //Don't send + Sign according to the support team !!
        params.put("authkey", getAuthKey());

        String url = null;
        if (generateFresh) {
            url = "https://api.msg91.com/api/v5/otp";
            String templateId = getTemplateId();
            params.put("otp_expiry", getOtpExpiry()); //10 minutes
            params.put("sender", getSenderId());
            params.put("otp",otp);
            params.put("otp_length", otp.length());

            if (!ObjectUtil.isVoid(templateId)){
                params.put("template_id", templateId);
            }
        } else {
            url = "https://api.msg91.com/api/v5/otp/retry";
            params.put("retrytype","text");
        }


            JSONObject output = new Call<JSONObject>().method(HttpMethod.GET).url(url).
                inputFormat(InputFormat.FORM_FIELDS).input(params).getResponseAsJson();

        if (!ObjectUtil.equals(output.get("type"), "success")) {
            MultiException ex = new MultiException("OTP Send Failed");
            ex.add(new RuntimeException(output.toString()));
            throw ex;
        }
    }
}
