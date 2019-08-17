package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CompanySpecificParticipantExtension<M extends Model & CompanySpecific> extends ParticipantExtension<M>{

	@Override
	protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, M partiallyFilledModel, String fieldName) {
		
		User u = (User)user;
		if (partiallyFilledModel != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				if (u.getCompanyId() != null){
					List<Long> ret = new SequenceSet<>();
					ret.add(u.getCompanyId());
					ret.addAll(u.getCompany().getCustomers().stream().map(r->r.getCustomerId()).collect(Collectors.toList()));
					ret.addAll(u.getCompany().getVendors().stream().map(r->r.getVendorId()).collect(Collectors.toList()));
					return ret;
				}else {
					return new ArrayList<>();
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
