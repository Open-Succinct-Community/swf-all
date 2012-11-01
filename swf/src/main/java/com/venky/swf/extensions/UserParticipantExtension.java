package com.venky.swf.extensions;

import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;

public class UserParticipantExtension extends ParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}
	
	@Override
	protected List<Integer> getAllowedFieldValues(User user, User partial, String fieldName) {
		if ("SELF_USER_ID".equalsIgnoreCase(fieldName)) {
			SequenceSet<Integer> ret = new SequenceSet<Integer>();
			ret.add(user.getId());
			return ret.list();
		}
		return null;
	}
}
