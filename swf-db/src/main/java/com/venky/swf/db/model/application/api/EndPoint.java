package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.ENCRYPTED;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;

import java.util.List;

@HAS_DESCRIPTION_FIELD("BASE_URL")
public interface EndPoint extends Model {
    @HIDDEN
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY
    public String getBaseUrl();
    public void setBaseUrl(String baseUrl);

    @ENCRYPTED
    public String getClientId();
    public void setClientId(String clientId);


    @ENCRYPTED
    public String getSecret();
    public void setSecret(String secret);

    @ENCRYPTED
    public String getTokenName();
    public void setTokenName(String tokenName);

    @ENCRYPTED
    @COLUMN_SIZE(2048)
    public String getTokenValue();
    public void setTokenValue(String tokenValue);

    @IS_NULLABLE
    public Long getOpenApiId();
    public void setOpenApiId(Long id);
    public OpenApi getOpenApi();

    public List<EventHandler> getEventHandlers();
}
