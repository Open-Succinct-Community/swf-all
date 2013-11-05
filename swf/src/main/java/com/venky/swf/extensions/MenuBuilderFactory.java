package com.venky.swf.extensions;

import com.venky.swf.menu._IMenuBuilder;
import com.venky.swf.routing.Config;

public class MenuBuilderFactory {
	private MenuBuilderFactory(){
	}
	private static Object createObject(String className){
		try {
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static MenuBuilderFactory _instance = null;
	public static MenuBuilderFactory instance(){
		if (_instance != null){
			return _instance;
		}
		synchronized (MenuBuilderFactory.class) {
			if (_instance == null){
				_instance = new MenuBuilderFactory();
			}
		}
		return _instance;
	}
    private static final String MENU_BUILDER_CLASS = "swf.menu.builder.class";

	public _IMenuBuilder getMenuBuilder(){ 
		_IMenuBuilder b =  Config.instance().getObject(_IMenuBuilder.MENU_BUILDER_OBJECT_KEY);
		if (b == null){
			b = (_IMenuBuilder) createObject(Config.instance().getProperty(MENU_BUILDER_CLASS));
			Config.instance().setObject(_IMenuBuilder.MENU_BUILDER_OBJECT_KEY,b);
		}
		return b;
	}
}
