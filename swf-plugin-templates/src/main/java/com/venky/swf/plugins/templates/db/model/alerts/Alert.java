package com.venky.swf.plugins.templates.db.model.alerts;

import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

import java.sql.Timestamp;
@EXPORTABLE(false)

public interface Alert  extends Model  {
    @UNIQUE_KEY
    public long getId();

    @PARTICIPANT
    @IS_NULLABLE(false)
    @Index
    public Long getUserId();
    public void setUserId(Long userId);
    public User getUser();

    @Index
    public String getSubject();
    public void setSubject(String subject);

    @COLUMN_SIZE(512)
    @Index
    public String getMessage();
    public void setMessage(String message);


    public Timestamp getAlertedAt();
    public void setAlertedAt(Timestamp timestamp);

    @HIDDEN
    public String getTemplate();
    public void setTemplate(String template);


}
