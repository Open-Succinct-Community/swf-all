package com.venky.swf.db.model.application;

import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

import java.io.Reader;

public interface ApplicationEvent extends Model {
    @UNIQUE_KEY
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY
    public Long getEventId();
    public void setEventId(Long id);
    public Event getEvent();


    public String getNotificationUrl();
    public void setNotificationUrl(String notificationUrl);

    @Enumeration("application/json")
    public String getContentType();
    public void setContentType(String contentType);

    public Reader getTemplate();
    public void setTemplate(Reader reader);


}
