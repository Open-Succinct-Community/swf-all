package com.venky.swf.path;

import java.util.List;

import com.venky.cache.Cache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.annotations.model.CONTROLLER;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;

public class ControllerCache extends Cache<String,String>{
	static {
    	Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown",new Extension(){
			@Override
			public void invoke(Object... context) {
				dispose();
			}
    	});
    }
    protected ControllerCache(){
		super(0,0);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -2476891313307932956L;
	private static ControllerCache _instance = null;
	
	public static ControllerCache instance(){
		if (_instance != null) {
			return _instance;
		}
		synchronized (ControllerCache.class) {
			if (_instance == null) {
				_instance = new ControllerCache();
			}
		}
		return _instance;
	}
	public static void dispose(){
		synchronized (ControllerCache.class) {
			if (_instance != null){
				_instance.clear();
				_instance = null;
			}
		}
	}
	
    private static final String CONTROLLER_PACKAGE_ROOT = "swf.controller.package.root";
    public List<String> getControllerPackageRoots(){
    	return Config.instance().getPackageRoots(CONTROLLER_PACKAGE_ROOT);
    }

	@Override
	protected String getValue(String k) {
		String clazzName = null;
		for (String controllerPackageRoot: getControllerPackageRoots()){
            clazzName = controllerPackageRoot + "." + StringUtil.camelize(k) + "Controller";
            if (Path.getClass(clazzName) != null) {
                break;
            }
            clazzName = null;
		}
		if (clazzName == null){
            Class<? extends Model> modelClass = Path.getModelClass(k);
            if (modelClass != null){
            	ModelReflector<?> ref = ModelReflector.instance(modelClass);
            	CONTROLLER controller = ref.getAnnotation(CONTROLLER.class);
            	if (controller != null){
            		clazzName = controller.value();
            	}
            	if (ObjectUtil.isVoid(clazzName)){
            		clazzName = ModelController.class.getName();
            	}
            }
        }
        return clazzName;
	}

}
