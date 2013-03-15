package com.venky.swf.path;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.swf.views._IView;

public interface _IPath {

	void setRequest(HttpServletRequest request);
	HttpServletRequest getRequest();
	
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
	Map<String, Object> getFormFields();
	boolean canAccessControllerAction();
	boolean canAccessControllerAction(String action);
	boolean canAccessControllerAction(String action, String parameter);
	_IPath createRelativePath(String toUrl);
	Object getSessionUser();
	Integer getSessionUserId();
	
	boolean isGuestUserLoggedOn();
	void invalidateSession();
	void autoInvalidateSession();
}
