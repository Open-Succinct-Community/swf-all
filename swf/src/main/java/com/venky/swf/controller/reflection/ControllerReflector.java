package com.venky.swf.controller.reflection;

import java.lang.reflect.Method;
import java.util.List;

import com.venky.cache.Cache;
import com.venky.reflection.Reflector;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;

public class ControllerReflector<C extends Controller> extends Reflector<Controller,C>{
    
	private class SingleRecordActionMatcher implements MethodMatcher {
		public boolean matches(Method method) {
			return isAnnotationPresent(method,SingleRecordAction.class);
		} 
    }
	
	private List<Method> singleRecordActionMethods = null; 
    public List<Method> getSingleRecordActionMethods(){
    	if (singleRecordActionMethods == null){
    		singleRecordActionMethods = super.getMethods(new SingleRecordActionMatcher());
    	}
    	return singleRecordActionMethods;
    }


	public ControllerReflector(Class<C> reflectedClass) {
		super(reflectedClass, Controller.class);
	}

	public static <C extends Controller> ControllerReflector<C> instance(Class<C> reflectedClass){
		return new ControllerReflector<C>(reflectedClass);
	}
	
	public List<Method> getActionMethods(String actionPathElementName){
		return actionMethods.get(actionPathElementName);
	}

	public boolean isActionSecure(String actionPathElementName){
		return actionPathElementSecurity.get(actionPathElementName);
	}
	
    public boolean isSecuredActionMethod(Method m){
    	return actionMethodSecurity.get(m);
    }

	private Cache<String,List<Method>> actionMethods = new Cache<String, List<Method>>() {

		private static final long serialVersionUID = -3113151764059136919L;

		@Override
		protected List<Method> getValue(final String actionPathElement) {
			return getMethods(new MethodMatcher() {
				public boolean matches(Method method) {
					boolean matches = false;
					Class<?>[] parameterTypes = method.getParameterTypes();
					
					if (parameterTypes.length <= 1){
						matches = method.getName().equals(actionPathElement) && View.class.isAssignableFrom(method.getReturnType());
						if (matches && parameterTypes.length == 1){
							matches = (parameterTypes[0] == String.class || Path.isNumberClass(parameterTypes[0]));
						}
					}
					
					return matches;
				}
			});
		}
	};

	private Cache<String,Boolean> actionPathElementSecurity = new Cache<String, Boolean>() {
		private static final long serialVersionUID = -1166764248378859015L;

		@Override
		protected Boolean getValue(String actionPathElement) {
			List<Method> methods = getActionMethods(actionPathElement);
	    	for (Method m : methods){
	    		if (isSecuredActionMethod(m)){
	    			return true;
	    		}
	    	}
	    	return false;
		}
		
	};

    private Cache<Method,Boolean> actionMethodSecurity = new Cache<Method, Boolean>() {
		private static final long serialVersionUID = 8663665459607365588L;

		@Override
		protected Boolean getValue(Method method) {
	    	boolean requireLogin = true; 
	    	RequireLogin ur = getAnnotation(method,RequireLogin.class);
	    	
	    	if (ur != null){
	    		requireLogin = ur.value();
	    	}

	    	return requireLogin;
		}
	};
	
}
