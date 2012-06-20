package com.venky.swf.plugins.collab.extensions.participation;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

public class CompanyParticipantExtension extends ParticipantExtension<Company>{
	static {
		registerExtension(new CompanyParticipantExtension());
	}

	protected CompanyParticipantExtension() {
		super(Company.class);
	}

	
	@Override
	protected List<Integer> getAllowedFieldValues(com.venky.swf.db.model.User user, Company partial , String fieldName) {
		List<Integer> ret = null;
		User u = (User)user;
		if ("SELF_COMPANY_ID".equalsIgnoreCase(fieldName)){
			ret = new ArrayList<Integer>();
			ret.add(u.getCompanyId());
			if (partial.getId() > 0 && partial.getCreatorUserId() == user.getId()){
				ret.add(partial.getId());
			}
		}else if ("CREATOR_USER_ID".equalsIgnoreCase(fieldName)){
			if (partial.getId() == 0 ){
				ret = new ArrayList<Integer>();
				ret.add(user.getId());
			}
		}
		
		return ret;
	}


}
