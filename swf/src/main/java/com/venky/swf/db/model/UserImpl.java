package com.venky.swf.db.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
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
	public void generateApiKey(){
		StringBuilder key = new StringBuilder();
		key.append(getProxy().getId()).append(":").append(getProxy().getName()).append(":").append(getProxy().getPassword()).append(":").append(System.currentTimeMillis());
		String encryptedKey = Encryptor.encrypt(key.toString());
		getProxy().setApiKey(encryptedKey);
		getProxy().save();
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
		/**
		 * 
		 */
		private static final long serialVersionUID = -1863284571543640448L;

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
	
	private Cache<Class<? extends Model>, Set<String>> relatedTables = new Cache<Class<? extends Model>, Set<String>>() {
		private static final long serialVersionUID = 3264144367149148150L;
		@Override
		protected Set<String> getValue(Class<? extends Model> modelClass) {
			Set<String> tables = new HashSet<String>();
			load(tables,modelClass);
			return tables;
		}
		private void load(Set<String> tables, Class<? extends Model> modelClass){
			if (tables.add(ModelReflector.instance(modelClass).getTableName())){
				for (Method m : ModelReflector.instance(modelClass).getParticipantModelGetters()){ 
					@SuppressWarnings("unchecked")
					Class<? extends Model> referredModelClass = (Class<? extends Model>)m.getReturnType();
					load(tables,referredModelClass);
				}
			}
		}
	}; 
	
	public <R extends Model> Cache<String,Map<String,List<Integer>>> getParticipationOptions(Class<R> modelClass){
		Set<String> tables = new HashSet<String>(relatedTables.get(modelClass));
		tables.retainAll(Database.getInstance().getCurrentTransaction().getTablesChanged());
		Cache<Class<? extends Model>,Cache<String,Map<String,List<Integer>>>> baseParticipationOptions = Database.getInstance().getCurrentTransaction().getAttribute(this.getClass().getName() + ".getParticipationOptions" );
		if (baseParticipationOptions == null){
			baseParticipationOptions = new Cache<Class<? extends Model>, Cache<String,Map<String,List<Integer>>>>() {
				private static final long serialVersionUID = -5570691940356510299L;

				@Override
				protected Cache<String, Map<String, List<Integer>>> getValue(
						Class<? extends Model> k) {
					return getParticipationOptions(k,Database.getTable(k).newRecord());
				}
			};
			Database.getInstance().getCurrentTransaction().setAttribute(this.getClass().getName() + ".getParticipationOptions" ,baseParticipationOptions);
		}

		if (!tables.isEmpty()){
			baseParticipationOptions.remove(modelClass);
		}

		return baseParticipationOptions.get(modelClass);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Cache<String,Map<String,List<Integer>>> getParticipationOptions(Class<? extends Model> modelClass, Model model){
		Timer timer = Timer.startTimer();
		try {
			Cache<String,Map<String, List<Integer>>> mapParticipatingGroupOptions = new Cache<String, Map<String,List<Integer>>>(){

				/**
				 * 
				 */
				private static final long serialVersionUID = -6588079568930541478L;

				@Override
				protected Map<String, List<Integer>> getValue(String k) {
					return new HashMap<String, List<Integer>>();
				}
				
			};
			User user = getProxy();
			if (user.isAdmin()){
				return mapParticipatingGroupOptions;
			}
			ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
			for (Method referredModelGetter : ref.getParticipantModelGetters()){
				String referredModelIdFieldName = ref.getReferenceField(referredModelGetter);
				PARTICIPANT participant = ref.getAnnotation(ref.getFieldGetter(referredModelIdFieldName), PARTICIPANT.class);

				Map<String,List<Integer>> mapParticipatingOptions = mapParticipatingGroupOptions.get(participant.value()); 
				
				
				boolean extnFound = false;
				for (String extnPoint: getParticipationExtensionPoints(modelClass)){
					if (Registry.instance().hasExtensions(extnPoint)){
						Registry.instance().callExtensions(extnPoint, user, model,referredModelIdFieldName, mapParticipatingGroupOptions);
						extnFound = true;
						break;
					}					
				}
				
				if (!extnFound && Registry.instance().hasExtensions(User.GET_PARTICIPATION_OPTION)){
					Registry.instance().callExtensions(User.GET_PARTICIPATION_OPTION, user, modelClass, model, referredModelIdFieldName, mapParticipatingGroupOptions);
					extnFound = true;
				}
				
				if (!extnFound) {
					Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
					Integer rmid = (Integer)model.getRawRecord().get(referredModelIdFieldName);
					Model referredModel = null;
					if (rmid != null){
						referredModel = Database.getTable(referredModelClass).get(rmid);
					}
					Cache<String,Map<String,List<Integer>>> referredModelParticipatingGroupOptions = null; 
					if (referredModel == null){
						referredModelParticipatingGroupOptions = user.getParticipationOptions(referredModelClass);
					}else {
						referredModelParticipatingGroupOptions = user.getParticipationOptions(referredModelClass,referredModel);
					}
		
					ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);

					if (referredModelParticipatingGroupOptions.size() > 0){
						Set<String> fields = new HashSet<String>();
						for (String g: referredModelParticipatingGroupOptions.keySet()){
							fields.addAll(referredModelParticipatingGroupOptions.get(g).keySet());
						}
						boolean couldFilterUsingDSW = !DataSecurityFilter.anyFieldIsVirtual(fields,referredModelReflector); 
						
						Select q = new Select().from(referredModelClass);
						Select.ResultFilter filter = null;
						if (couldFilterUsingDSW){
							q.where(getDataSecurityWhereClause(referredModelReflector,referredModelParticipatingGroupOptions));
						}else {
							filter = new Select.AccessibilityFilter<Model>(user);
						}
						List<? extends Model> referables = q.execute(referredModelClass,filter);
						List<Integer> ids = new ArrayList<Integer>();
						for (Model referable:referables){
							ids.add(referable.getId());
						}
						mapParticipatingOptions.put(referredModelIdFieldName,ids);
					}
				}
			}
			
			return mapParticipatingGroupOptions;
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
		Cache<String,Map<String,List<Integer>>> participatingOptions = getParticipationOptions(modelClass,model);
		return getDataSecurityWhereClause(ref, participatingOptions);
	}
	public Expression getDataSecurityWhereClause(ModelReflector<? extends Model> ref, Cache<String,Map<String,List<Integer>>> participatingGroupOptions){
		Expression dswMandatory = new Expression(Conjunction.AND);
		
		Map<String,Expression> optionalWhere = new HashMap<String, Expression>();
		for (String participantRoleGroup: participatingGroupOptions.keySet()){
			Expression dswOptional = new Expression(Conjunction.OR);
			optionalWhere.put(participantRoleGroup, dswOptional);
			dswMandatory.add(dswOptional);
		}

		for (String participantRoleGroup : participatingGroupOptions.keySet()){
			Map<String,List<Integer>> participatingOptions = participatingGroupOptions.get(participantRoleGroup);
			Iterator<String> fieldNameIterator = participatingOptions.keySet().iterator();
			
			while (fieldNameIterator.hasNext()){
				String key = fieldNameIterator.next();
				List<Integer> values = participatingOptions.get(key);
				
				ColumnDescriptor cd = ref.getColumnDescriptor(key);
		    	if (cd.isVirtual()){
		    		continue;
		    	}
		    	
		    	Expression dsw = optionalWhere.get(participantRoleGroup);
				
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
	    			dsw.add(Expression.createExpression(cd.getName(),Operator.IN, values.toArray()));
		    	}
			}
		}
		return dswMandatory;
	}
	
	public User getSelfUser(){
		return getProxy();
	}

}
