package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.pm.DataSecurityFilter;


public class FacilityParticipantExtension extends CompanySpecificParticipantExtension<Facility>{
	static {
		registerExtension(new FacilityParticipantExtension());
	}
	@Override
	protected List<Long> getAllowedFieldValues(User user,
			Facility partiallyFilledModel, String fieldName) {
		if (fieldName.equals("COUNTRY_ID")){
			return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Country.class, user));
		}else if (fieldName.equals("STATE_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getCountryId())){
				return DataSecurityFilter.getIds(partiallyFilledModel.getCountry().getStates());
			}else {
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(State.class, user));
			}
		}else if (fieldName.equals("CITY_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getStateId())){
				return DataSecurityFilter.getIds(partiallyFilledModel.getState().getCities());
			}else {
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(City.class, user));
			}
		} 
		return super.getAllowedFieldValues(user, partiallyFilledModel, fieldName);
	}
	
}
