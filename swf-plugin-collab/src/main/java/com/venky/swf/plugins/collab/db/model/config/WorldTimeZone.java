package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_FIELD("DESCRIPTION")
@MENU("World")

public interface WorldTimeZone extends Model {
    @UNIQUE_KEY("ZONE_ID")
    public String getZoneId();
    public void setZoneId(String zoneId);

    public String getName();
    public void setName(String name);

    @IS_VIRTUAL
    @Index
    public String getDescription();

}
