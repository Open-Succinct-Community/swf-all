package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.user.User;

public class BeforeSaveFacility extends BeforeSaveAddress<Facility> {
    static {
        registerExtension(new BeforeSaveFacility());
    }
}
