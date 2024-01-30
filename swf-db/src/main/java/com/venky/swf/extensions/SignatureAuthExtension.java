package com.venky.swf.extensions;

import com.venky.core.security.Crypt;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationPublicKey;
import com.venky.swf.db.model.application.ApplicationUtil;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class SignatureAuthExtension extends ApplicationAuthenticator{
    static {
        Registry.instance().registerExtension(ApplicationUtil.APPLICATION_AUTHENTICATOR_EXTENSION,new SignatureAuthExtension());
    }
    @Override
    protected void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder) {
        if (!scheme.equals("signature")) {
            return;
        }
        if (applicationObjectHolder.get() != null){
            return;
        }
        Map<String,String> params = ApplicationUtil.extractAuthorizationParams(schemeDetails);
        String keyId = params.get("keyId");
        String appId = null;
        if (keyId.contains("|")) {
            StringTokenizer tokenizer = new StringTokenizer(keyId, "|");
            appId = tokenizer.nextToken();
            keyId = tokenizer.nextToken();
        }

        ApplicationPublicKey applicationPublicKey = ApplicationPublicKey.find(ApplicationPublicKey.PURPOSE_SIGNING,keyId);


        if (applicationPublicKey == null){
            return ;
        }
        Application application = applicationPublicKey.getApplication();
        if (appId != null && !ObjectUtil.equals(appId,application.getAppId())){
            throw new RuntimeException("Cannot sign for a different application");
        }

        String digest = Crypt.getInstance().toBase64(Crypt.getInstance().digest(application.getHashingAlgorithm(), StringUtil.read(payload)));
        if (headers.containsKey("digest")){
            params.putIfAbsent("digest",headers.get("digest"));
        }
        if (!params.containsKey("digest")) {
            params.put("digest", String.format("%s=%s", application.getHashingAlgorithm(), digest));
        }

        String signingString = ApplicationUtil.getSigningString(params);
        if (Crypt.getInstance().verifySignature(signingString,
                params.get("signature"),
                application.getSigningAlgorithm(),
                Crypt.getInstance().getPublicKey(application.getSigningAlgorithm(),applicationPublicKey.getPublicKey()))){
            applicationObjectHolder.set(application);
        }
    }

}
