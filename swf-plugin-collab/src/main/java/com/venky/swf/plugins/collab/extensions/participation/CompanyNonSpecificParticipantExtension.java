package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyNonSpecificParticipantExtension<M extends Model & CompanyNonSpecific> extends ParticipantExtension<M>{

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, M partiallyFilledModel, String fieldName) {
		
		User u = (User)user;
		if (partiallyFilledModel != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				List<Long> ret = new SequenceSet<>();

				if (u.getCompanyId() != null){
					ret.add(u.getCompanyId());
					ret.addAll(u.getCompany().getCustomers().stream().map(r->r.getCustomerId()).collect(Collectors.toList()));
					ret.addAll(u.getCompany().getVendors().stream().map(r->r.getVendorId()).collect(Collectors.toList()));
					ret.addAll(u.getCompany().getCreatedCompanies().stream().map(c->c.getId()).collect(Collectors.toList()));
				}
				for (UserEmail userEmail : u.getUserEmails()){
					Long companyId = userEmail.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.UserEmail.class).getCompanyId();
					if (!getReflector().isVoid(companyId )){
						ret.add(companyId);
					}
				}
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
