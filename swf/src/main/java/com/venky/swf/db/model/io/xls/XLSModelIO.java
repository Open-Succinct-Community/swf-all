package com.venky.swf.db.model.io.xls;

import java.lang.reflect.Method;
import java.util.StringTokenizer;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelIO;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKey;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKeyFieldDescriptor;
import com.venky.swf.db.table.Table;

public class XLSModelIO<M extends Model>  extends ModelIO<M>{
	
	protected XLSModelIO(Class<M> beanClass) {
		super(beanClass);
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
	
	

	protected Object getValue(Model record, String headingName){
		StringTokenizer fieldPartTokenizer = new StringTokenizer(headingName,".");

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
				return ref.get(current, StringUtil.underscorize(nextToken));
			}
		}
		return null;
		
	}
	
	protected Method getGetter(String heading) {
		StringTokenizer tok = new StringTokenizer(heading, ".");
		String firstPart =  tok.nextToken();
		return super.getGetter(firstPart);
	}



}
