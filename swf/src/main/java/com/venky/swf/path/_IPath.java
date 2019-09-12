package com.venky.swf.path;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.views._IView;

public interface _IPath {

	void setRequest(HttpServletRequest request);
	HttpServletRequest getRequest();

	InputStream getInputStream();
	
	void setSession(HttpSession session);
	HttpSession getSession();
	
	void setResponse(HttpServletResponse response);
	HttpServletResponse getResponse();
	
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

}
