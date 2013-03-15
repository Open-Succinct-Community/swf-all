package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.pm.DataSecurityFilter;


public class UserEmailParticipantExtension extends CompanySpecificParticipantExtension<UserEmail>{
	static {
		registerExtension(new UserEmailParticipantExtension());
	}
	
	@Override
	protected List<Integer> getAllowedFieldValues(User user,
			UserEmail partiallyFilledModel, String fieldName) {
		if (fieldName.equalsIgnoreCase("USER_ID")){
			return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(User.class, user));
		}
		return super.getAllowedFieldValues(user, partiallyFilledModel, fieldName);
	}
}
