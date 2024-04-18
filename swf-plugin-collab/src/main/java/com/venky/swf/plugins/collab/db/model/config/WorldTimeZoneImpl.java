package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.table.ModelImpl;

public class WorldTimeZoneImpl extends ModelImpl<WorldTimeZone> {
    public WorldTimeZoneImpl(WorldTimeZone z){
        super(z);
    }

    public String getDescription(){
        if (getRawRecord().isNewRecord()){
            return null;
        }
        return getProxy().getZoneId() + " - " + getProxy().getName();
    }
}
