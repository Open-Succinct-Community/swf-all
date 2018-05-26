package com.venky.swf.plugins.collab.extensions.participation;

import java.util.Arrays;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;

public class CompanySpecificParticipantExtension<M extends Model> extends ParticipantExtension<M>{

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, M partiallyFilledModel, String fieldName) {
		
		CompanySpecific cs = null;
		User u = (User)user;
		if (CompanySpecific.class.isInstance(partiallyFilledModel)){
			cs = (CompanySpecific)partiallyFilledModel;
		}
		if (cs != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				if (cs.getCompanyId() != null && cs.getCompany().isAccessibleBy(user)){
					return Arrays.asList(cs.getCompanyId());
				}else {
					List<UserCompany> ucs = u.getUserCompanies();
					SequenceSet<Long> ids =  new SequenceSet<>();
					for (UserCompany uc:ucs){ 
						ids.add(uc.getCompanyId());
					}
					return ids;
				}
			}
		}
		return null;
	}

}
