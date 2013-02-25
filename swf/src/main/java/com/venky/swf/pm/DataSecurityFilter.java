package com.venky.swf.pm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
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
	public static <M extends Model> List<M> getRecordsAccessible(Class<M> modelClass, User by){
		return getRecordsAccessible(modelClass, by, null);
	}
	public static <M extends Model> List<M> getRecordsAccessible(Class<M> modelClass, User by, Expression condition){
		Cache<String,Map<String,List<Integer>>> pOptions = by.getParticipationOptions(modelClass);
		Expression where = new Expression(Conjunction.AND);
		
		if (condition != null){
			where.add(condition);
		}
		
		Select s = new Select().from(modelClass);
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
		Set<String> fields = new HashSet<String>();
		for (String g : pOptions.keySet()){
			fields.addAll(pOptions.get(g).keySet());
		}
		if (!anyFieldIsVirtual(fields,ref)){
			where.add(by.getDataSecurityWhereClause(ref, pOptions));
		}
		s.where(where);
		return s.execute(modelClass,new Select.AccessibilityFilter<M>(by));
	}
	
	public static SequenceSet<Integer> getIds(List<? extends _Identifiable> idables){
		SequenceSet<Integer> ret = new SequenceSet<Integer>();
		for (_Identifiable idable: idables){
			ret.add(idable.getId());
		}
		return ret;
	}
}
