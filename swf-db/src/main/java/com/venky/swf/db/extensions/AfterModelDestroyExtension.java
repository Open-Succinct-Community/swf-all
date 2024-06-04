package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class AfterModelDestroyExtension<M extends Model> implements Extension{
	
	protected static <M extends Model> void registerExtension(AfterModelDestroyExtension<M> instance){
		Registry.instance().registerExtension(instance.getModelClass().getSimpleName() +".after.destroy", instance);
	}
    protected static <M extends Model> void deregisterExtension(AfterModelDestroyExtension<M> instance){
        Registry.instance().deregisterExtension(instance.getModelClass().getSimpleName() +".after.destroy", instance);
    }
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(AfterModelDestroyExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}
	
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		M model = (M)context[0];
		afterDestroy(model);
	}
	protected Class<M> getModelClass(){
		return getModelClass(this);
	}
	public abstract void afterDestroy(M model);
}
