package com.venky.swf.controller;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.User;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.login.LoginView.LoginContext;
import com.venky.swf.views.model.ModelListView;
import com.venky.swf.views.model.ModelShowView;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UsersController extends ModelController<User>{

	public UsersController(Path path) {
		super(path);
	}

	@SingleRecordAction
	public View generateApiKey(long id){
		User u = Database.getTable(User.class).get(id);
		u.generateApiKey();
		StringBuilder message = new StringBuilder(); 
		message.append("API Key for ").append(u.getName()).append(" generated: (").append(u.getApiKey()).append(")");
		if (getIntegrationAdaptor() != null){
			return getIntegrationAdaptor().createResponse(getPath(), u,Arrays.asList("API_KEY"));
		}else {
			return new BytesView(getPath(), u.getApiKey().getBytes());
		}
		
	}

	protected View forgot_password(String userName){
		throw new RuntimeException("Please ask administrator to reset your password");
	}

	@RequireLogin(value = false)
	public View reset_password(){
		if (getPath().getRequest().getMethod().equals("GET")){
			return createLoginView(LoginContext.PASSWORD_RESET);
		}
		Map<String,Object> formFields = getPath().getFormFields();
		String name = (String)formFields.get("name");
		String apiKey = (String)formFields.get("ApiKey");
		String password = (String)formFields.get("password");
		String password2 = (String)formFields.get("password2");
		if (!ObjectUtil.isVoid(name)){
			return forgot_password(name);
		}
		if (!ObjectUtil.equals(password,password2)){
			return new RedirectorView( getPath(),"reset_password?ApiKey=" + apiKey +"&message=" + "Passwords not matching");
		}
		User u = getPath().getUser("API_KEY",apiKey);
		if (u == null){
			return new RedirectorView( getPath(),"reset_password?ApiKey=" + apiKey +"&message=" + "Stale Link");
		}
		User user = u.getRawRecord().getAsProxy(User.class);
		user.setChangePassword(password);
		user.generateApiKey(true);
		return new RedirectorView( getPath(),"/","login?message=Password Reset");
	}
	
    protected View constructModelListView(List<User> records, boolean isCompleteList){
    	@SuppressWarnings("unchecked")
		ModelListView<User> v = (ModelListView<User>) super.constructModelListView(records, isCompleteList);
    	v.getIncludedFields().remove("CHANGE_PASSWORD");
    	return v;
    }
    protected ModelShowView<User> constructModelShowView(Path path, User record){
    	ModelShowView<User> v = new ModelShowView<User>(path, getIncludedFields(), record);
    	v.getIncludedFields().remove("CHANGE_PASSWORD");
    	return v;
    }

	/*
	Only root can close
	 */
	public View closeAccount(long id){
		if (getPath().getSessionUserId() == 1 && id != 1){
			User user = Database.getTable(getModelClass()).get(id);
			user.setAccountClosed(true);
			user.save();
			return back();
		}else {
			throw new AccessDeniedException("Insufficient Privileges");
		}
	}
	@RequireLogin
	public View requestAccountClosure(){
		User user = getPath().getSessionUser();
		if (getPath().getRequest().getMethod().equals("GET")){
			return html("close");
		}
		String msg = "Initiated request to close account.";
		if (user != null) {
			user.setAccountClosureInitiated(true);
			user.save();
			if (getPath().getProtocol() == MimeType.TEXT_HTML){
				getPath().addInfoMessage(msg);
				return back();
			}else {
				JSONObject out = new JSONObject();
				out.put("message",msg);
				return new BytesView(getPath(),out.toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
			}
		}else {
			return new BytesView(getPath(), new byte[]{}, MimeType.TEXT_PLAIN);
		}
	}

	@RequireLogin
	public View cancelAccountClosureRequest(){
		User user = getPath().getSessionUser();
		String msg = "Cancelled your request to close account!!";
		if (user != null) {
			user.setAccountClosureInitiated(false);
			user.save();
			if (getPath().getProtocol() == MimeType.TEXT_HTML){
				getPath().addInfoMessage(msg);
				return back();
			}else {
				JSONObject out = new JSONObject();
				out.put("message",msg);
				return new BytesView(getPath(),out.toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
			}
		}else {
			return new BytesView(getPath(), new byte[]{}, MimeType.TEXT_PLAIN);
		}
	}



}
