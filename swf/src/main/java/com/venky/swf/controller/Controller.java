/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.venky.core.date.DateUtils;
import com.venky.swf.controller.annotations.Unrestricted;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.routing.Path;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.login.LoginView;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

/**
 *
 * @author venky
 */
public class Controller {
    protected final Path path;

    public Path getPath() {
        return path;
    }
    
    public Controller(Path path){
        this.path = path ;
    }
    @Unrestricted
    public View login(){
        if (getPath().getRequest().getMethod().equals("GET")&& getPath().getSession() == null){
            return createLoginView();
        }else if (getPath().getSession() != null){
    		return new RedirectorView(getPath(), "dashboard");
		}else{ 
			return authenticate();
        }
    }

    protected View authenticate(){
        String username = getPath().getRequest().getParameter("name");
        String password = getPath().getRequest().getParameter("password");
        User user = getUser(username);
        if (user != null && user.authenticate(password)){
            HttpSession newSession = getPath().getRequest().getSession(true);
            newSession.setAttribute("user", user);
            return new RedirectorView(getPath(), "dashboard");
        }else {
            throw new RuntimeException("Login incorrect!");
        }
    }
    
    protected View createLoginView(){
    	return new LoginView(getPath());
    }
    
    public <U extends User> U getSessionUser(){
    	return (U)getPath().getSessionUser();
    }
    public Class<? extends User> getUserClass(){
    	return Database.getInstance().getTable(User.class).getModelClass(); //Ensures correct model class is seen.
    }
    protected User getUser(String username){
        Select q = new Select().from(getUserClass());
        String nameColumn = ModelReflector.instance(getUserClass()).getColumnDescriptor("name").getName();
        q.where(new Expression(nameColumn,Operator.EQ,new BindVariable(username)));
        
		List<? extends User> users  = q.execute(getUserClass());
        if (users.size() == 1){
        	return users.get(0);
        }
        return null;
    }
    
    @Unrestricted
    public View logout(){
        if (getPath().getSession()!=null){
            getPath().getSession().invalidate();
        }
        return new RedirectorView(getPath(), "login");
    }

    public View index(){
        return new RedirectorView(getPath(), "dashboard");
    }

    public DashboardView dashboard(){
        DashboardView dashboard = new DashboardView(getPath());
        return dashboard;
    }
    
    protected DashboardView dashboard(HtmlView aContainedView){
        DashboardView dashboard = dashboard();
        dashboard.addChildView(aContainedView);
        return dashboard;
    }

    @Unrestricted
    public View resources(String name) throws IOException{
    	Path p = getPath();
    	if (name.equals("config")){
    		return new BytesView(p, "Access Denied!".getBytes());
    	}
    	
        String url = "/" + path.getTarget().substring(path.getTarget().indexOf(name));
        InputStream is = getClass().getResourceAsStream(url);
        
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] buffer = new byte[1024];
        int read = 0 ;
        try {
        	if (is != null){
	            while ((read = is.read(buffer)) >= 0){ 
	                baos.write(buffer,0,read);
	            }
        	}else {
        		return new BytesView(p, "No such resource!".getBytes());
        	}
        }catch (IOException ex){
            //
        }

        p.getResponse().setDateHeader("Expires", DateUtils.addHours(System.currentTimeMillis(), 24*365*15));
        return new BytesView(getPath(), baos.toByteArray());
    }
    
    public <M extends Model> View autocomplete(Class<M> modelClass, Expression baseWhereClause, String fieldName ,String value){
        XMLDocument doc = new XMLDocument("entries");
        XMLElement root = doc.getDocumentRoot();
        XMLElement elem = null ;
        System.out.println("Parameter:" + value);
        ModelReflector reflector = ModelReflector.instance(modelClass);
        Select q = new Select().from(modelClass);
        String columnName = reflector.getColumnDescriptor(fieldName).getName();
        Expression where = new Expression(Conjunction.AND);
        where.add(baseWhereClause);
        where.add(new Expression(columnName,Operator.LK,new BindVariable("%"+value+"%")));
        q.where(where);
        List<M> records = q.execute(modelClass);
        for (M record:records){
            try {
                elem = root.createChildElement("entry");
                elem.setAttribute("name", reflector.getFieldGetter(fieldName).invoke(record));
                elem.setAttribute("id", record.getId());
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return new BytesView(path, String.valueOf(doc).getBytes());
    }
    
}
