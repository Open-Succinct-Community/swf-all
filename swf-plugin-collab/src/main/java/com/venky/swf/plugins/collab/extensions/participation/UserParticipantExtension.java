package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;


public class UserParticipantExtension extends ParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, User partiallyFilledModel,
			String fieldName) {
		
		SequenceSet<Long> ret = null;
		if (fieldName.equals("COMPANY_ID")){
			ret = new SequenceSet<>();
			
			User operator = (User)user;
			ret.add(operator.getId());
			
			SequenceSet<Long> accessableCompanies = new SequenceSet<>();
			
			for (UserCompany uc : operator.getUserCompanies()){
				accessableCompanies.add(uc.getCompanyId());
			}

		}
		return ret;
	}
}
