package com.venky.swf.db.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;

public abstract class ParticipantExtension<M extends Model> implements Extension{

	protected static <M extends Model> void registerExtension(ParticipantExtension<M> instance){
		Registry.instance().registerExtension(User.GET_PARTICIPATION_OPTION + "." + instance.getModelClass().getSimpleName(), instance);
	}
	
	private final Class<M> modelClass; 
	private final ModelReflector ref ; 
	protected ParticipantExtension(Class<M> modelClass){
		this.modelClass = modelClass; 
		this.ref = ModelReflector.instance(modelClass);
	}
	
	public ModelReflector getReflector(){
		return ref;
	}
	
	public Class<M> getModelClass(){
		return modelClass;
	}
	
	public void invoke(Object... context) {
		User user = (User)context[0];
		M model = (M) context[1];
		String fieldName =  (String)context[2];
		Map<String,List<Integer>> participatingOptions = (Map<String, List<Integer>>)context[3];
		List<Integer> allowedValues = getAllowedFieldValues(user,model,fieldName);
		if (allowedValues != null){ 
			List<Integer> existing = participatingOptions.get(fieldName);
			if (existing == null){
				existing = new ArrayList<Integer>();
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
