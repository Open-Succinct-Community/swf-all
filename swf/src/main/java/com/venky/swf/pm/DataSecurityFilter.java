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
		Cache<String,Map<String,List<Integer>>> pOptions = by.getParticipationOptions(modelClass);
		Select s = new Select().from(modelClass);
		ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
		Set<String> fields = new HashSet<String>();
		for (String g : pOptions.keySet()){
			fields.addAll(pOptions.get(g).keySet());
		}
		if (!anyFieldIsVirtual(fields,ref)){
			s.where(by.getDataSecurityWhereClause(modelClass));
		}
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
