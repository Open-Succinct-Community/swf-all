package com.venky.swf.db.model;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;

public class UserImpl<M extends User> extends ModelImpl<M>{
	
	public UserImpl(Class<M> modelClass, Record record) {
		super(modelClass, record);
	}

	
	public boolean authenticate(String password){
		try {
			User user = getProxy();
			if (Registry.instance().hasExtensions(User.USER_AUTHENTICATE)){
				Registry.instance().callExtensions(User.USER_AUTHENTICATE, user,password);
			}else {
				ObjectUtil.equals(user.getPassword(),password);
			}
			return true;
		}catch (AccessDeniedException ex){
			return false;
		}
	}
	
	public <R extends Model> Map<String,List<Integer>> getParticipationOptions(Class<R> modelClass){
		User user = getProxy();
		
		Map<String, List<Integer>> mapParticipatingOptions = new HashMap<String, List<Integer>>();
		ModelReflector<R> ref = ModelReflector.instance(modelClass);
		for (Method referredModelGetter : ref.getReferredModelGetters()){
			String referredModelIdFieldName = ref.getReferredModelIdFieldName(referredModelGetter);
			Method referredModelIdGetter = ref.getFieldGetter(referredModelIdFieldName);
			if (referredModelIdGetter.isAnnotationPresent(PARTICIPANT.class)){
				if (User.class.isAssignableFrom(ref.getReferredModelClass(referredModelGetter))){
					mapParticipatingOptions.put(referredModelIdFieldName, Arrays.asList(user.getId()));
				}else {
					String extnPoint = User.GET_PARTICIPATION_OPTION + "."+ modelClass.getSimpleName();
					if (Registry.instance().hasExtensions(extnPoint)){
						Registry.instance().callExtensions(extnPoint, user, referredModelIdFieldName, mapParticipatingOptions);
					}else{
						Registry.instance().callExtensions(User.GET_PARTICIPATION_OPTION, user, modelClass, referredModelIdFieldName, mapParticipatingOptions);
					}
				}
			}
		}
		
		return mapParticipatingOptions;
	}

	public <R extends Model> String getDataSecurityWhereClause(Class<R> modelClass){
		ModelReflector<R> ref = ModelReflector.instance(modelClass);
		Map<String,List<Integer>> columnValuesMap = getParticipationOptions(modelClass);
		StringBuilder dsw = new StringBuilder();
		Iterator<String> fieldNameIterator = columnValuesMap.keySet().iterator();
		
		while (fieldNameIterator.hasNext()){
			String key = fieldNameIterator.next();
			List<Integer> values = columnValuesMap.get(key);
			
	    	ColumnDescriptor cd = ref.getColumnDescriptor(key);
	    	if (cd != null){
	    		dsw.append(cd.getName());
	    	}else{
	    		dsw.append(key);
	    	}
	    	if (values.isEmpty()){
	    		dsw.append(" is null ");
	    	}else if (values.size() == 1){
	    		dsw.append(" = ").append(values.get(0));
	    	}else {
	    		dsw.append(" in (");
	    		Iterator<Integer> valueIterator = values.iterator();
	    		while (valueIterator.hasNext()){
	    			dsw.append(valueIterator.next());
	    			if (valueIterator.hasNext()){
	    				dsw.append(",");
	    			}
	    		}
	    		dsw.append(")");
	    	}
	    	if (fieldNameIterator.hasNext()){
	    		dsw.append(" or ");
	    	}
		}
		return dsw.toString();
	}
}
