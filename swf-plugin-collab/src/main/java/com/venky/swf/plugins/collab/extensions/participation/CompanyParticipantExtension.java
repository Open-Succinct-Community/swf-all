package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.participants.admin.CompanyRelationship;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

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
			ret = getAssociatedCompanyIds(u);
		}else if ("CUSTOMER_ID".equalsIgnoreCase(fieldName)){
			if (partial.getId() > 0){
				ret = new SequenceSet<>();
				ret.addAll(partial.getCustomers().stream().map(CompanyRelationship::getCustomerId).collect(Collectors.toList()));
			}else {
				return new ArrayList<>();
			}
		}else if ("VENDOR_ID".equalsIgnoreCase(fieldName)){
			if (partial.getId() > 0){
				ret = new SequenceSet<>();
				ret.addAll(partial.getVendors().stream().map(CompanyRelationship::getVendorId).collect(Collectors.toList()));
			}else {
				ret = new ArrayList<>();
			}
		}else if ("CREATOR_COMPANY_ID".equalsIgnoreCase(fieldName)){
			ret = getAssociatedCompanyIds(u);
		}else if ("ANY_USER_ID".equalsIgnoreCase(fieldName)){
			ret = Arrays.asList(user.getId());
		}else {
			ret = new ArrayList<>();
		}
		return ret;
	}

	public List<Long> getAssociatedCompanyIds(User u){
		/*
		// How is this different from u.getCompanyIds()
		List<Long> ret = new SequenceSet<>();
		if (u.getCompanyId() != null){
			ret.add(u.getCompanyId());
		}

		for (com.venky.swf.db.model.UserEmail ue : u.getUserEmails()){
			UserEmail userEmail = ue.getRawRecord().getAsProxy(UserEmail.class);
			if (userEmail.isValidated() && userEmail.getCompanyId() != null){
				ret.add(userEmail.getCompanyId());
			}
		}

		ModelReflector<Company> ref = ModelReflector.instance(Company.class);
		List<Company> companies = new Select("ID").from(Company.class).where(new Expression(ref.getPool(),ref.getColumnDescriptor("CREATOR_USER_ID").getName(), Operator.EQ, u.getId())).execute();
		ret.addAll(DataSecurityFilter.getIds(companies));
		return ret;
		
		 */
		return u.getCompanyIds();
	}


}
