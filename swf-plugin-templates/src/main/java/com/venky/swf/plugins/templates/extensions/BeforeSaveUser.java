package com.venky.swf.plugins.templates.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.templates.db.model.User;
import com.venky.swf.routing.Config;
import org.json.simple.JSONObject;

import java.net.URLEncoder;

public class BeforeSaveUser extends BeforeModelSaveExtension<User> {
    static {
        registerExtension(new BeforeSaveUser());
    }
    @Override
    public void beforeSave(User user) {
        if (ObjectUtil.isVoid(user.getPhoneNumber())){
            return;
        }
        if (user.getRawRecord().isNewRecord() || user.getRawRecord().isFieldDirty("WHATS_APP_NOTIFICATION_ENABLED")){
            JSONObject input = new JSONObject();
            input.put("userid", Config.instance().getProperty("whatsapp.userid"));
            input.put("password",Config.instance().getProperty("whatsapp.password"));
            if (ObjectUtil.isVoid(input.get("userid")) || ObjectUtil.isVoid(input.get("password"))){
                return;
            }
            String whatsAppProviderUrl = Config.instance().getProperty("whatsapp.url");
            input.put("format","json");
            input.put("phone_number",user.getPhoneNumber().substring(1));
            input.put("v",1.1);
            input.put("auth_scheme","plain");
            input.put("channel","WHATSAPP");
            if (user.isWhatsAppNotificationEnabled()){
                input.put("method","OPT_IN");
            }else {
                input.put("method","OPT_OUT");
            }
            Call<JSONObject> call = new Call<JSONObject>().url(whatsAppProviderUrl).inputFormat(InputFormat.FORM_FIELDS).
                    input(input).method(HttpMethod.GET);
            JSONObject response = call.getResponseAsJson();
            if (response != null){
                response = (JSONObject) response.get("response");
            }
            if (call.hasErrors() || (ObjectUtil.equals(response.get("status"),"error"))){
                throw  new RuntimeException("Could not subscribe to whatsapp notifications for " + user.getPhoneNumber());
            }
        }
    }
}
