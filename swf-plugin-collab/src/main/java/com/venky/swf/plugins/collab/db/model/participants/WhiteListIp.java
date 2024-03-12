package com.venky.swf.plugins.collab.db.model.participants;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface WhiteListIp extends com.venky.swf.db.model.application.WhiteListIp {
    @PARTICIPANT
    public Long getApplicationId();

}
