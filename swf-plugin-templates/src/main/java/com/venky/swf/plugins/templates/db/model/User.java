package com.venky.swf.plugins.templates.db.model;


import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.templates.db.model.alerts.Alert;
import com.venky.swf.plugins.templates.db.model.alerts.Device;

import java.util.List;
public interface User extends com.venky.swf.plugins.mail.db.model.User {

    public List<Alert> getAlerts();

    @HIDDEN
    public List<Device> getDevices();

    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public boolean isNotificationEnabled();
    public void setNotificationEnabled(boolean enabled);

}
