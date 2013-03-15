package com.venky.swf.plugins.wiki.extensions;

import com.venky.swf.plugins.collab.extensions.participation.CompanySpecificParticipantExtension;
import com.venky.swf.plugins.wiki.db.model.Page;

public class PageParticipantExtension extends CompanySpecificParticipantExtension<Page>{
	static {
		registerExtension(new PageParticipantExtension());
	}
	
	
}
