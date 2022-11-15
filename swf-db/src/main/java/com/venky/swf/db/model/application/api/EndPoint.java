package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;

import java.util.List;

public interface EndPoint extends Model {
    @HIDDEN
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();


    public String getBaseUrl();
    public void setBaseUrl(String baseUrl);

    public List<ImplementedInterface> getImplementedInterfaces();
}
