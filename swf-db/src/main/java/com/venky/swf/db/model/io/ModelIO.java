package com.venky.swf.db.model.io;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.venky.core.string.StringUtil;
import com.venky.reflection.BeanIntrospector;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.ModelReflector.FieldGetterMissingException;

public class ModelIO<M extends Model> extends BeanIntrospector<M>{ 

	protected ModelIO(Class<M> beanClass) {
		super(beanClass);
		ref = ModelReflector.instance(getBeanClass());
		for (String field: ref.getFields() ){
			String attributeName = StringUtil.camelize(field);
			attributeFieldMap.put(attributeName, field);
			fieldAttributeMap.put(field, attributeName);
		}
	}
	
	private Map<String,String> fieldAttributeMap = new HashMap<String,String>();
	private Map<String,String> attributeFieldMap = new HashMap<String,String>();
	
	public Set<String> getFields(){
		return fieldAttributeMap.keySet();
	}
	
	public Set<String> getAttributes(){
		return attributeFieldMap.keySet();
	}
	
	protected String getAttributeName(String fieldName){
		return fieldAttributeMap.get(fieldName);
	}

	protected String getFieldName(String attributeName){
		return attributeFieldMap.get(attributeName);
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


	protected Method getGetter(String attributeName) {
		Method m = super.getGetter(attributeName);
		if (m == null) {
			String field = getFieldName(attributeName);
			try {
				if (field != null){
					m = ref.getFieldGetter(field);
				}
			} catch (FieldGetterMissingException ex) {
				m = null;
			}
		}
		return m;
	}
	
}
