package com.venky.swf.extensions;

import com.venky.core.security.Crypt;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectHolder;
import com.venky.extension.Registry;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationPublicKey;
import com.venky.swf.db.model.application.ApplicationUtil;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class SignatureAuthExtension extends ApplicationAuthenticator{
    static {
        Registry.instance().registerExtension(ApplicationUtil.APPLICATION_AUTHENTICATOR_EXTENSION,new SignatureAuthExtension());
    }
    @Override
    protected void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder) {
        if (!scheme.equals("signature")) {
            return;
        }
        Map<String,String> params = ApplicationUtil.extractAuthorizationParams(schemeDetails);

        ApplicationPublicKey applicationPublicKey = ApplicationPublicKey.find(params.get("keyId"));
        if (applicationPublicKey == null){
            return ;
        }
        Application application = applicationPublicKey.getApplication();

        String digest = Crypt.getInstance().toBase64(Crypt.getInstance().digest(application.getHashingAlgorithm(), StringUtil.read(payload)));
        if (headers.containsKey("digest")){
            params.putIfAbsent("digest",headers.get("digest"));
        }
        if (!params.containsKey("digest")) {
            params.put("digest", String.format("%s=%s", application.getHashingAlgorithmCommonName(), digest));
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
