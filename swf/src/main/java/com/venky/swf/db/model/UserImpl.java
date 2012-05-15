package com.venky.swf.db.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class UserImpl extends ModelImpl<User>{
	
	public UserImpl(User user) {
		super(user);
	}

	
	public boolean authenticate(String password){
		try {
			User user = getProxy();
			if (Registry.instance().hasExtensions(User.USER_AUTHENTICATE)){
				Registry.instance().callExtensions(User.USER_AUTHENTICATE, user,password);
			}else {
				return ObjectUtil.equals(user.getPassword(),password);
			}
			return true;
		}catch (AccessDeniedException ex){
			return false;
		}
	}
	
	public <R extends Model> SequenceSet<String> getParticipationExtensionPoints(Class<R> modelClass){
		SequenceSet<String> extnPoints = new SequenceSet<String>();
		ModelReflector<R> ref = ModelReflector.instance(modelClass);
		for (Class<? extends Model> inHierarchy : ref.getClassHierarchies()){
			String extnPoint = User.GET_PARTICIPATION_OPTION + "."+ inHierarchy.getSimpleName();
			extnPoints.add(extnPoint);
		}
		return extnPoints;
	}
	
	public <R extends Model> Map<String,List<Integer>> getParticipationOptions(Class<R> modelClass){
		return getParticipationOptions(modelClass,Database.getTable(modelClass).newRecord()); // For Dummy record.
	}
	
	public Map<String,List<Integer>> getParticipationOptions(Class<? extends Model> modelClass, Model model){
		Map<String, List<Integer>> mapParticipatingOptions = new HashMap<String, List<Integer>>();
		User user = getProxy();
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
		for (Method referredModelGetter : ref.getReferredModelGetters()){
			String referredModelIdFieldName = ref.getReferenceField(referredModelGetter);
			Method referredModelIdGetter = ref.getFieldGetter(referredModelIdFieldName);
			Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
			Model referredModel = null;
			
			Integer rmid = (Integer)model.getRawRecord().get(referredModelIdFieldName);
			if (rmid != null){
				referredModel = Database.getTable(referredModelClass).get(rmid);
			}
			if (referredModel == null){
				referredModel = Database.getTable(referredModelClass).newRecord(); //Dummy;
			}

			if (ref.isAnnotationPresent(referredModelIdGetter,PARTICIPANT.class)){
				boolean extnFound = false;
				for (String extnPoint: getParticipationExtensionPoints(modelClass)){
					if (Registry.instance().hasExtensions(extnPoint)){
						Registry.instance().callExtensions(extnPoint, user, model,referredModelIdFieldName, mapParticipatingOptions);
						extnFound = true;
						break;
					}					
				}
				
				if (!extnFound && Registry.instance().hasExtensions(User.GET_PARTICIPATION_OPTION)){
					Registry.instance().callExtensions(User.GET_PARTICIPATION_OPTION, user, modelClass, model, referredModelIdFieldName, mapParticipatingOptions);
					extnFound = true;
				}
				
				if (!extnFound) {
					Map<String,List<Integer>> referredModelParticipatingOptions = user.getParticipationOptions(referredModelClass,referredModel);
					if (!referredModelParticipatingOptions.isEmpty()){
						Select q = new Select().from(referredModelClass).where(getDataSecurityWhereClause(referredModelClass,referredModel));
						List<? extends Model> referables = q.execute(referredModelClass);
						List<Integer> ids = new ArrayList<Integer>();
						for (Model referable:referables){
							ids.add(referable.getId());
						}
						ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
						if (referredModelReflector.reflects(User.class)){
							ids.add(user.getId());
						}
						mapParticipatingOptions.put(referredModelIdFieldName,ids);
					}
				}
			}
		}
		
		return mapParticipatingOptions;
	}

	public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass){
		Model dummy = Database.getTable(modelClass).newRecord();
		return getDataSecurityWhereClause(modelClass, dummy);
	}
	public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass,Model model){
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
		Map<String,List<Integer>> columnValuesMap = getParticipationOptions(modelClass,model);
		
		Expression dsw = new Expression(Conjunction.OR);
		Iterator<String> fieldNameIterator = columnValuesMap.keySet().iterator();
		
		while (fieldNameIterator.hasNext()){
			String key = fieldNameIterator.next();
			List<Integer> values = columnValuesMap.get(key);
			
	    	ColumnDescriptor cd = ref.getColumnDescriptor(key);

	    	if (values.isEmpty()){
	    		dsw.add(new Expression(cd.getName(),Operator.EQ));
	    	}else if (values.size() == 1){
	    		Integer value = values.get(0);
	    		if (value == null){
	    			dsw.add(new Expression(cd.getName(),Operator.EQ));
	    		}else {
	    			dsw.add(new Expression(cd.getName(),Operator.EQ, new BindVariable(values.get(0))));
	    		}
	    	}else {
	    		List<BindVariable> parameters = new ArrayList<BindVariable>();
	    		for (Integer value: values){
	    			parameters.add(new BindVariable(value));
	    		}
	    		dsw.add(Expression.createExpression(cd.getName(),Operator.IN, parameters.toArray()));
	    	}
		}
		return dsw;
	}
}
