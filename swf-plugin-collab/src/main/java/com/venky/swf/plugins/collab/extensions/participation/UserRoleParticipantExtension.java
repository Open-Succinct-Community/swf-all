package com.venky.swf.plugins.collab.extensions.participation;

import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.pm.DataSecurityFilter;


public class UserRoleParticipantExtension extends ParticipantExtension<UserRole>{
	static {
		registerExtension(new UserRoleParticipantExtension());
	}
	
	@Override
	protected List<Long> getAllowedFieldValues(User user,
			UserRole partiallyFilledModel, String fieldName) {
		SequenceSet<Long> ret = null;
		if (fieldName.equalsIgnoreCase("USER_ID")){
			if (!partiallyFilledModel.getReflector().isVoid(partiallyFilledModel.getUserId())){
				ret = new SequenceSet<>();
				if (partiallyFilledModel.getUserId() == user.getId() || partiallyFilledModel.getUser().isAccessibleBy(user)) {
					ret.add(partiallyFilledModel.getUserId());
				}
			}else if (!user.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.User.class).isStaff()){
				ret = DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(com.venky.swf.plugins.collab.db.model.user.User.class, user));
			}
		}
		return ret;

	}
}
