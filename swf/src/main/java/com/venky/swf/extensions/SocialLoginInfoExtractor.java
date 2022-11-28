package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Extension;
import com.venky.swf.controller.OidController.OIDProvider;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.json.simple.JSONObject;

public abstract class SocialLoginInfoExtractor implements Extension {


    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Object... context) {
        OIDProvider provider = (OIDProvider) context[0];
        OAuthAccessTokenResponse oAuthAccessTokenResponse ;
        OAuthResourceResponse oAuthResourceResponse ;
        ObjectHolder<JSONObject> userJsonHolder = (ObjectHolder<JSONObject>) context[2];
        if (context[1] instanceof OAuthAccessTokenResponse){
            oAuthAccessTokenResponse = (OAuthAccessTokenResponse) context[1];
            userJsonHolder.set(extractUserInfo(provider,oAuthAccessTokenResponse));
        }else if (context[1] instanceof OAuthResourceResponse){
            oAuthResourceResponse = (OAuthResourceResponse) context[1];
            userJsonHolder.set(extractUserInfo(provider,oAuthResourceResponse));
        }
    }

    public JSONObject extractUserInfo(OIDProvider provider, OAuthAccessTokenResponse accessTokenResponse){
        return new JSONObject();
    }
    public JSONObject extractUserInfo(OIDProvider provider,OAuthResourceResponse resourceResponse){
        return new JSONObject();
    }

}
