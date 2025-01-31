package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.controller.OidController.OIDProvider;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Arrays;
import java.util.Base64;
import java.util.StringTokenizer;

public class DefaultSocialLoginInfoExtractor implements Extension {
    static {
        Registry.instance().registerExtension(DefaultSocialLoginInfoExtractor.class.getName(),new DefaultSocialLoginInfoExtractor());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Object... context) {
        if (Registry.instance().hasExtensions(SocialLoginInfoExtractor.class.getName())){
            Registry.instance().callExtensions(SocialLoginInfoExtractor.class.getName(), context);
        }else {
            try {
                ObjectHolder<JSONObject> userJsonHolder = (ObjectHolder<JSONObject>) context[2];
                if (context[1] instanceof OAuthAccessTokenResponse){
                    userJsonHolder.set(extractEmail((OIDProvider) context[0],(OAuthAccessTokenResponse) context[1]));
                }else if (context[1] instanceof OAuthResourceResponse) {
                    userJsonHolder.set(extractEmail((OIDProvider) context[0],(OAuthResourceResponse) context[1]));
                }
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

    }

    public JSONObject extractEmail(OIDProvider provider ,OAuthResourceResponse oAuthResponse) throws Exception{
        JSONObject body = (JSONObject) new JSONParser().parse(oAuthResponse.getBody());
        String email =  (String) body.get("email");
        if (ObjectUtil.isVoid(email)){
            try {
                email = (String) ((JSONObject) (((JSONObject) ((JSONArray) body.get("elements")).get(0)).get("handle~"))).get("emailAddress");
                body = new JSONObject();
                body.put("email", email);
            }catch (Exception ex){
                //
            }
        }
        return body;
    }
    public JSONObject extractEmail(OIDProvider provider ,OAuthAccessTokenResponse oAuthResponse) throws Exception{
        if (oAuthResponse instanceof OAuthJSONAccessTokenResponse){
            String idToken = oAuthResponse.getParam("id_token");
            StringTokenizer tk = new StringTokenizer(idToken,".");
            String headerBuf = new String(Base64.getDecoder().decode(tk.nextToken()));
            String bodyBuf = new String(Base64.getDecoder().decode(tk.nextToken()));
            JSONObject header = (JSONObject) new JSONParser().parse(headerBuf);
            JSONObject body = (JSONObject) new JSONParser().parse(bodyBuf);

            String emailId = (String) body.get("email");
            String[] issuers = new String[] {provider.iss(), "http://" + provider.iss() , "https://" + provider.iss() };

            if (body.get("aud").equals(provider.clientId()) && Arrays.asList(issuers).contains(body.get("iss"))) {
                return body;
            }
        }
        throw new RuntimeException("OAuth Failed");

    }
}
