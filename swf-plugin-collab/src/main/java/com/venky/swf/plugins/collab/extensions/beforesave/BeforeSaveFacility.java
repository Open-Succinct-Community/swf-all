package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.routing.Config;

public class BeforeSaveFacility extends BeforeSaveAddress<Facility> {
    static {
        registerExtension(new BeforeSaveFacility());
    }

    @Override
    protected boolean isOkToSetLocationAsync() {
        return Config.instance().getBooleanProperty("swf.collab.facility.geo.assign.async",false);
    }
}
