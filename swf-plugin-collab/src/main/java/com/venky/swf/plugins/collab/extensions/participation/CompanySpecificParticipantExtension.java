package com.venky.swf.plugins.collab.extensions.participation;

import java.util.Arrays;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;

public class CompanySpecificParticipantExtension<M extends Model & CompanySpecific> extends ParticipantExtension<M>{

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, M partiallyFilledModel, String fieldName) {
		
		User u = (User)user;
		if (partiallyFilledModel != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				if (!partiallyFilledModel.getReflector().isVoid(partiallyFilledModel.getCompanyId()) && partiallyFilledModel.getCompany().isAccessibleBy(user)){
					return Arrays.asList(partiallyFilledModel.getCompanyId());
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
