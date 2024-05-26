package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.pm.DataSecurityFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyNonSpecificParticipantExtension<M extends Model & CompanyNonSpecific> extends ParticipantExtension<M>{

	@Override
	public List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, M partiallyFilledModel, String fieldName) {
		
		User u = (User)user;
		if (partiallyFilledModel != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				List<Long> ret = u.getCompanyIds();
				return ret;
			}else if ("USER_ID".equalsIgnoreCase(fieldName)){
				if (u.isStaff()) {
					return  null;
				}else {
					return Arrays.asList(u.getId());
				}
			}
		}
		return null;
	}
}
