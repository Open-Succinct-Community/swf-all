package com.venky.swf.db.model;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.ui.User;

import java.sql.Timestamp;

@MENU("Logs")
public interface UserLogin extends Model , GeoLocation {
    @UNIQUE_KEY
    @Index
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
