package com.venky.swf.controller;

import com.venky.cache.Cache;
import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.extensions.DefaultSocialLoginInfoExtractor;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.login.LoginView.LoginContext;
import org.apache.oltu.oauth2.client.HttpClient;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;


public class OidController extends Controller{

	public enum OAuthProviderType {
		FACEBOOK("https://graph.facebook.com","/oauth/authorize", "/oauth/access_token","/logout"),
		FOURSQUARE( "https://foursquare.com","/oauth2/authenticate", "/oauth2/access_token","/logout"),
		GITHUB( "https://github.com","/login/oauth/authorize", "/login/oauth/access_token","/logout"),
		GOOGLE("https://accounts.google.com","/o/oauth2/auth", "/o/oauth2/token","/logout"),
		INSTAGRAM("https://api.instagram.com","/oauth/authorize", "/oauth/access_token","/logout"),
		LINKEDIN("https://www.linkedin.com","/uas/oauth2/authorization", "/uas/oauth2/accessToken","/logout"),
		MICROSOFT( "https://login.live.com","/oauth20_authorize.srf", "/oauth20_token.srf","/logout"),
		PAYPAL("https://identity.x.com","/xidentity/resources/authorize", "/xidentity/oauthtokenservice","/logout"),
		REDDIT("https://ssl.reddit.com","/api/v1/authorize", "/api/v1/access_token","/logout"),
		SALESFORCE( "https://login.salesforce.com","/services/oauth2/authorize", "/services/oauth2/token","/logout"),
		YAMMER( "https://www.yammer.com","/dialog/oauth", "/oauth2/access_token.json","/logout"),
		HUMBOL("https://id.humbhionline.in","/oauth/authorize","/oauth/token","/logout?_redirect_to="+ Encode.forUriComponent(Config.instance().getServerBaseUrl()));

		private final String authzEndpoint;
		private final String tokenEndpoint;
		private final String defaultBaseUrl;
		private final String logoutEndPoint;
		OAuthProviderType(String defaultBaseUrl, String authzEndpoint, String tokenEndpoint){
			this(defaultBaseUrl,authzEndpoint,tokenEndpoint,null);
		}

	  	OAuthProviderType(String defaultBaseUrl, String authzEndpoint, String tokenEndpoint, String logoutEndPoint) {
			this.authzEndpoint = authzEndpoint;
			this.tokenEndpoint = tokenEndpoint;
			this.defaultBaseUrl = defaultBaseUrl;
			this.logoutEndPoint = logoutEndPoint;
		}

		public String authzEndpoint(String overrideBaseUrl) {
			return  String.format("%s%s",overrideBaseUrl != null ?overrideBaseUrl : defaultBaseUrl ,authzEndpoint) ;
		}

		public String tokenEndPoint(String overrideBaseUrl){
			return  String.format("%s%s",overrideBaseUrl != null ?overrideBaseUrl : defaultBaseUrl ,tokenEndpoint) ;
		}

		public String logoutEndPoint(String overrideBaseUrl){
			return  String.format("%s%s",overrideBaseUrl != null ?overrideBaseUrl : defaultBaseUrl ,logoutEndPoint) ;
		}
	}



	public OidController(Path path) {
		super(path);
	}
	public static class OIDProvider {
		public String getRedirectUrl(){
			if (!openIdProvider.equals("LINKEDIN")){
				return Config.instance().getServerBaseUrl() + "/oid/verify?SELECTED_OPEN_ID="+ openIdProvider;
			}else {
				return Config.instance().getServerBaseUrl() + "/oid/linkedin";
			}
		}
		public OIDProvider(String opendIdProvider, String overrideBaseUrl , OAuthProviderType providerType,
				String issuer , Class< ? extends OAuthAccessTokenResponse> tokenResponseClass,
						   String resourceUrl,String scope,GrantType grantType ,
						   boolean resoureUrlNeedsHeaders,boolean redirectUrlSupportsParams) {
			this.overrideBaseUrl = overrideBaseUrl;
			this.iss = issuer ; 
			this.tokenResponseClass = tokenResponseClass;
			this.resourceUrl = resourceUrl;
			this.openIdProvider = opendIdProvider ;  this.providerType = providerType; 
			this.clientId = Config.instance().getClientId(opendIdProvider); 
			this.clientSecret = Config.instance().getClientSecret(opendIdProvider) ; 
			this.scope = scope;
			this.grantType = grantType;
			this.resourceUrlNeedsHeaders = resoureUrlNeedsHeaders;
			this.redirectUrlSupportsParams = redirectUrlSupportsParams;

		}
		String overrideBaseUrl;
		boolean redirectUrlSupportsParams;
		boolean resourceUrlNeedsHeaders;
		GrantType grantType;
		String openIdProvider;
		OAuthProviderType providerType;
		String clientId;
		public String clientId(){
			return clientId;
		}
		String clientSecret;
		String iss;
		Class<? extends OAuthAccessTokenResponse> tokenResponseClass;
		String resourceUrl;
		String scope;
		public OAuthClientRequest createRequest(String _redirect_to){
			try {
				String redirectTo = getRedirectUrl() + (ObjectUtil.isVoid(_redirect_to) ?  "" : (redirectUrlSupportsParams ? "&_redirect_to=" : "/" )+ _redirect_to );
				return OAuthClientRequest.authorizationLocation(providerType.authzEndpoint(overrideBaseUrl)).
						setClientId(clientId).setResponseType(OAuth.OAUTH_CODE)
						.setScope(scope).
						setRedirectURI(redirectTo).buildQueryMessage();
			} catch (OAuthSystemException e) {
				throw new RuntimeException(e);
			}
		}
		public JSONObject authorize(String code,String _redirect_to){
			try {
				String redirectTo = getRedirectUrl() + (ObjectUtil.isVoid(_redirect_to) ?  "" : (redirectUrlSupportsParams ? "&_redirect_to=" : "/" ) +_redirect_to);
				OAuthClientRequest oauthRequest = OAuthClientRequest
				        .tokenLocation(providerType.tokenEndPoint(overrideBaseUrl))
				        .setGrantType(grantType)
				        .setClientId(clientId)
				        .setClientSecret(clientSecret)
				        .setRedirectURI(redirectTo)
				        .setCode(code)
				        .setScope(scope)
				        .buildBodyMessage();
				oauthRequest.addHeader("Authorization" , "Basic " + Crypt.getInstance().toBase64(String.format("%s:%s",clientId,clientSecret).getBytes(StandardCharsets.UTF_8)));

				OAuthClient oAuthClient = new OAuthClient(new OidHttpClient());

		        OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(oauthRequest, tokenResponseClass);

				ObjectHolder<JSONObject> userJsonHolder = new ObjectHolder<>(null);
		        if (ObjectUtil.isVoid(resourceUrl)){
					Registry.instance().callExtensions(DefaultSocialLoginInfoExtractor.class.getName(),this,oAuthResponse,userJsonHolder);
		        }else {
		        	String accessToken = oAuthResponse.getAccessToken();

			    	OAuthClientRequest bearerClientRequest ;
					OAuthBearerClientRequest oAuthBearerClientRequest = new OAuthBearerClientRequest(resourceUrl)
							.setAccessToken(accessToken);

			    	if (!resourceUrlNeedsHeaders){
						bearerClientRequest = oAuthBearerClientRequest.buildQueryMessage();
					}else{
			    		bearerClientRequest = oAuthBearerClientRequest.buildHeaderMessage();
					}
					bearerClientRequest.addHeader("accept","application/json");

					OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
					Registry.instance().callExtensions(DefaultSocialLoginInfoExtractor.class.getName(),this,resourceResponse,userJsonHolder);
					//return extractEmail(resourceResponse);
		        }
				return userJsonHolder.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}


		public String iss() {
			return iss;
		}


	}
	private final static Map<String,OIDProvider> oidproviderMap = new HashMap<>();
	static { 
		oidproviderMap.put("GOOGLE", new OIDProvider("GOOGLE",null, OAuthProviderType.GOOGLE,"accounts.google.com",
				OAuthJSONAccessTokenResponse.class,"","email",GrantType.AUTHORIZATION_CODE,false,true));

		oidproviderMap.put("FACEBOOK", new OIDProvider("FACEBOOK",null, OAuthProviderType.FACEBOOK,"",
				OAuthJSONAccessTokenResponse.class,"https://graph.facebook.com/me?fields=email,name","email",
				GrantType.AUTHORIZATION_CODE,false,true));

		oidproviderMap.put("LINKEDIN", new OIDProvider("LINKEDIN", null ,OAuthProviderType.LINKEDIN,"",
				OAuthJSONAccessTokenResponse.class,"https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))","r_emailaddress",
				GrantType.AUTHORIZATION_CODE, true,false));


		for (Map.Entry<String,Map<String,String>> group : getHumBolProviders().entrySet()) {
			oidproviderMap.put(group.getKey(), new OIDProvider(group.getKey(), group.getValue().get("base.url") ,
					OAuthProviderType.HUMBOL, group.getValue().get("base.url"),
					OAuthJSONAccessTokenResponse.class, group.getValue().get("resource.url"), "all",
					GrantType.AUTHORIZATION_CODE, true,
					true));
		}
	}

	public static Map<String, Map<String,String>>   getHumBolProviders() {
		Map<String, Map<String,String>> groupMap = new Cache<>() {
			@Override
			protected Map<String, String> getValue(String groupKey) {
				return new Cache<>() {
					@Override
					protected String getValue(String key) {
						return Config.instance().getProperty(String.format("swf.%s.%s",groupKey,key));
					}
				};
			}
		};
		for (String humBolKeys : Config.instance().getPropertyKeys("swf\\.HUMBOL.*\\..*")){
			String[] group = humBolKeys.split("\\.");
			String groupKey = group[1];
			groupMap.get(groupKey);
		}
		return groupMap;

	}

	protected View authenticate() {
		String selectedOpenId = getPath().getRequest().getParameter("SELECTED_OPEN_ID");
		
		if (ObjectUtil.isVoid(selectedOpenId)){
			HtmlView lv = createLoginView(LoginContext.LOGIN);
			lv.setStatus(StatusType.ERROR, "Open id provider not specified");
			return lv;
		}
		
		try {
			String _redirect_to= getPath().getRequest().getParameter("_redirect_to");
			OAuthClientRequest request = oidproviderMap.get(selectedOpenId).createRequest(_redirect_to);
			RedirectorView ret = new RedirectorView(getPath());
			ret.setRedirectUrl(request.getLocationUri());
			return ret;
		} catch (Exception e) {
			return createLoginView(StatusType.ERROR,e.getMessage());
		} 
	}
	@RequireLogin(false)
	public View logout() {
		invalidateSession();
		String selectedOpenId = getPath().getRequest().getParameter("SELECTED_OPEN_ID");
		if (ObjectUtil.isVoid(selectedOpenId)){
			throw new RuntimeException("Open id provider not specified");
		}
		OIDProvider provider = oidproviderMap.get(selectedOpenId);

		return new RedirectorView(getPath(), provider.providerType.logoutEndPoint(provider.overrideBaseUrl),"");
	}

	@RequireLogin(false)
	public View linkedin()throws OAuthProblemException, OAuthSystemException, ParseException{
		return linkedin("");
	}
	@RequireLogin(false)
	public View linkedin(String redirectUrl) throws OAuthProblemException, OAuthSystemException, ParseException{
		return verify("LINKEDIN",redirectUrl);
	}
	@RequireLogin(false)
	public View verify() throws OAuthProblemException, OAuthSystemException, ParseException {
		return verify(getPath().getRequest().getParameter("SELECTED_OPEN_ID"),getPath().getRequest().getParameter("_redirect_to"));
	}
	private View verify(String selectedOpenId,String redirectedTo) throws OAuthProblemException{
		HttpServletRequest request = getPath().getRequest();
		OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
		String code = oar.getCode();
		
		OIDProvider provider = oidproviderMap.get(selectedOpenId);

		try {
	        JSONObject userObject = provider.authorize(code,redirectedTo);
			String email = (String)userObject.get("email");
			Transaction txn = Database.getInstance().getTransactionManager().createTransaction();
			User u = initializeUser(selectedOpenId,userObject);
			List<UserEmail> emails = u.getUserEmails();
			if (emails.isEmpty() && !ObjectUtil.isVoid(email)){
				UserEmail oid = Database.getTable(UserEmail.class).newRecord();
				oid.setUserId(u.getId());
				oid.setEmail(email);
				oid.save();
			}
			txn.commit();
    		getPath().createUserSession(u, false);
            
			return redirectSuccess(redirectedTo);
		} catch (Exception e) {
			cat.log(Level.WARNING,e.getMessage(),e);
			return createLoginView(StatusType.ERROR, e.getMessage());
		}
	}

	private User initializeUser(String selectedOpenId , JSONObject userObject) {
		FormatHelper.instance(userObject).change_key_case(KeyCase.SNAKE,KeyCase.CAMEL);

		User u = ModelIOFactory.getReader(User.class, JSONObject.class).read(userObject,true);
		u = Database.getTable(User.class).getRefreshed(u);
		u.save();
		return u;
	}

	protected RedirectorView redirectSuccess(String redirectedTo){
		return new RedirectorView(getPath(),"",loginSuccessful(redirectedTo));
	}
	
	public static class OidHttpClient implements HttpClient {

	    public OidHttpClient() {
	    }

	    public <T extends OAuthClientResponse> T execute(OAuthClientRequest request, Map<String, String> headers,
	                                                     String requestMethod, Class<T> responseClass)
	            throws OAuthSystemException, OAuthProblemException {

	        InputStream responseBody = null;
	        URLConnection c;
	        Map<String, List<String>> responseHeaders = new HashMap<>();
	        int responseCode;
	        try {
	            URL url = new URI(request.getLocationUri()).toURL();

	            c = url.openConnection();
	            c.setConnectTimeout(20000);
	            c.setReadTimeout(20000);
	            responseCode = -1;
	            if (c instanceof HttpURLConnection httpURLConnection) {
					
					if (headers != null && !headers.isEmpty()) {
	                    for (Map.Entry<String, String> header : headers.entrySet()) {
	                        httpURLConnection.addRequestProperty(header.getKey(), header.getValue());
	                    }
	                }

	                if (request.getHeaders() != null) {
	                    for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
	                        httpURLConnection.addRequestProperty(header.getKey(), header.getValue());
	                    }
	                }

	                if (OAuthUtils.isEmpty(requestMethod)) {
	                    httpURLConnection.setRequestMethod(OAuth.HttpMethod.GET);
	                } else {
	                    httpURLConnection.setRequestMethod(requestMethod);
	                    setRequestBody(request, requestMethod, httpURLConnection);
	                }
	                
	                httpURLConnection.connect();

	                InputStream inputStream;
	                responseCode = httpURLConnection.getResponseCode();
	                if (responseCode == SC_BAD_REQUEST || responseCode == SC_UNAUTHORIZED) {
	                    inputStream = httpURLConnection.getErrorStream();
	                } else {
	                    inputStream = httpURLConnection.getInputStream();
	                }

	                responseHeaders = httpURLConnection.getHeaderFields();
	                responseBody = inputStream;
	            }
	        } catch (IOException | URISyntaxException e) {
	            throw new OAuthSystemException(e);
	        }

	        return OAuthClientResponseFactory
	                .createCustomResponse(responseBody, c.getContentType(), responseCode, responseHeaders, responseClass);
	    }

	    private void setRequestBody(OAuthClientRequest request, String requestMethod, HttpURLConnection httpURLConnection)
	            throws IOException {
	        String requestBody = request.getBody();
	        if (OAuthUtils.isEmpty(requestBody)) {
	            return;
	        }

	        if (OAuth.HttpMethod.POST.equals(requestMethod) || OAuth.HttpMethod.PUT.equals(requestMethod)) {
	            httpURLConnection.setDoOutput(true);
	            OutputStream ost = httpURLConnection.getOutputStream();
	            PrintWriter pw = new PrintWriter(ost);
	            pw.print(requestBody);
	            pw.flush();
	            pw.close();
	        }
	    }

	    @Override
	    public void shutdown() {
	        // Nothing to do here
	    }

	}

}
