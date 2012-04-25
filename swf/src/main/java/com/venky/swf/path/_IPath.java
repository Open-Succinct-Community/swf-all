package com.venky.swf.path;

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

}
