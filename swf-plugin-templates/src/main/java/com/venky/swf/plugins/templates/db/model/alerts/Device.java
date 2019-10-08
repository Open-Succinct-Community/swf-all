package com.venky.swf.plugins.templates.db.model.alerts;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
@EXPORTABLE(false)

public interface Device extends Model {
    @UNIQUE_KEY
    @PARTICIPANT
    @IS_NULLABLE(false)
    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();

    @UNIQUE_KEY
    @COLUMN_SIZE(512)
    public String getDeviceId();
    public void setDeviceId(String id);
}
