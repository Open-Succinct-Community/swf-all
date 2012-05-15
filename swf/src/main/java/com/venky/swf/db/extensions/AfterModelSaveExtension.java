package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

public abstract class AfterModelSaveExtension<M extends Model> implements Extension{
	protected static <M extends Model> void registerExtension(AfterModelSaveExtension<M> instance){
		Registry.instance().registerExtension(getModelClass(instance).getSimpleName() +".after.save", instance);
	}
	
	protected static <M extends Model> Class<M> getModelClass(AfterModelSaveExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}

	public void invoke(Object... context) {
		M model = (M)context[0];
		afterSave(model);
	}
	
	public abstract void afterSave(M model);
}
