package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class BeforeModelDestroyExtension<M extends Model> implements Extension{
	protected static <M extends Model> void registerExtension(BeforeModelDestroyExtension<M> instance){
		Registry.instance().registerExtension(getModelClass(instance).getSimpleName() +".before.destroy", instance);
	}
    protected static <M extends Model> void deregisterExtension(BeforeModelDestroyExtension<M> instance){
        Registry.instance().deregisterExtension(getModelClass(instance).getSimpleName() +".before.destroy", instance);
    }
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(BeforeModelDestroyExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}
	

	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		M model = (M)context[0];
		beforeDestroy(model);
	}
	
	public abstract void beforeDestroy(M model);
}
