package com.venky.swf.db.model.reflection;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;

public class ModelWriter<M extends Model> {
	
	Class<M> modelClass = null;
	ModelReflector<M> ref = null; 
	public ModelWriter(Class<M> modelClass){
		this.modelClass = modelClass;
		this.ref = ModelReflector.instance(modelClass); 
	}
	
	public ModelReflector<M> getReflector(){
		return ref;
	}
	public void write (List<M> records,PrintWriter w){ 
		write(records,w,ref.getFields());
	}
	public void write (List<M> records,PrintWriter w, List<String> fields){
    	ModelReflector ref = getReflector();
    	
    	Iterator<String> fi = fields.iterator();
    	
    	HashMap<String, Class<? extends Model>> referedModelMap = new HashMap<String,Class<? extends Model>>();
    	while(fi.hasNext()){
    		String fieldName = fi.next();
    		Method getter = ref.getFieldGetter(fieldName);
			Method referredModelGetter = ref.getReferredModelGetterFor(getter);
			if (referredModelGetter != null ){
				w.print(referredModelGetter.getName().substring("get".length()));
				referedModelMap.put(fieldName, ref.getReferredModelClass(referredModelGetter));
			}else {
				w.print(StringUtil.camelize(fieldName));
			}
    		if (fi.hasNext()){
    			w.print("\t");
    		}else {
    	    	w.println();
    		}
    	}
    	
    	for (M m: records){
    		fi = fields.iterator();
    		while (fi.hasNext()){
    			String f = fi.next();
    			Object value = ref.get(m, f);
    			String sValue = null ;
    			if (value != null ){
        			TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(value.getClass()).getTypeConverter();
    				sValue = converter.toString(value);
        			if (referedModelMap.get(f) != null){
        				Class<? extends Model> referredModelClass = referedModelMap.get(f);
        				ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
    					Model referred = Database.getTable(referredModelClass).get(((Number)converter.valueOf(value)).intValue());
    					
    					value = referred.getRawRecord().get(referredModelReflector.getDescriptionColumn());
    					sValue = StringUtil.valueOf(referred.getRawRecord().get(referredModelReflector.getDescriptionColumn()));
        			}
    			}

    			if (!ObjectUtil.isVoid(sValue)){
    				if (String.class.isAssignableFrom(value.getClass())){
    					w.print("\"" + sValue  + "\"");
    				}else {
    					w.print(sValue);
    				}
    			}
    			if (fi.hasNext()){
    				w.print("\t");
    			}else {
    				w.println();
    			}
			}
    	}

	}

}
