package com.venky.swf.db.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.pm.DataSecurityFilter;
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
	
	
	private Cache<Class<? extends Model>, SequenceSet<String>> participantExtensionPointsCache = new Cache<Class<? extends Model>, SequenceSet<String>>() {
		@Override
		protected SequenceSet<String> getValue(Class<? extends Model> modelClass) {
			SequenceSet<String> extnPoints = new SequenceSet<String>();
			ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
			for (Class<? extends Model> inHierarchy : ref.getClassHierarchies()){
				String extnPoint = User.GET_PARTICIPATION_OPTION + "."+ inHierarchy.getSimpleName();
				extnPoints.add(extnPoint);
			}
			return extnPoints;
		}
	};
	public <R extends Model> SequenceSet<String> getParticipationExtensionPoints(Class<R> modelClass){
		return participantExtensionPointsCache.get(modelClass);
	}
	
	public <R extends Model> Map<String,List<Integer>> getParticipationOptions(Class<R> modelClass){
		return getParticipationOptions(modelClass,Database.getTable(modelClass).newRecord()); // For Dummy record.
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String,List<Integer>> getParticipationOptions(Class<? extends Model> modelClass, Model model){
		Timer timer = Timer.startTimer();
		try {
			Map<String, List<Integer>> mapParticipatingOptions = new HashMap<String, List<Integer>>();
			User user = getProxy();
			if (user.isAdmin()){
				return mapParticipatingOptions;
			}
			ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
			for (Method referredModelGetter : ref.getParticipantModelGetters()){
				String referredModelIdFieldName = ref.getReferenceField(referredModelGetter);

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
					Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
					Integer rmid = (Integer)model.getRawRecord().get(referredModelIdFieldName);
					Model referredModel = null;
					if (rmid != null){
						referredModel = Database.getTable(referredModelClass).get(rmid);
					}
					if (referredModel == null){
						referredModel = Database.getTable(referredModelClass).newRecord(); //Dummy;
					}
		
					Map<String,List<Integer>> referredModelParticipatingOptions = user.getParticipationOptions(referredModelClass,referredModel);
					ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
					if (!referredModelParticipatingOptions.isEmpty()){
						boolean couldFilterUsingDSW = !DataSecurityFilter.anyFieldIsVirtual(referredModelParticipatingOptions.keySet(),referredModelReflector); 
						
						Select q = new Select().from(referredModelClass);
						Select.ResultFilter filter = null;
						if (couldFilterUsingDSW){
							q.where(getDataSecurityWhereClause(referredModelReflector,referredModelParticipatingOptions));
						}else {
							filter = new Select.AccessibilityFilter<Model>(user);
						}
						List<? extends Model> referables = q.execute(referredModelClass,filter);
						List<Integer> ids = new ArrayList<Integer>();
						for (Model referable:referables){
							ids.add(referable.getId());
						}
						/* VENKY TODO  Check why this at all.!! 
						if (referredModelReflector.reflects(User.class)){
							ids.add(user.getId());
						}*/
						mapParticipatingOptions.put(referredModelIdFieldName,ids);
					}
				}
			}
			
			return mapParticipatingOptions;
		}finally{
			timer.stop();
		}
	}
	
	public boolean isAdmin(){
		return getProxy().getId() == 1;
	}
	

	public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass){
		Model dummy = Database.getTable(modelClass).newRecord();
		return getDataSecurityWhereClause(modelClass, dummy);
	}
	
	public Expression getDataSecurityWhereClause(Class<? extends Model> modelClass,Model model){
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
		Map<String,List<Integer>> participatingOptions = getParticipationOptions(modelClass,model);
		return getDataSecurityWhereClause(ref, participatingOptions);
	}
	public Expression getDataSecurityWhereClause(ModelReflector<? extends Model> ref, Map<String,List<Integer>> participatingOptions){
		Expression dsw = new Expression(Conjunction.OR);
		Iterator<String> fieldNameIterator = participatingOptions.keySet().iterator();
		
		while (fieldNameIterator.hasNext()){
			String key = fieldNameIterator.next();
			List<Integer> values = participatingOptions.get(key);
			
	    	ColumnDescriptor cd = ref.getColumnDescriptor(key);
	    	if (cd.isVirtual()){
	    		continue;
	    	}
	    	if (values.isEmpty()){
	    		dsw.add(new Expression(cd.getName(),Operator.EQ));
	    	}else if (values.size() == 1){
	    		Integer value = values.get(0);
	    		if (value == null){
	    			dsw.add(new Expression(cd.getName(),Operator.EQ));
	    		}else {
	    			dsw.add(new Expression(cd.getName(),Operator.EQ, values.get(0)));
	    		}
	    	}else {
	    		int indexOfNull = values.indexOf(null);
	    		if (indexOfNull >= 0){
	    			values.remove(indexOfNull);
	    			dsw.add(new Expression(cd.getName(),Operator.EQ));
	    		}
	    		dsw.add(Expression.createExpression(cd.getName(),Operator.IN, values.toArray()));
	    	}
		}
		return dsw;
	}
	
	public User getSelfUser(){
		return getProxy();
	}

}
