package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;

public class BeforeSaveFacility extends BeforeSaveAddress<Facility> {
    static {
        registerExtension(new BeforeSaveFacility());
    }

    @Override
    protected boolean isOkToSetLocationAsync() {
        return false;
    }
}
