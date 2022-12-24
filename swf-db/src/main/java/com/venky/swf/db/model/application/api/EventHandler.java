package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.Event;

public interface EventHandler extends Model {
    @PARTICIPANT
    @IS_NULLABLE(false)
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY
    public Long getEventId();
    public void setEventId(Long id);
    public Event getEvent();

    @UNIQUE_KEY
    public Long getEndPointId();
    public void setEndPointId(Long id);
    public EndPoint getEndPoint();

    public String getRelativeUrl();
    public void setRelativeUrl(String relativeUrl);


    @Enumeration("application/json")
    public String getContentType();
    public void setContentType(String contentType);

}
