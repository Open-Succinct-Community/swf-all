package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;

public interface Email extends OtpEnabled {
    @IS_NULLABLE(false)
    public String getEmail();
    public void setEmail(String email);

}
