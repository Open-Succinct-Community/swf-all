package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Select.ResultFilter;

import java.util.List;


public class 	UserParticipantExtension extends ParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, User partiallyFilledModel,
			String fieldName) {
		
		SequenceSet<Long> ret = null;
		if (fieldName.equals("COMPANY_USER_ID")){
			ret = new SequenceSet<>();
			
			User operator = (User)user;

			SequenceSet<Long> accessableCompanies = new SequenceSet<>();
			for (UserCompany uc : operator.getUserCompanies()){
				accessableCompanies.add(uc.getCompanyId());
			}

			List<UserCompany> userCompanies = new Select().from(UserCompany.class)
					.where(new Expression(ModelReflector.instance(UserCompany.class).getPool(),"COMPANY_ID", Operator.IN, accessableCompanies.toArray()))
					.execute();
			for (UserCompany uc : userCompanies){
				ret.add(uc.getUserId());
			}
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
				ret = DataSecurityFilter.getIds(partiallyFilledModel.getState().getCities());
			}else {
				ret = null ; //DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(City.class, user));
			}
		}
		return ret;
	}
}
