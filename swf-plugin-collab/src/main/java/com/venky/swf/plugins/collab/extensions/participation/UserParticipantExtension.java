package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.pm.DataSecurityFilter;

import java.util.List;


public class UserParticipantExtension extends CompanyNonSpecificParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}

	@Override
	public List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, User partiallyFilledModel,
			String fieldName) {
		
		SequenceSet<Long> ret = null;
		User u = user.getRawRecord().getAsProxy(User.class);
		if ("SELF_USER_ID".equalsIgnoreCase(fieldName)) {
			if (!u.isStaff() || partiallyFilledModel.getId() == user.getId()){
				ret = new SequenceSet<>();
				ret.add(user.getId());
			}
		}else if (fieldName.equals("COMPANY_ID")){
			return super.getAllowedFieldValues(user,partiallyFilledModel,fieldName);
		}else if (fieldName.equals("COUNTRY_ID")){
			ret =  null ; //DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Country.class, user));
		}else if (fieldName.equals("STATE_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getCountryId())){
				ret =  DataSecurityFilter.getIds(partiallyFilledModel.getCountry().getStates());
			}else {
				ret = null ;//DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(State.class, user));
			}
		}else if (fieldName.equals("CITY_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getStateId())){
				State state =  partiallyFilledModel.getState();
				if (state != null) {
					ret = DataSecurityFilter.getIds(state.getCities());
				}else {
					ret = null;
				}
			}else {
				ret = null ; //DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(City.class, user));
			}
		}
		return ret;
	}
}
