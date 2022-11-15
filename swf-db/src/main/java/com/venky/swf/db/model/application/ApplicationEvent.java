package com.venky.swf.db.model.application;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.api.ImplementedInterface;

public interface ApplicationEvent extends Model {
    @UNIQUE_KEY
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY
    public Long getEventId();
    public void setEventId(Long id);
    public Event getEvent();


    public Long getApiId();
    public void setApiId(Long id);
    public ImplementedInterface getApi();


}
