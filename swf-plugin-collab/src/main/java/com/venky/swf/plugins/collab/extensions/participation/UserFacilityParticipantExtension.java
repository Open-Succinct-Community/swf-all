package com.venky.swf.plugins.collab.extensions.participation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;

public class UserFacilityParticipantExtension extends ParticipantExtension<UserFacility> {
	static {
		registerExtension(new UserFacilityParticipantExtension());
	}
	protected UserFacilityParticipantExtension() {
		super(UserFacility.class);
	}

	@Override
	protected List<Integer> getAllowedFieldValues(User user, UserFacility model, String fieldName) {
		List<Integer> ret = null;
		if (fieldName.equalsIgnoreCase("FACILITY_ID")){
			ret = new ArrayList<Integer>();
			if (model.getFacilityId() > 0 ){
				if (model.getFacility().isAccessibleBy(user, Facility.class)){
					if (model.getUserId() == 0 || model.getFacility().isAccessibleBy(model.getUser(), Facility.class)){
						ret.add(model.getFacilityId());
					}
				}
			}else {
				List<Facility> facilites = DataSecurityFilter.getFacilitiesAccessible(user);
 				if (model.getUserId() > 0 ){
 					ret = new ArrayList<Integer>();
					for (Facility f : facilites){
						if (f.isAccessibleBy(model.getUser(), Facility.class)){
							ret.add(f.getId());
						}
					}
				}else {
					ret = DataSecurityFilter.getIds(facilites);
				}
			}
		}else if (fieldName.equalsIgnoreCase("USER_ID")){
			if (model.getUserId() > 0){
				ret = new ArrayList<Integer>();
				if (model.getUser().isAccessibleBy(user, User.class)){
					if (model.getFacilityId() == 0 || 
							(model.getFacility().isAccessibleBy(user, Facility.class) && 
									model.getFacility().isAccessibleBy(model.getUser(), Facility.class))){
						ret.add(model.getUserId());
					}
				}
			}else {
				if (model.getFacilityId() > 0){
					ret = new ArrayList<Integer>();
					Facility f = model.getFacility();
					if (f.isAccessibleBy(user, Facility.class)){
						List<User> allowedUsers = DataSecurityFilter.getAllowedUsers(f);
						Iterator<User> allowedUserIterator = allowedUsers.iterator(); 
						while (allowedUserIterator.hasNext()){
							User allowedUser = allowedUserIterator.next();
							if (!allowedUser.isAccessibleBy(user)){
								allowedUserIterator.remove();
							}
						}
						ret = DataSecurityFilter.getIds(allowedUsers);
					}
				}else {
					ret = DataSecurityFilter.getIds(DataSecurityFilter.getUsersAccessible(user));
				}
				
			}
			
		}
		return ret;	
	}

}