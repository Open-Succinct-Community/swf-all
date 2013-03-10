package com.venky.swf.controller;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
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
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextBox;

public class OidController extends Controller{

	public OidController(Path path) {
		super(path);
	}
	
	protected HtmlView createLoginView(){
		invalidateSession();
		HtmlView view = new HtmlView(getPath()) {
			
			@Override
			protected void createBody(_IControl b) {
				Form form = new Form();
				form.setAction(getPath().controllerPath(), "login");
				form.setMethod(Form.SubmitMethod.POST);

				Submit sbm = new Submit("SignIn");
				
		        
				Table layout = new Table();
				Row row = layout.createRow();

				Label lblOpenId = new Label();
				lblOpenId.setText("Choose your OpenID provider");
				com.venky.swf.views.controls.page.text.Select cmbOpenId = new com.venky.swf.views.controls.page.text.Select();
				cmbOpenId.setName("SELECTED_OPEN_ID");
				cmbOpenId.createOption("-Select-", "");
				cmbOpenId.createOption("Google", "https://www.google.com/accounts/o8/id");
				cmbOpenId.createOption("Yahoo", "https://me.yahoo.com/");
				cmbOpenId.createOption("OpenId", "https://myopenid.com/");
				row.createColumn().addControl(lblOpenId);
				row.createColumn().addControl(cmbOpenId);
				row.createColumn().addControl(sbm);
				
				row = layout.createRow();
				row.createColumn(3).addControl(new Label("OR"));
				
				row = layout.createRow();
				row.createColumn().addControl(new Label("Enter your OpenID provider"));
				TextBox txtOpenId = new TextBox();
				txtOpenId.setName("OPEN_ID");
				row.createColumn().addControl(txtOpenId);				
				row.createColumn().addControl(sbm);				
				
				String _redirect_to = getPath().getRequest().getParameter("_redirect_to");
				if (!ObjectUtil.isVoid(_redirect_to)){
					TextBox redirect_to = new TextBox();
					redirect_to.setVisible(false);
					redirect_to.setName("_redirect_to");
					redirect_to.setValue(_redirect_to);
					layout.createRow().createColumn().addControl(redirect_to);
				}
				form.addControl(layout);
				b.addControl(form);
			}
		};
		
		return view;
	}
	
	private ConsumerManager _manager = null ;
	
	protected ConsumerManager getManager() {
		if ( _manager == null ){
			_manager = new ConsumerManager();
			if (Config.instance().isDevelopmentEnvironment()){
				_manager.getRealmVerifier().setEnforceRpId(false);	
			}
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
	
	@SuppressWarnings("rawtypes")
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
	
	
	
	@SuppressWarnings("rawtypes")
	protected View authenticate() {
		String openId = getPath().getRequest().getParameter("OPEN_ID");
		String selectedOpenId = getPath().getRequest().getParameter("SELECTED_OPEN_ID");
		String _redirect_to = getPath().getRequest().getParameter("_redirect_to");
		
		if (ObjectUtil.isVoid(openId) && ObjectUtil.isVoid(selectedOpenId)){
			HtmlView lv = createLoginView();
			lv.setStatus(StatusType.ERROR, "Open id provider not specified");
			return lv;
		}
		
		if (ObjectUtil.isVoid(openId) && !ObjectUtil.isVoid(selectedOpenId)){
			openId = selectedOpenId;
		}
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
			
			String returnUrl = req.getScheme() + "://"  + req.getServerName() + sPort + getPath().controllerPath() + "/verify" + (_redirect_to == null ? "" : "?_redirect_to=" + _redirect_to);  
					
			AuthRequest authReq = manager.authenticate(discovered, returnUrl);
			authReq.addExtension(initializeFetchRequest());
			
			RedirectorView ret = new RedirectorView(getPath());
			ret.setRedirectUrl(authReq.getDestinationUrl(true));
			return ret;
		} catch (Exception e) {
			return createLoginView(StatusType.ERROR,e.getMessage());
		} 
	}
	
	
	@SuppressWarnings("rawtypes")
	@RequireLogin(false)
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
	    			Select select = new Select().from(UserEmail.class).where(new Expression("email",Operator.IN, emails.toArray()));
	    			List<UserEmail> oids = select.execute(UserEmail.class);
	    			int numOids = oids.size();
	    			
	    			if (numOids > 0) {
		    			SortedSet<Integer> numUsers = new TreeSet<Integer>();
		    			for (UserEmail oid: oids){
		    				numUsers.add(oid.getUserId());
		    			}
		    			if (numUsers.size() > 1) {
		    				return createLoginView(StatusType.ERROR, "Multiple users associated with same email id");
		    			}
						u = Database.getTable(User.class).get(numUsers.first());
	    			}
	    			
	    			if (u == null){
		    			Transaction txn = Database.getInstance().createTransaction();
						u = Database.getTable(User.class).newRecord();
		    			u.setName(name);
		    			u.setPassword(null);
		    			u.save();
		    			
		    			for (Object email : emails){
							UserEmail oid = Database.getTable(UserEmail.class).newRecord();
			    			oid.setUserId(u.getId());
			    			oid.setEmail(StringUtil.valueOf(email));
			    			oid.save();
		    			}
		    			txn.commit();
		    		}
		    		HttpSession newSession = getPath().getSession();
		            newSession.setAttribute("user", u);
		            
	    			return new RedirectorView(getPath(), loginSuccessful());
		    	}
			}
		    return createLoginView();
		} catch (Exception e) {
			return createLoginView(StatusType.ERROR, e.getMessage());
		}

	}
	 
}
