package com.venky.swf.db.model;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

import java.sql.Timestamp;

public interface UserLogin extends Model , GeoLocation {
    @UNIQUE_KEY
    public long getUserId();
    public void setUserId(long id);
    public User getUser();

    @UNIQUE_KEY
    public String getFromIp();
    public void setFromIp(String fromIp);

    @COLUMN_SIZE(512)
    public String getUserAgent();
    public void setUserAgent(String agent);

    public Timestamp getLoginTime();
    public void setLoginTime(Timestamp loginTime);



}
