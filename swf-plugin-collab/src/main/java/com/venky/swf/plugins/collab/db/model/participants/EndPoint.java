package com.venky.swf.plugins.collab.db.model.participants;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface EndPoint extends com.venky.swf.db.model.application.api.EndPoint {
    @PARTICIPANT
    public Long getApplicationId();
}
