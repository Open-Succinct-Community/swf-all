package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
		}else if ("CUSTOMER_ID".equalsIgnoreCase(fieldName)){
			if (partial.getId() > 0){
				ret = new SequenceSet<>();
				ret.addAll(partial.getCustomers().stream().map(r->r.getCustomerId()).collect(Collectors.toList()));
			}else {
				return new ArrayList<>();
			}
		}else if ("VENDOR_ID".equalsIgnoreCase(fieldName)){
			if (partial.getId() > 0){
				ret = new SequenceSet<>();
				ret.addAll(partial.getVendors().stream().map(r->r.getVendorId()).collect(Collectors.toList()));
			}else {
				return new ArrayList<>();
			}
		}else if ("CREATOR_COMPANY_ID".equalsIgnoreCase(fieldName)){
			if (u.getCompanyId() != null){
				return Arrays.asList(u.getCompanyId());
			}else if (!partial.getReflector().isVoid(partial.getId()) && partial.getCreatorCompanyId() == null){
				return Arrays.asList(partial.getId());
			}else {
				ret = new ArrayList<>();
				ret.add(null);
			}
		}
		return ret;
	}


}
