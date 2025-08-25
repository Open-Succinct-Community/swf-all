package com.venky.swf.path;

import com.venky.swf.db.model.Model;
import com.venky.swf.views._IView;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface _IPath {
	public String getAccept();
	public String getContentType();

	void setRequest(Request request);
	Request getRequest();


	InputStream getInputStream() throws IOException;
	
	void setSession(Session session);
	Session getSession();
	
	void setResponse(Response response);
	Response getResponse();
	
	Callback getCallback();
	void setCallback(Callback callBack);
	
	_IView invoke();
    String getTarget();
	String getBackTarget();
	String controllerPath();
	String controllerPathElement();
	String action();
	String parameter();
	Map<String, Object> getFormFields();
	boolean canAccessControllerAction();
	boolean canAccessControllerAction(String action);
	boolean canAccessControllerAction(String action, String parameter);
	_IPath createRelativePath(String toUrl);
	<M extends Model> _IPath getModelAccessPath(Class<M> modelClass);
	Object getSessionUser();
	Long getSessionUserId();
	
	void addErrorMessage(String msg);
	void addInfoMessage(String msg);
	List<String> getErrorMessages();
	List<String> getInfoMessages();
	
	boolean isGuestUserLoggedOn();
	void invalidateSession();
	void autoInvalidateSession();
	boolean isForwardedRequest();
	String getOriginalRequestUrl();

    boolean redirectOnException();

    public static final String USER_LOGIN_SUCCESS_EXTENSION = "user.login.success";
	public static final String USER_LOCATION_UPDATED_EXTENSION = "user.login.updated";
	
	
	public Map<String,String> getHeaders();
	
	public String getHeader(String name);
	
	_IPath  constructNewPath(String target);

}
