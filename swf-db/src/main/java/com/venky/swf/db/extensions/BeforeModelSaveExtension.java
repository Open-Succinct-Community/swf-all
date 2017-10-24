package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

public abstract class BeforeModelSaveExtension<M extends Model> implements Extension{
	protected static <M extends Model> void registerExtension(BeforeModelSaveExtension<M> instance){
		Registry.instance().registerExtension(getModelClass(instance).getSimpleName() +".before.save", instance);
	}
    protected static <M extends Model> void deregisterExtension(BeforeModelSaveExtension<M> instance){
        Registry.instance().deregisterExtension(getModelClass(instance).getSimpleName() +".before.save", instance);
    }
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(BeforeModelSaveExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}
	public String getPool(){
		return ModelReflector.instance(getModelClass(this)).getPool();
	}
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		M model = (M)context[0];
		beforeSave(model);
	}
	
	public abstract void beforeSave(M model);
}
