package com.venky.swf.plugins.oauth.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;

import com.venky.core.string.StringUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.Unrestricted;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.oauth.db.model.UserOid;
import com.venky.swf.routing.Path;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextBox;

public class OidController extends Controller{

	public OidController(Path path) {
		super(path);
	}

	protected View createLoginView(){
		HtmlView view = new HtmlView(getPath()) {
			
			@Override
			protected void createBody(Body b) {
				Form form = new Form();
				form.setAction(getPath().controllerPath(), "login");
				form.setMethod(Form.SubmitMethod.POST);
		        
				Label lblOpenId = new Label();
				lblOpenId.setText("OpenID:");
				
				TextBox txtOpenId = new TextBox();
				txtOpenId.setName("OPEN_ID");

				Submit sbm = new Submit("SignIn");

				form.addControl(lblOpenId);
				form.addControl(txtOpenId);
				form.addControl(sbm);
				b.addControl(form);
			}
		};
		
		return view;
	}
	
	private ConsumerManager _manager = null ;
	
	protected ConsumerManager getManager() {
		if ( _manager == null ){
			_manager = new ConsumerManager();
			_manager.getRealmVerifier().setEnforceRpId(false);
		}
		return _manager;
	}
	
	private void addAttributes(FetchRequest fetchReq,String[] attributeTypeURI) throws MessageException{
		for (int i  = 0 ; i < attributeTypeURI.length ; i++){
			fetchReq.addAttribute(attributeTypeURI[i], true);
		}
	}
	private FetchRequest initializeFetchRequest() throws MessageException{
		FetchRequest fetchReq = FetchRequest.createFetchRequest();
		addAttributes(fetchReq, EMAIL_TYPE_URI);
		addAttributes(fetchReq, FULLNAME_TYPE_URI);
		addAttributes(fetchReq, FIRSTNAME_TYPE_URI);
		addAttributes(fetchReq, LASTNAME_TYPE_URI);
		return fetchReq;
	}
	
	private static final String[] EMAIL_TYPE_URI = { "http://schema.openid.net/contact/email" ,  "http://axschema.org/contact/email" };
	private static final String[] FULLNAME_TYPE_URI = { "http://schema.openid.net/namePerson", "http://axschema.org/namePerson"};
	private static final String[] FIRSTNAME_TYPE_URI = { "http://schema.openid.net/namePerson/first", "http://axschema.org/namePerson/first"};
	private static final String[] LASTNAME_TYPE_URI = { "http://schema.openid.net/namePerson/last", "http://axschema.org/namePerson/last"};
	
	private List getEmails(FetchResponse response){
		String alias = getAlias(response, EMAIL_TYPE_URI);
		return response.getAttributeValues(alias);
	}
	private String getFullName(FetchResponse response){
		String fullNameAlias = getAlias(response, FULLNAME_TYPE_URI);
		String fullName = null ;
		
		if (fullNameAlias == null){
			String firstAlias = getAlias(response, FIRSTNAME_TYPE_URI);
			if (firstAlias != null){
				fullName = response.getAttributeValue(firstAlias);
			}
			String lastAlias = getAlias(response, LASTNAME_TYPE_URI);
			if (lastAlias != null){
				if (fullName != null){
					fullName += " " ;
				}
				fullName += response.getAttributeValue(lastAlias);
			}
		}else {
			fullName = response.getAttributeValue(fullNameAlias);
		}
		
		
		return fullName;
	}
	
	private String getAlias(FetchResponse response , String[] uriChoices){
		String alias = null ;
		for (int i = 0; alias == null && i < uriChoices.length ; i ++ ){
			alias = response.getAttributeAlias(uriChoices[i]);
		}
		return alias;
	}
	
	
	
	@SuppressWarnings("unused")
	protected View authenticate() {
		String openId = getPath().getRequest().getParameter("OPEN_ID");
		HttpSession newSession = null;
		try {
			ConsumerManager manager = getManager();
			List discoveries = manager.discover(openId);
			DiscoveryInformation discovered = manager.associate(discoveries);
			newSession = getPath().getRequest().getSession(true);
			newSession.setAttribute("discovered", discovered);
			newSession.setAttribute("manager", manager);
			
			HttpServletRequest req = getPath().getRequest();
			
			int port = req.getServerPort(); 
			String sPort = ":" + String.valueOf(port);
			if (port == -1 || port == 80 || port == 443){
				sPort = "" ; // Default ports must be squashed.
			}
			
			String returnUrl = req.getScheme() + "://"  + req.getServerName() + sPort + getPath().controllerPath() + "/verify"; 
					
			AuthRequest authReq = manager.authenticate(discovered, returnUrl);
			authReq.addExtension(initializeFetchRequest());
			
			RedirectorView ret = new RedirectorView(getPath());
			ret.setRedirectUrl(authReq.getDestinationUrl(true));
			return ret;
		} catch (DiscoveryException e) {
			if (newSession != null) {
				newSession.invalidate();
			}
			throw new RuntimeException(e);
		} catch (MessageException e) {
			if (newSession != null) {
				newSession.invalidate();
			}
			throw new RuntimeException(e);
		} catch (ConsumerException e) {
			if (newSession != null) {
				newSession.invalidate();
			}
			throw new RuntimeException(e);
		}
		
	}
	
	
	@Unrestricted
	public View verify(){
		HttpServletRequest request = getPath().getRequest();
		ParameterList openidResp = new ParameterList(request.getParameterMap());

	    // retrieve the previously stored discovery information
	    DiscoveryInformation discovered = (DiscoveryInformation) getPath().getSession().getAttribute("discovered");
	    ConsumerManager manager = (ConsumerManager) getPath().getSession().getAttribute("manager");
	    // extract the receiving URL from the HTTP request
	    StringBuffer receivingURL = request.getRequestURL();
	    String queryString = request.getQueryString();
	    if (queryString != null && queryString.length() > 0)
	        receivingURL.append("?").append(request.getQueryString());

	    // verify the response
	    VerificationResult verification;
		try {
			verification = manager.verify(receivingURL.toString(), openidResp, discovered);
		    // examine the verification result and extract the verified identifier
		    Identifier verified = verification.getVerifiedId();
		    
		    if (verified != null){
		    	AuthSuccess result = (AuthSuccess)verification.getAuthResponse(); 
		    	if (result.hasExtension(AxMessage.OPENID_NS_AX)){
		    		FetchResponse fetchResp = (FetchResponse) result.getExtension(AxMessage.OPENID_NS_AX);
		    		List emails = getEmails(fetchResp);
		    		String name = getFullName(fetchResp);

		    		User u = null;
	    			Select select = new Select().from(UserOid.class).where(new Expression("email",Operator.IN, emails.toArray()));
	    			List<UserOid> oids = select.execute();
	    			int numOids = oids.size();
	    			
	    			if (numOids > 0) {
		    			SortedSet<Integer> numUsers = new TreeSet<Integer>();
		    			for (UserOid oid: oids){
		    				numUsers.add(oid.getUserId());
		    			}
		    			assert (numUsers.size() == 1);
		    			u = Database.getInstance().getTable(getUserClass()).get(numUsers.first());
	    			}
	    			
	    			if (u == null){
		    			Transaction txn = Database.getInstance().createTransaction();
		    			u = Database.getInstance().getTable(User.class).newRecord();
		    			u.setName(name);
		    			u.setPassword(null);
		    			u.save();
		    			
		    			for (Object email : emails){
			    			UserOid oid = Database.getInstance().getTable(UserOid.class).newRecord();
			    			oid.setUserId(u.getId());
			    			oid.setEmail(StringUtil.valueOf(email));
			    			oid.save();
		    			}
		    			txn.commit();
		    		}
		    		HttpSession newSession = getPath().getSession();
		            newSession.setAttribute("user", u);
		            
	    			return new RedirectorView(getPath(), "dashboard");
		    	}
			}
		    getPath().getRequest().getSession().invalidate();
			return new RedirectorView(getPath(),"login");
		    
		} catch (MessageException e) {
			throw new RuntimeException(e);
		} catch (DiscoveryException e) {
			throw new RuntimeException(e);
		} catch (AssociationException e) {
			throw new RuntimeException(e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}
	 
}
