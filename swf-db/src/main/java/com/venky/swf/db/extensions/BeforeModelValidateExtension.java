package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class BeforeModelValidateExtension<M extends Model> implements Extension{
	protected static <M extends Model> void registerExtension(BeforeModelValidateExtension<M> instance){
		Registry.instance().registerExtension(instance.getModelClass().getSimpleName() +".before.validate", instance);
	}
    protected static <M extends Model> void deregisterExtension(BeforeModelValidateExtension<M> instance){
        Registry.instance().deregisterExtension(instance.getModelClass().getSimpleName() +".before.validate", instance);
    }
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(BeforeModelValidateExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		M model = (M)context[0];
		beforeValidate(model);
	}
	protected Class<M> getModelClass(){
		return getModelClass(this);
	}
	public abstract void beforeValidate(M model);
}
