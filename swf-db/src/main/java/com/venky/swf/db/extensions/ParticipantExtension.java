package com.venky.swf.db.extensions;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;

public abstract class ParticipantExtension<M extends Model> implements Extension{

	protected static <M extends Model> void registerExtension(ParticipantExtension<M> instance){
		Registry.instance().registerExtension(User.GET_PARTICIPATION_OPTION + "." + instance.getModelClass().getSimpleName() , instance);
	}
    protected static <M extends Model> void deregisterExtension(ParticipantExtension<M> instance){
        Registry.instance().deregisterExtension(User.GET_PARTICIPATION_OPTION + "." + instance.getModelClass().getSimpleName() , instance);
    }
	
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(ParticipantExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}


	
	private ModelReflector<M> ref = null;
	private Class<M> modelClass = null;
	protected ParticipantExtension(){
		
	}
	
	public ModelReflector<M> getReflector(){
		if (ref == null) {
			ref =  ModelReflector.instance(getModelClass());
		}
		return ref;
	}


	protected Class<M> getModelClass(){
		if (modelClass == null) {
			modelClass = getModelClass(this);
		}
		return modelClass;
	}
	
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		User user = (User)context[0];
		M model = (M) context[1];
		String fieldName =  (String)context[2];
		Cache<String,Map<String,List<Long>>> participatingGroupOptions = (Cache<String,Map<String, List<Long>>>)context[3];

		PARTICIPANT participant = getReflector().getAnnotation(getReflector().getFieldGetter(fieldName), PARTICIPANT.class);
		
		Map<String,List<Long>> participatingOptions = participatingGroupOptions.get(participant.value());
		List<Long> allowedValues = getAllowedFieldValues(user,model,fieldName);

		if (allowedValues != null){
			List<Long> ret = new SequenceSet<>();
			ret.addAll(allowedValues);
			allowedValues = ret;

			if (getReflector().getColumnDescriptor(fieldName).isNullable()){
				allowedValues.add(null);
			}
			List<Long> existing = participatingOptions.get(fieldName);
			if (existing == null && !participatingOptions.containsKey(fieldName)){
				existing = new SequenceSet<Long>();
				participatingOptions.put(fieldName, existing);
			}
			if (existing != null) {
				existing.addAll(allowedValues);
			}
		}else {
			participatingOptions.put(fieldName, null);
		}
	}

	
	/**
	 * @param user
	 * @param fieldName
	 * @return Allowed field values for passed used. <code>null</code> implies all values are allowed. 
	 */
	protected abstract List<Long> getAllowedFieldValues(User user, M partiallyFilledModel, String fieldName);
}
