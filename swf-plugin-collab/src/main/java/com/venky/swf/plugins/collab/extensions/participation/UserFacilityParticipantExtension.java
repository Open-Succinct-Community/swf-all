package com.venky.swf.plugins.collab.extensions.participation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.participants.admin.FacilityUser;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;
import com.venky.swf.pm.DataSecurityFilter;

public class UserFacilityParticipantExtension extends ParticipantExtension<UserFacility> {
	static {
		registerExtension(new UserFacilityParticipantExtension());
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
				List<Facility> facilites = DataSecurityFilter.getRecordsAccessible(Facility.class,user);
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
				List<Facility> facilites = DataSecurityFilter.getRecordsAccessible(Facility.class,user);
				List<Integer> facilityIds = DataSecurityFilter.getIds(facilites);
				ret = new SequenceSet<Integer>();
				if (facilityIds.isEmpty() || ( model.getFacilityId() > 0 && !facilityIds.contains(model.getFacilityId()))){
					return ret; 
				}
				//Facility is accessible or not passed.
				
				List<Integer> userIdsAlreadySubscribedToSomeFacility = new SequenceSet<Integer>();
				for (Facility f : facilites){
					for (FacilityUser fu : f.getFacilityUsers()){
						userIdsAlreadySubscribedToSomeFacility.add(fu.getUserId());
					}
				}
				
				Facility f = model.getFacility(); 
				
				List<User> allowedUsers = DataSecurityFilter.getRecordsAccessible(User.class,user);
				
				Iterator<User> aui = allowedUsers.iterator();
				while( aui.hasNext() ){
					User allowedUser = aui.next();
					Integer companyId = ((com.venky.swf.plugins.collab.db.model.user.User)allowedUser).getCompanyId();
					if (companyId == null){
						aui.remove();
					}else if (userIdsAlreadySubscribedToSomeFacility.contains(allowedUser.getId())){
						aui.remove();
					}else if (f != null){
						if (f.getCompanyId() != companyId){
							aui.remove();
						}else if (!f.isAccessibleBy(allowedUser)){
							aui.remove();
						}
					}
				}
				
				ret = DataSecurityFilter.getIds(allowedUsers);
			}
		}
		return ret;	
	}


}
