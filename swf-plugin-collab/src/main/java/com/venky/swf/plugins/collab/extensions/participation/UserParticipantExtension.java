package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.plugins.collab.db.model.user.User;


public class UserParticipantExtension extends CompanySpecificParticipantExtension<User>{
	static {
		registerExtension(new UserParticipantExtension());
	}
}
