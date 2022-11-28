package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class BasicAuthExtension extends ApplicationAuthenticator{
    static {
        Registry.instance().registerExtension(ApplicationUtil.APPLICATION_AUTHENTICATOR_EXTENSION,new BasicAuthExtension());
    }

    @Override
    protected void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder) {
        if (!scheme.equals("basic")) {
            return;
        }
        //String base64Credentials = schemeDetails;
        byte[] credDecoded = Base64.getDecoder().decode(schemeDetails);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);
        // credentials = username:password
        final String[] values = credentials.split(":", 2);
        if (values.length == 2) {
            String appId = values[0];
            String plainPass = values[1];
            Application app = ApplicationUtil.find(appId);
            if (app != null && ObjectUtil.equals(app.getEncryptedSecret(plainPass),app.getSecret())){
                applicationObjectHolder.set(app);
            }
        }

    }
}
