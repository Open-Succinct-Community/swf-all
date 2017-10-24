package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class AfterModelCreateExtension<M extends Model> implements Extension{
	protected static <M extends Model> void registerExtension(AfterModelCreateExtension<M> instance){
		Registry.instance().registerExtension(getModelClass(instance).getSimpleName() +".after.create", instance);
	}
    protected static <M extends Model> void deregisterExtension(AfterModelCreateExtension<M> instance){
        Registry.instance().deregisterExtension(getModelClass(instance).getSimpleName() +".after.create", instance);
    }
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(AfterModelCreateExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		M model = (M)context[0];
		afterCreate(model);
	}
	
	public abstract void afterCreate(M model);
}
