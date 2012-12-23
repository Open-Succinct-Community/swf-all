package com.venky.swf.plugins.wiki.extensions;

import java.util.Arrays;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.wiki.db.model.Page;
import com.venky.swf.pm.DataSecurityFilter;

public class PageParticipantExtension extends ParticipantExtension<Page>{
	static {
		registerExtension(new PageParticipantExtension());
	}
	
	
	@Override
	protected List<Integer> getAllowedFieldValues(User user, Page partial, String fieldName) {
		if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
			if (partial.getCompanyId() != null && partial.getCompany().isAccessibleBy(user)){
				return Arrays.asList(partial.getCompanyId());
			}else {
				SequenceSet<Integer> ids =  DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Company.class,user));
				ids.add(null);
				return ids;
			}
		}
		return null;
	}

}
