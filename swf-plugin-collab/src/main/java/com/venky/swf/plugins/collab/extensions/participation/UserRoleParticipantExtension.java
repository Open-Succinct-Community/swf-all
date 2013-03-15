package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.user.UserRole;
import com.venky.swf.pm.DataSecurityFilter;


public class UserRoleParticipantExtension extends CompanySpecificParticipantExtension<UserRole>{
	static {
		registerExtension(new UserRoleParticipantExtension());
	}
	
	@Override
	protected List<Integer> getAllowedFieldValues(User user,
			UserRole partiallyFilledModel, String fieldName) {
		if (fieldName.equalsIgnoreCase("USER_ID")){
			return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(User.class, user));
		}
		return super.getAllowedFieldValues(user, partiallyFilledModel, fieldName);
	}
}
