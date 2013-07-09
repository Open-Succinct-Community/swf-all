package com.venky.swf.plugins.collab.extensions.participation;

import java.util.Arrays;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.pm.DataSecurityFilter;

public class CompanySpecificParticipantExtension<M extends Model> extends ParticipantExtension<M>{

	@Override
	protected List<Integer> getAllowedFieldValues(User user,
			M partiallyFilledModel, String fieldName) {
		CompanySpecific cs = null;
		if (CompanySpecific.class.isInstance(partiallyFilledModel)){
			cs = (CompanySpecific)partiallyFilledModel;
		}
		if (cs != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				if (cs.getCompanyId() != null && cs.getCompany().isAccessibleBy(user)){
					return Arrays.asList(cs.getCompanyId());
				}else {
					SequenceSet<Integer> ids =  DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Company.class,user));
					if (!getReflector().isFieldMandatory(fieldName)){
						ids.add(null);
					}
					return ids;
				}
			}else if ("COMPANY_CREATOR_USER_ID".equalsIgnoreCase(fieldName)){
				return Arrays.asList(user.getId());
			}
		}
		return null;
	}

}
