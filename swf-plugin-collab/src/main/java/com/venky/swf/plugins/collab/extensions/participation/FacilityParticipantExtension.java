package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;


public class FacilityParticipantExtension extends CompanySpecificParticipantExtension<Facility>{
	static {
		registerExtension(new FacilityParticipantExtension());
	}

	
}
