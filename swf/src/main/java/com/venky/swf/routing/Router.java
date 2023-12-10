/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;


import com.venky.core.log.ExtendedLevel;
import com.venky.core.util.PackageUtil;
import com.venky.extension.Registry;
import com.venky.swf.db._IDatabase;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.eventloop.jetty._HttpCoreEvent;
import com.venky.swf.views._IView;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 *
 * @author venky
 */
public class Router extends AbstractHandler {

    protected Router() {
    	
    }

	@Override
	public void handle(String target, Request request, jakarta.servlet.http.HttpServletRequest httpServletRequest, jakarta.servlet.http.HttpServletResponse httpServletResponse) throws IOException, jakarta.servlet.ServletException {
		jakarta.servlet.AsyncContext context = request.startAsync(httpServletRequest,httpServletResponse);
		context.setTimeout(0);
		try {
			_HttpCoreEvent coreEvent = (_HttpCoreEvent) getClass("com.venky.swf.plugins.background.eventloop.jetty.HttpCoreEvent").getConstructor(String.class, jakarta.servlet.AsyncContext.class).newInstance(target, context);
			context.start(coreEvent);
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}

	private static Router router = null;
    private static Object mutex = new Object();
    public static Router instance(){
    	if (router != null ){
    		return router;
		}
    	synchronized (mutex){
    		if (router == null){
    			router = new Router();
				Runtime.getRuntime().addShutdownHook(new Thread(() -> router.shutDown()));
			}
		}
    	return router;
    }
    
    private ClassLoader loader = null;
    public ClassLoader getLoader() {
    	synchronized (this) {
    		return loader;
		}
	}
    private void callShutdownExtensions(){
    	Registry.instance().callExtensions("com.venky.swf.routing.Router.shutdown");
    }
    public void shutDown(){
    	callShutdownExtensions();
    }
	public void setLoader(ClassLoader loader) {
		synchronized (this) {
			if (this.loader != loader) {
				shutDown();
		    	clearExtensions();
		    	if (this.loader != null){
		    		disposeDatabase();
		    	}
				this.loader = loader;
				if (loader != null){
			    	try {
			    	    ExtendedLevel.TIMER.intValue();

			    		InputStream is = this.loader.getResourceAsStream("config/logger.properties");
			    		if (is != null){
			    			LogManager.getLogManager().readConfiguration(is);
			    		}else {
			    			Config.instance().getLogger(Router.class.getName()).info("Logging not configured! using defaults");
			    		}
					} catch (Exception e1) {
						Config.instance().getLogger(Router.class.getName()).info("config/logger.properties not configured! using defaults");
					}
					_IDatabase db = getDatabase(true);
					loadExtensions();
					try {
						db.loadFactorySettings();
						db.getCurrentTransaction().commit();
					}catch (Exception ex){
						try {
							db.getCurrentTransaction().rollback(ex);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} finally{
						db.close();
					}
					try {
						getPathClass();
						getExceptionViewClass();
					} catch (Exception e) {
						Config.instance().getLogger(Router.class.getName()).log(Level.SEVERE,e.getMessage(),e);
					}
				}
			}
		}
	}
	public void clearExtensions(){
		Registry.instance().clearExtensions();
	}
    public void loadExtensions(){
		for (String root : Config.instance().getExtensionPackageRoots()){
			for (URL url:Config.instance().getResourceBaseUrls()){
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

	public  _IPath createPath(String target){
    	try {
			Class<?> ipc = getPathClass();
			_IPath path = (_IPath)(ipc.getConstructor(String.class).newInstance(target));
			return path;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
    }
	
	public Class<?> getClass(String className) throws ClassNotFoundException{
		return Class.forName(className,true,getLoader());
	}
	
	private Class<?> getPathClass() throws ClassNotFoundException{
		return getClass("com.venky.swf.path.Path");
	}
	private Class<?> getExceptionViewClass() throws ClassNotFoundException {
		return getClass("com.venky.swf.views.ExceptionView");
	}
	private Class<?> getRedirectorViewClass() throws ClassNotFoundException {
		return getClass("com.venky.swf.views.RedirectorView");
	}
	private Class<?> getDatabaseClass() throws ClassNotFoundException{
		return getClass("com.venky.swf.db.Database");
	}
	public _IView createRedirectorView(_IPath p,String url){
		try {
			Class<?> evc = getRedirectorViewClass();
			_IView ev = (_IView) evc.getConstructor(_IPath.class).newInstance(p);
			Method m = evc.getMethod("setRedirectUrl", String.class);
			m.invoke(ev, url);
			return ev;
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	public _IView createExceptionView(_IPath p, Throwable th){
		try {
			Class<?> evc = getExceptionViewClass();
			_IView ev = (_IView) evc.getConstructor(_IPath.class,Throwable.class).newInstance(p,th);
			return ev;
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	public _IDatabase getDatabase(){
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
	
	@SuppressWarnings("unchecked")
	private void disposeDatabase(){
		try {
			//Database.getInstance()
			Class<_IDatabase> c = (Class<_IDatabase>)getDatabaseClass();
			c.getMethod("dispose").invoke(c);
			c = null;
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}




	public void reset() {
    	try {
			if (Config.instance().isDevelopmentEnvironment()){
				setLoader(new SWFClassLoader(getClass().getClassLoader()));
			}
		}catch (Exception ex){
    		throw new RuntimeException(ex);
		}
	}
}
