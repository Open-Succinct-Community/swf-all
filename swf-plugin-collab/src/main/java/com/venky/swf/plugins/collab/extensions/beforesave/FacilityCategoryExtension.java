package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.collab.db.model.participants.admin.FacilityCategory;

public class FacilityCategoryExtension extends ModelOperationExtension<FacilityCategory> {
    static {
        registerExtension(new FacilityCategoryExtension());
    }

    @Override
    protected void beforeValidate(FacilityCategory instance) {
        super.beforeValidate(instance);
        if (!instance.getReflector().isVoid(instance.getMasterFacilityCategoryValueId())){
            instance.setMasterFacilityCategoryId(instance.getMasterFacilityCategoryValue().getMasterFacilityCategoryId()); //UK is changed. Lets refresh it.
            FacilityCategory newinstance = Database.getTable(FacilityCategory.class).getRefreshed(instance);
            if (!newinstance.getRawRecord().isNewRecord()) {
                instance.setRawRecord(newinstance.getRawRecord());
            }
        }
    }
}
