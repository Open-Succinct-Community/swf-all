package com.venky.swf.db.model.application;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.model.Model;

/**
 * Whitelisted ips from where an application can reach
 */
public interface WhiteListIp  extends Model {
    @IS_NULLABLE(false)
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    public String getIpAddress();
    public void setIpAddress(String ipAddress);
    
}
