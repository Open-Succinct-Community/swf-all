package com.venky.swf.plugins.oauth.extensions;

import java.util.Arrays;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.oauth.db.model.UserEmail;
import com.venky.swf.pm.DataSecurityFilter;

public class UserEmailParticipantExtension extends ParticipantExtension<UserEmail>{
	static {
		registerExtension(new UserEmailParticipantExtension());
	}
	
	protected UserEmailParticipantExtension() { 
		super(UserEmail.class);
	}

	@Override
	protected List<Integer> getAllowedFieldValues(User user,
			UserEmail partial, String fieldName) {
		if ("SELF_USER_ID".equalsIgnoreCase(fieldName)) {
			SequenceSet<Integer> ret = new SequenceSet<Integer>();
			ret.add(user.getId());
			return ret;
		}else if ("USER_ID".equalsIgnoreCase(fieldName)){
			if (partial.getUserId() != 0 && partial.getUser().isAccessibleBy(user)){
				return Arrays.asList(partial.getUserId());
			}else if (partial.getUserId() == 0) {
				List<User> users = DataSecurityFilter.getRecordsAccessible(User.class,user);
				return DataSecurityFilter.getIds(users);
			}
		}
		return null;
	}

}
