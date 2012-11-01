/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.venky.core.log.TimerStatistics;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.PackageUtil;
import com.venky.extension.Registry;
import com.venky.swf.db._IDatabase;
import com.venky.swf.menu._IMenuBuilder;
import com.venky.swf.path._IPath;
import com.venky.swf.views._IView;

/**
 *
 * @author venky
 */
public class Router extends AbstractHandler {

    protected Router() {
    	
    }
    
    private static Router router = new Router();
    public static Router instance(){
    	return router;
    }
    
    private ClassLoader loader = null; 
    public ClassLoader getLoader() {
    	synchronized (this) {
    		return loader;
		}
	}
	public void setLoader(ClassLoader loader) {
		synchronized (this) {
			if (this.loader != loader) {
				this.loader = loader;
				if (loader != null){
			    	clearExtensions();
					_IDatabase db = getDatabase(true);
					loadExtensions();
					db.loadFactorySettings();
					db.close();
					setMenuBuilder();
					try {
						getPathClass();
						getExceptionViewClass();
					} catch (Exception e) {
						Logger.getLogger(Router.class.getName()).log(Level.SEVERE,e.getMessage(),e);
					}
				}
			}
		}
	}
	private void setMenuBuilder(){
		String className = Config.instance().getMenuBuilderClassName(); 
		try {
			_IMenuBuilder builder = null; 
			if (className != null ){
				builder = (_IMenuBuilder) (getClass(className).newInstance());
			}
	    	Config.instance().setMenuBuilder(builder);
		}catch (Exception ex){
			throw new RuntimeException(ExceptionUtil.getRootCause(ex));
		}
	}
	public void clearExtensions(){
		Registry.instance().clearExtensions();
	}
    public void loadExtensions(){
		for (String root : Config.instance().getExtensionPackageRoots()){
			for (URL url:Config.instance().getResouceBaseUrls()){
				for (String extnClassName : PackageUtil.getClasses(url, root.replace('.', '/'))){
					try {
						getClass(extnClassName);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
        	}
        }
    }

	private _IPath createPath(String target){
    	try {
			Class<?> ipc = getPathClass();
			_IPath path = (_IPath)(ipc.getConstructor(String.class).newInstance(target));
			return path;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
    }
	
	private Class<?> getClass(String className) throws ClassNotFoundException{
		return Class.forName(className,true,getLoader());
	}
	
	private Class<?> getPathClass() throws ClassNotFoundException{
		return getClass("com.venky.swf.path.Path");
	}
	private Class<?> getExceptionViewClass() throws ClassNotFoundException {
		return getClass("com.venky.swf.views.ExceptionView");
	}
	private Class<?> getDatabaseClass() throws ClassNotFoundException{
		return getClass("com.venky.swf.db.Database");
	}
	
	private _IView createExceptionView(_IPath p, Throwable th){
		try {
			Class<?> evc = getExceptionViewClass();
			_IView ev = (_IView) evc.getConstructor(_IPath.class,Throwable.class).newInstance(p,th);
			return ev;
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	private _IDatabase getDatabase(){
		return getDatabase(false);
	}
	@SuppressWarnings("unchecked")
	private _IDatabase getDatabase(boolean migrate){
		try {
			//Database.getInstance()
			Class<_IDatabase> c = (Class<_IDatabase>)getDatabaseClass();
			_IDatabase idb = (_IDatabase)(c.getMethod("getInstance",boolean.class).invoke(c,migrate));
			return idb;
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		TimerStatistics.setEnabled(Config.instance().isTimerEnabled()); // Ensure thread has right value.
		
    	Timer timer = Timer.startTimer();
    	try {
	        HttpSession session  = request.getSession(false); 
	        _IView view = null;
	        _IView ev = null ;
	        
	        _IPath p = createPath(target);
	        p.setSession(session);
	        p.setRequest(request);
	        p.setResponse(response);
	        
	        baseRequest.setHandled(true);
	        _IDatabase db = null ;
	        try {
	        	db = getDatabase(); 
	        	db.open(p.getSessionUser());
	            view = p.invoke();
	            if (view.isBeingRedirected()){
	            	db.getCurrentTransaction().commit();
	            }
	            view.write();
	            db.getCurrentTransaction().commit();
	        }catch(Exception e){
	        	try { 
	        		db.getCurrentTransaction().rollback(e);
	        	}catch (Exception ex){
	        		ex.printStackTrace();
	        	}
	            ev = createExceptionView(p, e);
	            ev.write();
	        }finally {
	        	if (db != null ){
	        		db.close();
	        	}
	        }
    	}finally{
    		timer.stop();
    		TimerStatistics.dumpStatistics();
    	}
    }
}
