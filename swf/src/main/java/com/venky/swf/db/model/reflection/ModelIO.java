package com.venky.swf.db.model.reflection;

import java.lang.reflect.Method;

import com.venky.core.string.StringUtil;
import com.venky.poi.BeanIntrospector;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector.FieldGetterMissingException;

public class ModelIO<M extends Model> extends BeanIntrospector<M>{
	protected ModelIO(Class<M> modelClass) {
		super(modelClass);
		ref = ModelReflector.instance(getBeanClass());
	}

	private ModelReflector<M> ref = null;
	protected ModelReflector<M> getReflector(){
		return ref;
	}

	protected M createInstance() {
		return Database.getTable(getBeanClass()).newRecord();
	}

	protected static enum GetterType {
		FIELD_GETTER, REFERENCE_MODEL_GETTER, UNKNOWN_GETTER,
	}


	protected Method getGetter(String heading) {
		Method m = super.getGetter(heading);
		if (m == null) {
			String field = StringUtil.underscorize(heading);
			try {
				m = ref.getFieldGetter(field);
			} catch (FieldGetterMissingException ex) {
				m = null;
			}
		}
		return m;
	}


}
