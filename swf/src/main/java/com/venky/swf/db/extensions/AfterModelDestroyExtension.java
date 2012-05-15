package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class AfterModelDestroyExtension<M extends Model> implements Extension{
	
	protected static <M extends Model> void registerExtension(AfterModelDestroyExtension<M> instance){
		Registry.instance().registerExtension(getModelClass(instance).getSimpleName() +".after.destroy", instance);
	}
	
	protected static <M extends Model> Class<M> getModelClass(AfterModelDestroyExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}
	
	public void invoke(Object... context) {
		M model = (M)context[0];
		afterDestroy(model);
	}
	
	public abstract void afterDestroy(M model);
}
