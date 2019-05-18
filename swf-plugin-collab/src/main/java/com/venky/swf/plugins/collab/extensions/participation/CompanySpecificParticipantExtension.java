package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Select.ResultFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompanySpecificParticipantExtension<M extends Model & CompanySpecific> extends ParticipantExtension<M>{

	@Override
	protected List<Long> getAllowedFieldValues(User user, M partiallyFilledModel, String fieldName) {
		
		User u = (User)user;
		if (partiallyFilledModel != null){
			if ("COMPANY_ID".equalsIgnoreCase(fieldName)){
				if (!partiallyFilledModel.getReflector().isVoid(partiallyFilledModel.getCompanyId()) && partiallyFilledModel.getCompany().isAccessibleBy(user)){
					return Arrays.asList(partiallyFilledModel.getCompanyId());
				}else {
					List<UserCompany> ucs = u.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.User.class).getUserCompanies();
					SequenceSet<Long> ids =  new SequenceSet<>();
					for (UserCompany uc:ucs){ 
						ids.add(uc.getCompanyId());
					}
					return ids;
				}
			}else if ("USER_ID".equalsIgnoreCase(fieldName)){
				return getAllowedUserIds(user,partiallyFilledModel,fieldName,false);
			}
		}
		return null;
	}

	protected List<Long> getAllowedUserIds(User user,  M partiallyFilledModel, String fieldName, boolean onlyStaff){

		if (!getReflector().getReferredModelGetterFor(getReflector().getFieldGetter(fieldName)).getReturnType().getSimpleName().equals(User.class.getSimpleName())){
			return null;
		}
		Long userId = getReflector().get(partiallyFilledModel,fieldName);
		if (!getReflector().isVoid(userId)){
			if (!ObjectUtil.equals(userId,user.getId())) {
				User other = Database.getTable(User.class).get(userId);
				if (other.isAccessibleBy(user)) {
					return Arrays.asList(other.getId());
				}
			}
		}else if (user.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.User.class).isStaff()) {
			return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(com.venky.swf.db.model.User.class, user).stream().filter((u)->{
				return !onlyStaff || u.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.User.class).isStaff();
			}).collect(Collectors.toList()));
		}

		return Arrays.asList(user.getId());




	}
}
