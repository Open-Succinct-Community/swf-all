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
		Class<M> modelClass = getModelClass(instance);
		instance.modelClass = modelClass;
		instance.ref = ModelReflector.instance(modelClass);
		Registry.instance().registerExtension(User.GET_PARTICIPATION_OPTION + "." + modelClass.getSimpleName() , instance);
	}
    protected static <M extends Model> void deregisterExtension(ParticipantExtension<M> instance){
        Class<M> modelClass = getModelClass(instance);
        instance.modelClass = modelClass;
        instance.ref = ModelReflector.instance(modelClass);
        Registry.instance().deregisterExtension(User.GET_PARTICIPATION_OPTION + "." + modelClass.getSimpleName() , instance);
    }
	
	@SuppressWarnings("unchecked")
	protected static <M extends Model> Class<M> getModelClass(ParticipantExtension<M> instance){
		ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}

	
	private Class<M> modelClass; 
	private ModelReflector<M> ref ; 
	protected ParticipantExtension(){
		
	}
	
	public ModelReflector<M> getReflector(){
		return ref;
	}
	
	public Class<M> getModelClass(){
		return modelClass;
	}
	
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		User user = (User)context[0];
		M model = (M) context[1];
		String fieldName =  (String)context[2];
		Cache<String,Map<String,List<Integer>>> participatingGroupOptions = (Cache<String,Map<String, List<Integer>>>)context[3];

		PARTICIPANT participant = getReflector().getAnnotation(getReflector().getFieldGetter(fieldName), PARTICIPANT.class);
		
		Map<String,List<Integer>> participatingOptions = participatingGroupOptions.get(participant.value());
		List<Integer> allowedValues = getAllowedFieldValues(user,model,fieldName);

		if (allowedValues != null){ 
			List<Integer> existing = participatingOptions.get(fieldName);
			if (existing == null){
				existing = new SequenceSet<Integer>();
				participatingOptions.put(fieldName, existing);
			}
			existing.addAll(allowedValues);
		}
	}

	
	/**
	 * @param user
	 * @param fieldName
	 * @return Allowed field values for passed used. <code>null</code> implies all values are allowed. 
	 */
	protected abstract List<Integer> getAllowedFieldValues(User user, M partiallyFilledModel, String fieldName);
}
