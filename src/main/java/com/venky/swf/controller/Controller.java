/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Query;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.login.LoginView;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.servlet.http.HttpSession;

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
    public View login(){
        if (getPath().getRequest().getMethod().equals("GET")&& getPath().getSession() == null){
            return new LoginView(getPath());
        }else if (getPath().getSession() != null){
    		return new RedirectorView(getPath(), "dashboard");
		}else{ 
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
    }
    protected Class<? extends User> getUserClass(){
    	return User.class;
    }
    protected User getUser(String username){
		Table<? extends User> userTable = Database.getInstance().getTable(getUserClass());
        Query q = new Query();
        q.select(userTable.getTableName());
        String nameColumn = ModelReflector.instance(getUserClass()).getColumnDescriptor("name").getName();
        q.add(" where " + nameColumn + " = ? ",new BindVariable(username));
        
		List<? extends User> users  = q.execute(getUserClass());
        if (users.size() == 1){
        	return users.get(0);
        }
        return null;
    }
    
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

    public View resources(String name) throws IOException{
        String url = "/" + path.getTarget().substring(path.getTarget().indexOf(name));
        
        InputStream is = getClass().getResourceAsStream(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] buffer = new byte[1024];
        int read = 0 ;
        try {
            while ((read = is.read(buffer)) >= 0){ 
                baos.write(buffer,0,read);
            }
        }catch (IOException ex){
            //
        }
        return new BytesView(getPath(), baos.toByteArray());
    }
    
    public <M extends Model> View autocomplete(Class<M> modelClass, String fieldName ,String value){
        XMLDocument doc = new XMLDocument("entries");
        XMLElement root = doc.getDocumentRoot();
        XMLElement elem = null ;
        System.out.println("Parameter:" + value);
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        Query q = new Query();
        Table<?> table = Database.getInstance().getTable(modelClass);

        q.select(table.getTableName());
        String columnName = reflector.getColumnDescriptor(fieldName).getName();
        
        q.add(" WHERE ").add(columnName).add(" like ?", new BindVariable("%"+value+"%"));
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
