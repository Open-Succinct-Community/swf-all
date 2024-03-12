package com.venky.swf.plugins.collab.db.model.participants;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface ApplicationPublicKey extends com.venky.swf.db.model.application.ApplicationPublicKey {
    @PARTICIPANT
    public Long getApplicationId();

}