package com.venky.swf.controller;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Grant;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import org.eclipse.jetty.server.Session;
import org.json.simple.JSONObject;
import org.owasp.encoder.Encode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OauthController extends Controller {
    public OauthController(Path path) {
        super(path);
    }

    /*
    OAuth Flow
        External web app redirects user to authorize
        authorize -> login -> loginSuccessful -> oauth/authorization_success -> app's redirect_uri -> token -> returns token to be passed as bearer token to all apis.
        
        
     */
    @RequireLogin(false)
    public View authorize(){
        return login();
    }
    private void prepareSession(){
        Session session = getPath().getSession();
        if (session != null) {
            session.setAttribute("autoInvalidate", false);
        }
    }
    @RequireLogin(false)
    public View authorization_success(){
        Map<String,Object> fields = getPath().getFormFields();
        String redirect_uri = (String)fields.get("redirect_uri");

        StringBuilder queryParams = new StringBuilder();
        if (ObjectUtil.isVoid(redirect_uri)){
            throw new AccessDeniedException("Don't know where to redirect");
        }
        queryParams.append(redirect_uri);
        if (redirect_uri.contains("?")){
            queryParams.append("&");
        }else{
            queryParams.append("?");
        }

        fields.forEach((k,v)->{
            if ( k.equals("_LOGIN") || k.equals("_REGISTER") || k.equals("error") || k.equals("redirect_uri")){
                return;
            }
            if (!queryParams.isEmpty()){
                queryParams.append("&");
            }
            queryParams.append(k);
            queryParams.append("=");
            queryParams.append(Encode.forUriComponent(v.toString()));
        });

        prepareSession();
        return new RedirectorView(getPath(),queryParams.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String loginSuccessful() {
        prepareSession();
        if (getFormFields().containsKey("redirect_uri")){
            StringBuilder queryParams = new StringBuilder();
            queryParams.append("/oauth/authorization_success?");
            JSONObject object = new JSONObject();
            getPath().getFormFields().forEach((k,v)->{
                if ("name".equals(k) || k.startsWith("password") || k.equals("_LOGIN") || k.equals("_REGISTER") || k.equals("error")){
                    return;
                }
                object.put(k,Encode.forUriComponent(v.toString()));
                if ("code_challenge".equals(k) ){
                    return;
                }
                if (!queryParams.isEmpty()){
                    queryParams.append("&");
                }
                queryParams.append(k);
                queryParams.append("=");
                queryParams.append(object.get(k));
            });
            object.put("code",generateCode());
            queryParams.append(String.format("&code=%s",object.get("code")));
            storeGrant(object);
            return queryParams.toString();
        }else {
            return super.loginSuccessful();
        }
    }

    static final long TOKEN_LIFE = 10L * 24L * 60L * 60L * 1000L; // 10 Days!!

    @RequireLogin(false)
    public View token(){
        getPath().getHeaders().put("Accept","application/json");
        if ( HttpMethod.valueOf(getPath().getRequest().getMethod().toUpperCase()) != HttpMethod.POST){
            throw new AccessDeniedException("Invalid method");
        }
        prepareSession();
        Map<String,Object> fields = getPath().getFormFields();
        Grant grant = null;
        if (ObjectUtil.equals(fields.get("grant_type"),"authorization_code")){
            grant = createAccessCodeGrant(fields);
        }else if(ObjectUtil.equals(fields.get("grant_type"),"refresh_token")){
            grant = refreshGrant(fields);
        }

        IntegrationAdaptor<Grant,JSONObject> integrationAdaptor = IntegrationAdaptor.instance(Grant.class,JSONObject.class);
        JSONObject out = new JSONObject();
        ModelIOFactory.getWriter(Grant.class, JSONObject.class).write(grant,out,grant.getReflector().getVisibleFields(new ArrayList<>()));
        FormatHelper.instance(out).change_key_case(KeyCase.CAMEL,KeyCase.SNAKE);

        View view = new BytesView(getPath(),out.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON,"Cache-Control","no-store");
        return view;
    }

    private Grant refreshGrant(Map<String, Object> fields) {
        Grant grant = Database.getTable(Grant.class).newRecord();
        grant.setRefreshToken((String)fields.get("refresh_token"));
        grant = Database.getTable(Grant.class).find(grant,false);
        if (grant == null){
            throw new AccessDeniedException("Bad Refresh Token");
        }
        grant.setAccessToken(Crypt.getInstance().toBase64(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        grant.setAccessTokenExpiry(System.currentTimeMillis()+TOKEN_LIFE);
        grant.save();
        return grant;
    }

    private Grant createAccessCodeGrant(Map<String, Object> fields) {
        String code = (String)fields.get("code");
        String code_verifier = (String)fields.get("code_verifier");
        String redirect_uri = (String)fields.get("redirect_uri");
        JSONObject grant = retrieveGrant(code);
        if (grant == null){
            throw new AccessDeniedException("Auth code has expired");
        }
        if (!ObjectUtil.equals(Encode.forUriComponent(redirect_uri),grant.get("redirect_uri"))){
            throw new AccessDeniedException("redirect_uri not same as the one auth code was granted for.");
        }
        if (ObjectUtil.equals("S256",grant.get("code_challenge_method") )) {
            String code_challenge = Crypt.getInstance().toBase64(Crypt.getInstance().digest("SHA-256",code_verifier));
            if (!ObjectUtil.equals(code_challenge,grant.get("code_challenge"))){
                throw new AccessDeniedException("Challenge resolution failure");
            }
        }
        Grant newGrant = Database.getTable(Grant.class).newRecord();
        newGrant.setApplicationId(getPath().getApplication().getId());
        newGrant.setUserId((Long)grant.get("id"));
        newGrant = Database.getTable(Grant.class).getRefreshed(newGrant);
        if (newGrant.getRawRecord().isNewRecord() || newGrant.getAccessTokenExpiry() < System.currentTimeMillis()){
            newGrant.setAccessToken(Crypt.getInstance().toBase64(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            newGrant.setAccessTokenExpiry(System.currentTimeMillis() + TOKEN_LIFE);
            if (newGrant.getRawRecord().isNewRecord()) {
                newGrant.setRefreshToken(Crypt.getInstance().toBase64(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
            }
            newGrant.save();
        }

        return newGrant;
    }

    private String generateCode(){
        return UUID.randomUUID().toString();
    }


    private static Map<String,JSONObject> grants = Collections.synchronizedMap(new HashMap<>());
    private static ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public void storeGrant(JSONObject grant){
        long life = 10*60*1000L;
        grant.put("expiry", System.currentTimeMillis()+life);
        grant.put("id",getPath().getSessionUserId());
        grants.put((String)grant.get("code"),grant);
        service.schedule(()-> {
            grants.remove((String)grant.get("code"));
        },life, TimeUnit.MILLISECONDS);
    }

    public JSONObject retrieveGrant(String code){
        JSONObject grant =  grants.get(code);
        if (grant != null) {
            if ((Long)grant.get("expiry") < System.currentTimeMillis()){
                grants.remove(code);
                grant = null;
            }
        }
        return grant;
    }

}
