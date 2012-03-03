package com.venky.swf.controller.reflection;

import com.venky.reflection.Reflector;
import com.venky.swf.controller.Controller;

public class ControllerReflector<C extends Controller> extends Reflector<Controller,C>{

	public ControllerReflector(Class<C> reflectedClass, Class<Controller> upperBoundClass) {
		super(reflectedClass, upperBoundClass);
	}

	
}
