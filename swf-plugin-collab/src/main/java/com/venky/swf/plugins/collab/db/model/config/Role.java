package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;

public interface Role extends com.venky.swf.plugins.security.db.model.Role {
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isStaff();
    public void setStaff(boolean staff);
}
