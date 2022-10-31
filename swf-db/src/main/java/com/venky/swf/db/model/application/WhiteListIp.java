package com.venky.swf.db.model.application;

import com.venky.swf.db.model.Model;

public interface WhiteListIp  extends Model {
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    public String getIpAddress();
    public void setIpAddress(String ipAddress);
    
}
