package com.venky.swf.plugins.collab.util.sms;

import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.routing.Config;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;


public class TwoFProvider extends AbstractSMSProvider{
    public TwoFProvider(String name){
        super(name);
    }

    @Override
    public void sendOtp(String mobile, String otp, boolean fresh) {
        if (!isEnabled()){
            return;
        }
        StringBuilder url = new StringBuilder();
        url.append("https://2factor.in/API/V1/")
                .append(getAuthKey())
                .append("/SMS/")
                .append(sanitizePhoneNumber(sanitizePhoneNumber(mobile)))
                .append("/").append(otp).append("/")
                .append(getTemplateId());

        Call<JSONObject> call = new Call<JSONObject>().method(HttpMethod.GET).url(url.toString());
        JSONObject output =  call.getResponseAsJson();
        if (output == null){
            output = (JSONObject) JSONValue.parse(new InputStreamReader(call.getErrorStream()));
        }
        if (output != null && !ObjectUtil.equals(output.get("Status"), "Success")) {
            if (ObjectUtil.equals(output.get("Details"),"Invalid Phone Number - Check Number Format") && !Config.instance().isDevelopmentEnvironment()){
                MultiException ex = new MultiException("OTP Send Failed");
                ex.add(new RuntimeException(output.toString()));
                throw ex;
            }
        }



    }
}
