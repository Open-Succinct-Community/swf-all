/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
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
    private static final   Set<String> TARGET_LOGIN = new HashSet<String>();
    private static final   Set<String> TARGET_LOGOUT = new HashSet<String>();
    static { 
        TARGET_LOGIN.add("/login");
        TARGET_LOGIN.add("/app/login");
        TARGET_LOGOUT.add("/logout");
        TARGET_LOGOUT.add("/app/logout");
        
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
        loadControllerClassName();
    }
    

    public String getTarget() {
        return target;
    }

    private void loadControllerClassName(){
        if (controllerClassName != null){
            return;
        }
        for (int i = pathelements.size() - 1; i >= 0; i--) {
            String pe = pathelements.get(i);
            String camelizedPe = camelize(pe);
            String clazzName = Config.instance().getProperty(Config.CONTROLLER_PACKAGE_ROOT) + "." + camelizedPe + "Controller";
            if (getClass(clazzName) != null) {
                controllerClassName = clazzName;
                controllerPathIndex = i ;
                break;
            }else {
                Class<?> modelClass = getModelClass(camelizedPe);
                if (modelClass != null){
                    controllerClassName = ModelController.class.getName();
                    controllerPathIndex = i ;
                    break;
                }
            }
        }
        if (controllerClassName == null) {
            controllerClassName = Config.instance().getProperty(Config.CONTROLLER_PACKAGE_ROOT) + ".AppController";
            pathelements.add(0, "app");
            controllerPathIndex = 0;
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
    		backTarget.append(controllerPath()).append("/index");
    	}
    	return backTarget.toString();
    }
    public String controllerPathElement(){
        if (controllerPathIndex <= pathelements.size() - 1){
            return pathelements.get(controllerPathIndex);
        }
        throw new RuntimeException("Controller pathelement could not be determined!");
    }

    public <M extends Model> Class<M> getModelClass(){
        return getModelClass(camelize(controllerPathElement()));
    }
    
    private <M extends Model> Class<M> getModelClass(String camelizedPathElement){
        String tableName = Table.tableName(camelizedPathElement);
        Table<M> table = Database.getInstance().getTable(tableName);
        if (table == null){
            return null;
        }else {
            return table.getModelClass();
        }
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
    

    public View invoke() {
        if (getSession() == null && !TARGET_LOGIN.contains(getTarget()) && !getTarget().startsWith("/resources")){ 
        	// Resources is primarily to ensure scripts and css in login view itself are not redirected.
            return new RedirectorView(this, "","login");
        }

        for (Method m :getControllerClass().getMethods()){
            if (m.getName().equals(action()) && 
                    View.class.isAssignableFrom(m.getReturnType()) ){
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
    	throw new RuntimeException("Donot know how to invoke controller action" + getTarget()) ;
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

    private String camelize(String s) {
        return StringUtil.camelize(s);
    }
}
