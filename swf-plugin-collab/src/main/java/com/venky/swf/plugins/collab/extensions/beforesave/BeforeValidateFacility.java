package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;

public class BeforeValidateFacility extends BeforeValidateAddress<Facility>{
    static {
        registerExtension(new BeforeValidateFacility());
    }

    @Override
    protected Class<Facility> getModelClass() {
        return Facility.class;
    }
}
