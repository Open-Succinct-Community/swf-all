package com.venky.swf.db.model.reflection;

import java.lang.reflect.Method;
import java.util.StringTokenizer;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.poi.BeanIntrospector;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector.FieldGetterMissingException;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKey;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKeyFieldDescriptor;
import com.venky.swf.db.table.Table;

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
		StringTokenizer tok = new StringTokenizer(heading, ".");
		
		String firstPart =  tok.nextToken();
		
		
		Method m = super.getGetter(firstPart);
		if (m == null) {
			String field = StringUtil.underscorize(firstPart);
			try {
				m = ref.getFieldGetter(field);
			} catch (FieldGetterMissingException ex) {
				m = null;
			}
		}
		return m;
	}
	
	protected void loadFieldsToExport(SequenceSet<String> fields, String baseFieldHeading , ModelReflector<? extends Model> referredModelReflector){
		for (UniqueKey<? extends Model> k : referredModelReflector.getUniqueKeys()){
			for (UniqueKeyFieldDescriptor<? extends Model> ukf: k.getFields()){
				if (ukf.getReferredModelReflector() == null){
					fields.add(baseFieldHeading + "." +  StringUtil.camelize(ukf.getFieldName()));
				}else {
					loadFieldsToExport(fields, baseFieldHeading + "." + StringUtil.camelize(ukf.getFieldName().substring(0,ukf.getFieldName().length() - "_ID".length())) , ukf.getReferredModelReflector());
				}
			}
		}
	}
	
	

	protected Object getValue(Model record, String fieldName){
		StringTokenizer fieldPartTokenizer = new StringTokenizer(fieldName,".");

		ModelReflector<? extends Model> ref = getReflector();
		Model current = record;		
		while (fieldPartTokenizer.hasMoreTokens()){
			String nextToken = fieldPartTokenizer.nextToken();
			if (fieldPartTokenizer.hasMoreTokens()){
				String referenceFieldName = StringUtil.underscorize(nextToken + "Id");
				Integer value = ref.get(current, referenceFieldName);
				if (value == null){
					break;
				}
				Class<? extends Model> referredModelClass = ref.getReferredModelClass(ref.getReferredModelGetterFor(ref.getFieldGetter(referenceFieldName)));
				
				Table<?> table = Database.getTable(referredModelClass);
				current = table.get(value);
				ref = table.getReflector();
			}else {
				return ref.get(current, nextToken);
			}
		}
		return null;
		
	}
	


}
