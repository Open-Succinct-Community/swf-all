package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.table.ModelImpl;

public class MasterFacilityCategoryValueImpl extends ModelImpl<MasterFacilityCategoryValue> {
    public MasterFacilityCategoryValueImpl(MasterFacilityCategoryValue p){
        super(p);
    }

    public String getDescription(){
        MasterFacilityCategory category = getProxy().getMasterFacilityCategory();
        if (category == null){
            return null;
        }
        String v = getProxy().getAllowedValue();
        if (v == null){
            return null;
        }
        return  String.format("%s-%s",category.getName() ,v);
    }
    public void setDescription(String description){

    }
}
