package com.venky.swf.db.annotations.model.validations;

import java.lang.reflect.Proxy;

import com.venky.core.util.MultiException;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelInvocationHandler;

public abstract class ModelValidator  {
	public ModelValidator(){
	}
	
	public <M extends Model> boolean isValid(M m , MultiException modelValidationException){
		ModelInvocationHandler h = (ModelInvocationHandler)Proxy.getInvocationHandler(m);
		return isValid(h.getReflector(),m, modelValidationException);
	}
	
	protected abstract <M extends Model>  boolean isValid(ModelReflector<M> reflector, M m, MultiException modelValidationException);
}
