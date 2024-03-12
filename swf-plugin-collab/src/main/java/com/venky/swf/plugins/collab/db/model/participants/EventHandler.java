package com.venky.swf.plugins.collab.db.model.participants;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface EventHandler extends com.venky.swf.db.model.application.api.EventHandler {
    @PARTICIPANT
    public Long getApplicationId();

}
