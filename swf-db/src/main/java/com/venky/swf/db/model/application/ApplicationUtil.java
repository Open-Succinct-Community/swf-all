package com.venky.swf.db.model.application;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.db.model.application.api.EventHandler;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationUtil {
    public static final String APPLICATION_AUTHENTICATOR_EXTENSION = "swf.application.authenticator";

    public static Application find(String appId){
        ModelReflector<Application> ref = ModelReflector.instance(Application.class);
        List<Application> applications = new Select().from(Application.class).where(new Expression(ref.getPool(),"APP_ID", Operator.EQ, appId)).execute();
        if (applications.size() != 1){
            return null;
        }
        return applications.get(0);
    }

    public static Map<String, String> extractAuthorizationParams(String authorization) {
        Map<String,String> params = new IgnoreCaseMap<>();

        Matcher matcher = Pattern.compile("([A-z]+)(=)[\"]*([^\",]*)[\"]*[, ]*").matcher(authorization);
        matcher.results().forEach(mr-> params.put(mr.group(1),mr.group(3)));

        return params;
    }


    public static Application find(ByteArrayInputStream is , Map<String, String> headers){
        ObjectHolder<Application> applicationObjectHolder = new ObjectHolder<>(null);
        Registry.instance().callExtensions("swf.application.authenticator",is, headers,applicationObjectHolder);

        Application app = applicationObjectHolder.get();
        if (app ==  null){
            return null;
        }


        String remoteAddress = headers.get("X-Real-IP");
        List<WhiteListIp> ips = app.getWhiteListIps();

        if (!ips.isEmpty() && ips.stream().noneMatch(ip->ObjectUtil.equals(ip.getIpAddress(),remoteAddress))) {
            return null;
        }

        return app;

    }


    public static String getSigningString(Map<String, String> params) {


        String h = params.get("headers");
        StringTokenizer tokenizer = new StringTokenizer(h);
        StringBuilder signingString = new StringBuilder();
        while (tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();
            String header = token.replaceAll("[()]","");

            signingString.append(token).append(": ");
            signingString.append(params.get(header));
            if (tokenizer.hasMoreTokens()){
                signingString.append("\n");
            }
        }
        return signingString.toString();

    }

    public static void addAuthorizationHeader(EventHandler event, Map<String,String> headers, String payload){
        EndPoint endPoint = event.getEndPoint();


        Application app = event.getApplication();
        String  sPrivateKey = getPrivateKey(app.getSigningAlgorithm());
        if (sPrivateKey == null){
            if (!ObjectUtil.isVoid(endPoint.getClientId()) && !ObjectUtil.isVoid(endPoint.getSecret())){
                headers.put("Authorization" ,
                        String.format("Basic %s", Crypt.getInstance().toBase64(String.format("%s:%s",endPoint.getClientId(),endPoint.getSecret()).getBytes(StandardCharsets.UTF_8))));
            }else if (!ObjectUtil.isVoid(endPoint.getTokenName())){
                headers.put(endPoint.getTokenName(),endPoint.getTokenValue());
            }
            return;
        }
        String[] parts = sPrivateKey.split(":");
        String keyId = parts[0];
        sPrivateKey = parts[1];
        PrivateKey privateKey = Crypt.getInstance().getPrivateKey(app.getSigningAlgorithm(),sPrivateKey);


        Map<String,String> dummy = new IgnoreCaseMap<>();
        long now = System.currentTimeMillis();
        String h = app.getHeaders();
        dummy.put("headers",h);
        dummy.put("keyId",keyId);
        String digest =  Crypt.getInstance().toBase64(Crypt.getInstance().digest(app.getHashingAlgorithm(),payload));
        dummy.put("digest",String.format("%s=%s",app.getHashingAlgorithmCommonName(),digest));
        dummy.put("created",Long.toString(now));
        if (app.getSignatureLifeMillis()>0) {
            dummy.put("expires", Long.toString(now + app.getSignatureLifeMillis()));
        }
        dummy.put("algorithm",app.getSigningAlgorithmCommonName());

        String signingString = getSigningString(dummy);
        dummy.put("signature",Crypt.getInstance().generateSignature(signingString,app.getSigningAlgorithm(), privateKey));
        StringBuilder signingHeaders = new StringBuilder();
        for (String k : dummy.keySet()){
            if(signingHeaders.length() > 0){
                signingHeaders.append(",");
            }
            signingHeaders.append(String.format("%s=\"%s\"",k,dummy.get(k)));
        }

        headers.put("Authorization",String.format("Signature %s",signingHeaders));

    }

    private static String getPrivateKey(String signingAlgorithm) {
        if (ObjectUtil.isVoid(signingAlgorithm)){
            return null;
        }
        ObjectHolder<String> keyHolder = new ObjectHolder<>(null);
        Registry.instance().callExtensions(String.format("private.key.get.%s" ,signingAlgorithm),keyHolder);
        return keyHolder.get();
    }





}
