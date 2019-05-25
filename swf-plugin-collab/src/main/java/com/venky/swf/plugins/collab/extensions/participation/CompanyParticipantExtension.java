package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompanyParticipantExtension extends ParticipantExtension<Company>{
	static {
		registerExtension(new CompanyParticipantExtension());
	}


	
	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Company partial , String fieldName) {
		List<Long> ret = null;
		User u = (User)user;
		if ("SELF_COMPANY_ID".equalsIgnoreCase(fieldName)){
			if (u.getCompanyId() != null){
				ret = Arrays.asList(u.getCompanyId());
			}else {
				ret = new ArrayList<>();
			}
		}
		return ret;
	}


}
