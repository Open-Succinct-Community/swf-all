package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;

import java.util.List;


public class UserEmailParticipantExtension extends ParticipantExtension<UserEmail>{
	static {
		registerExtension(new UserEmailParticipantExtension());
	}
	
	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user,
			UserEmail partiallyFilledModel, String fieldName) {

		SequenceSet<Long> ret = null;
		if (fieldName.equalsIgnoreCase("USER_ID")){
			if (!partiallyFilledModel.getReflector().isVoid(partiallyFilledModel.getUserId())){
				ret = new SequenceSet<>();
				if (partiallyFilledModel.getUserId() == user.getId() || partiallyFilledModel.getUser().isAccessibleBy(user)) {
					ret.add(partiallyFilledModel.getUserId());
				}
			}else if (!user.getRawRecord().getAsProxy(User.class).isStaff()){
				ret  = new SequenceSet<>();
                                ret.add(user.getId());
			}
		}else if (fieldName.equalsIgnoreCase("COMPANY_ID")){
			return user.getRawRecord().getAsProxy(User.class).getCompanyIds();
		}
		return ret;
	}
}
