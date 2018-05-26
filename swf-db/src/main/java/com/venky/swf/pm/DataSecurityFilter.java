package com.venky.swf.pm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model._Identifiable;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Select;

public class DataSecurityFilter {
	public static boolean anyFieldIsVirtual(Set<String> fieldSet, ModelReflector<? extends Model> ref){
		for (String field:fieldSet){
			if (ref.isFieldVirtual(field)){
				return true;
			}
		}
		return false;
	}
	public static Set<String> getRedundantParticipationFields(Set<String> fieldSet, ModelReflector<? extends Model> ref){
		SequenceSet<String> redundant = new SequenceSet<String>();
		for (String field:fieldSet){
			PARTICIPANT p = ref.getAnnotation(ref.getFieldGetter(field), PARTICIPANT.class);
			if (p.redundant()){
				redundant.add(field);
			}
		}
		return redundant;
	}
	
	public static <M extends Model> List<M> getRecordsAccessible(Class<M> modelClass, User by){
		return getRecordsAccessible(modelClass, by, null);
	}
	public static <M extends Model> List<M> getRecordsAccessible(Class<M> modelClass, User by, Expression condition){
		return getRecordsAccessible(modelClass, by, condition, Select.MAX_RECORDS_ALL_RECORDS);
	}
	public static <M extends Model> List<M> getRecordsAccessible(Class<M> modelClass, User by, Expression condition,int maxRecords){
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);

		Cache<String,Map<String,List<Long>>> pOptions = by.getParticipationOptions(modelClass);
		Expression where = new Expression(ref.getPool(),Conjunction.AND);
		
		if (condition != null){
			where.add(condition);
		}
		
		Select s = new Select().from(modelClass);
		Set<String> fields = new HashSet<String>();
		for (String g : pOptions.keySet()){
			fields.addAll(pOptions.get(g).keySet());
		}
		fields.removeAll(getRedundantParticipationFields(fields, ref));
		
		if (!anyFieldIsVirtual(fields,ref)){
			where.add(by.getDataSecurityWhereClause(ref, pOptions));
		}
		s.where(where);
		return s.execute(modelClass,maxRecords,new Select.AccessibilityFilter<M>(by));
	}
	
	public static SequenceSet<Long> getIds(List<? extends _Identifiable> idables){
		SequenceSet<Long> ret = new SequenceSet<>();
		for (_Identifiable idable: idables){
			ret.add(idable.getId());
		}
		return ret;
	}
}
