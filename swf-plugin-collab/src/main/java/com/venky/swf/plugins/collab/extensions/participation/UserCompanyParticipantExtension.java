package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.pm.DataSecurityFilter;

import java.util.List;


public class UserCompanyParticipantExtension extends CompanySpecificParticipantExtension<UserCompany> {
	static {
		registerExtension(new UserCompanyParticipantExtension());
	}

}
