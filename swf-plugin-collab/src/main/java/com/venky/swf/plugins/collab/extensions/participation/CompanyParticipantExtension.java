package com.venky.swf.plugins.collab.extensions.participation;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;

public class CompanyParticipantExtension extends ParticipantExtension<Company>{
	static {
		registerExtension(new CompanyParticipantExtension());
	}


	
	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Company partial , String fieldName) {
		List<Long> ret = null;
		User u = (User)user;
		if ("SELF_COMPANY_ID".equalsIgnoreCase(fieldName)){
			ret = new ArrayList<>();
			List<UserCompany> ucs = u.getUserCompanies(); 
			for (UserCompany uc :ucs){ 
				ret.add(uc.getCompanyId());
			}
			if (partial.getId() > 0 && ret.contains(partial.getId())){
				ret.add(partial.getId());
			}
		}
		return ret;
	}


}
