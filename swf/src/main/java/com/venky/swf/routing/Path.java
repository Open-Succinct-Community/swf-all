/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.core.string.StringUtil;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.Unrestricted;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;

/**
 *
 * @author venky
 */
public class Path {

    private List<String> pathelements = new ArrayList<String>();
    private String controllerClassName = null;
    private int controllerPathIndex = 0;
    private int actionPathIndex = 1 ; 
    private int parameterPathIndex = 2; 
    private String target = null;
    private HttpSession session = null ;
    private HttpServletRequest request = null ;
    private HttpServletResponse response = null ;
    public User getSessionUser(){
    	if (getSession() == null){
    		return null;
    	}
    	return (User)getSession().getAttribute("user");
    }

    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }
    
    public Path(String target) {
        this.target = target;
        StringTokenizer stok = new StringTokenizer(target, "/");
        while (stok.hasMoreTokens()) {
            pathelements.add(stok.nextToken());
        }
        
        int pathElementSize = pathelements.size();
        for (int i = 0 ; i < pathElementSize ; i++){
        	String token = pathelements.get(i);
        	Class<? extends Model> modelClass = getModelClass(token);
        	if (modelClass != null){
        		ModelInfo info = new ModelInfo(modelClass);
        		modelElements.add(info);
        		if (i <pathElementSize -1){
	        		info.setAction(pathelements.get(i+1));
	        		i+= 1;
        		}
        		try {
        			if (i < pathElementSize - 1){
	        			info.setId(Integer.valueOf(pathelements.get(i+1)));
	        			i+=1;
        			}
        		}catch (NumberFormatException ex){
        			//
        		}
        	}
    	}
        loadControllerClassName();
    }
    
    public static class ModelInfo{
    	private Class<? extends Model> modelClass;
    	private Integer id;
    	private String action = "index";
    	public ModelInfo(Class<? extends Model> modelClass){
    		setModelClass(modelClass);
    	}
		public Class<? extends Model> getModelClass() {
			return modelClass;
		}
		public void setModelClass(Class<? extends Model> modelClass) {
			this.modelClass = modelClass;
		}
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getAction() {
			return action;
		}
		public void setAction(String action) {
			this.action = action;
		}
		
    }
    
    List<ModelInfo> modelElements = new ArrayList<Path.ModelInfo>();
    
    public List<ModelInfo> getModelElements(){
    	return modelElements;
    }
    

    public String getTarget() {
        return target;
    }

    private void loadControllerClassName(){
        if (controllerClassName != null){
            return;
        }
        
        boolean controllerFound = false;
        for (int i = pathelements.size() - 1; i >= 0 && !controllerFound; i--) {
            String pe = pathelements.get(i);
            
            for (String controllerPackageRoot: Config.instance().getControllerPackageRoots()){
                String clazzName = controllerPackageRoot + "." + camelize(pe) + "Controller";
                if (getClass(clazzName) != null) {
                    controllerClassName = clazzName;
                    controllerPathIndex = i ;
                    controllerFound = true;
                }else {
                    Class<? extends Model> modelClass = getModelClass(pe);
                    if (modelClass != null){
                        controllerClassName = ModelController.class.getName();
                        controllerPathIndex = i ;
                        controllerFound = true;
                    }
                }
                if (controllerFound){
                	break;
                }
            }
        }
        if (controllerClassName == null) {
            controllerClassName = Controller.class.getName();
            controllerPathIndex = -1;
        }
        actionPathIndex = controllerPathIndex + 1 ;
        parameterPathIndex = controllerPathIndex + 2;
    }
    
    public String controllerPath(){
        if (controllerPathIndex <= pathelements.size() -1){
            StringBuilder p = new StringBuilder();
            for (int i = 0; i<= controllerPathIndex ; i ++){
                p.append("/");
                p.append(pathelements.get(i));
            }
            return p.toString();
        }
        throw new RuntimeException("Controller path could not be determined!");
    }
    
    public String getBackTarget(){
    	StringBuilder backTarget = new StringBuilder();
    	if (controllerPathIndex > 0 && controllerPathIndex < pathelements.size()) {
        	for (int i = 0 ; i < controllerPathIndex ; i ++  ){
        		backTarget.append("/");
        		backTarget.append(pathelements.get(i));
        	}
    	}
    	if (backTarget.length() == 0){
    		backTarget.setLength(0);
    		backTarget.append(controllerPath()).append("/index");
    	}
    	return backTarget.toString();
    }
    public String controllerPathElement(){
        if (controllerPathIndex <= pathelements.size() - 1){
        	if (controllerPathIndex >= 0){
                return pathelements.get(controllerPathIndex);
        	}else {
        		return "";
        	}
        }
        throw new RuntimeException("Controller pathelement could not be determined!");
    }

    public <M extends Model> Class<M> getModelClass(){
        return getModelClass(controllerPathElement());
    }
    
    public static <M extends Model> Class<M> getModelClass(String pathElement){
        Table<M> table = getTable(pathElement);
        if (table == null){
            return null;
        }else {
            return table.getModelClass();
        }
    }
    public static <M extends Model> Table<M> getTable(String pathElement){
        String tableName = Table.tableName(StringUtil.singularize(camelize(pathElement)));
        Table<M> table = Database.getInstance().getTable(tableName);
        return table;
    }
    
    public String action() {
        String action = "index";
        if (actionPathIndex <= pathelements.size() - 1) {
            action = pathelements.get(actionPathIndex);
        }
        return action;
    }

    public String parameter() {
        String parameter = null;
        if (parameterPathIndex <= pathelements.size() - 1) {
            parameter = pathelements.get(parameterPathIndex);
        }
        return parameter;
    }

    private Object controller = null; 
    public Object controller() {
        if (controller != null){
            return controller;
        }
        
        try {
            controller = getControllerClass().getConstructor(Path.class).newInstance(this);
            return controller;
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
    
    public static final String ALLOW_CONTROLLER_ACTION = "allow.controller.action" ; 
    
    public boolean isUserLoggedOn(){
    	return getSessionUser() != null; 
    }
    
    public boolean isSecuredAction(Method m){
    	return !m.isAnnotationPresent(Unrestricted.class);
    }
    
    
    public View invoke() throws AccessDeniedException{

    	for (Method m :getControllerClass().getMethods()){
            if (m.getName().equals(action()) && 
                    View.class.isAssignableFrom(m.getReturnType()) ){
            	boolean securedAction = isSecuredAction(m) ;
            	if (securedAction){
            		if (!isUserLoggedOn()){
                		return new RedirectorView(this,"","login");
            		}else {
            			ensureControllerActionAccess();	
            		}
            	}
            	try {
                    if (m.getParameterTypes().length == 0 && parameter() == null){
                        return (View)m.invoke(controller());
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == String.class){
                        return (View)m.invoke(controller(), parameter());
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == int.class){
                        return (View)m.invoke(controller(), Integer.valueOf(parameter()));
                    }else if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == long.class){
                        return (View)m.invoke(controller(), Long.valueOf(parameter()));
                    }
            	}catch(Exception e){
        			throw new RuntimeException(e);
            	}
            }
        }
		if (!isUserLoggedOn()){
    		return new RedirectorView(this,"","login");
		}
    	throw new RuntimeException("Donot know how to invoke controller action" + getTarget()) ;
    }
    
    public boolean canAccessControllerAction(){
    	return canAccessControllerAction(action());
    }
    public boolean canAccessControllerAction(String actionPathElement){
    	return canAccessControllerAction(actionPathElement,parameter());
    }
    public boolean canAccessControllerAction(String actionPathElement,String parameterPathElement){
    	return canAccessControllerAction(getSessionUser(), controllerPathElement(), actionPathElement, parameterPathElement);
    }

    public static boolean canAccessControllerAction(User user,String controllerPathElement,String actionPathElement,String parameterPathElement){
    	try {
    		ensureControllerActionAccess(user,controllerPathElement,actionPathElement,parameterPathElement);
    	}catch (AccessDeniedException ex){
    		return false;
    	}
    	return true;
    }
    
    private void ensureControllerActionAccess() throws AccessDeniedException{
    	ensureControllerActionAccess(getSessionUser(),controllerPathElement(),action(),parameter()); 
    }
    private static void ensureControllerActionAccess(User user,String controllerPathElement,String actionPathElement , String parameterPathElement) throws AccessDeniedException{
    	Registry.instance().callExtensions(ALLOW_CONTROLLER_ACTION, user, controllerPathElement,actionPathElement,parameterPathElement);
    }
    

    private Class getControllerClass() {
        return getClass(getControllerClassName());
    }

    private Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
    private String getControllerClassName() {
        return controllerClassName;
    }

    private static String camelize(String s) {
        return StringUtil.camelize(s);
    }
    
    public Path createRelativePath(String toUrl){
    	String relPath = null; 
    	if (!action().equals("index")){
    		relPath = getTarget();
    	}else {
    		relPath = controllerPath() ;
    	}
    	
    	if (!toUrl.startsWith("/")){
    		relPath = relPath + "/" + toUrl;
    	}else {
    		relPath = relPath + toUrl;
    	}
    	Path path = new Path(relPath);
    	path.setRequest(getRequest());
    	path.setResponse(getResponse());
    	path.setSession(getSession());
    	return path;
    }
}
