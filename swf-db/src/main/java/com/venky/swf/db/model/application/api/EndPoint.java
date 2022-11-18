package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;

import java.util.List;

public interface EndPoint extends Model {
    @HIDDEN
    @UNIQUE_KEY
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY
    public String getBaseUrl();
    public void setBaseUrl(String baseUrl);

    @UNIQUE_KEY
    @IS_NULLABLE
    public Long getOpenApiId();
    public void setOpenApiId(Long id);
    public OpenApi getOpenApi();
}
