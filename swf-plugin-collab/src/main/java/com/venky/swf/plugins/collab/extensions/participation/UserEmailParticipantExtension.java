package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.pm.DataSecurityFilter;


public class UserEmailParticipantExtension extends ParticipantExtension<UserEmail>{
	static {
		registerExtension(new UserEmailParticipantExtension());
	}
	
	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user,
			UserEmail partiallyFilledModel, String fieldName) {
		
		if (fieldName.equalsIgnoreCase("USER_ID")){
			return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(User.class, user));
		}
		return null;
	}
}
