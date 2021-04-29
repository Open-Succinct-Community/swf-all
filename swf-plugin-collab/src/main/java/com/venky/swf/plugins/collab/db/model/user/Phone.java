package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.plugins.templates.db.model.User;

public interface Phone extends OtpEnabled{
    @WATERMARK("e.g +911234567890")
    @IS_NULLABLE(false)
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    public static String sanitizePhoneNumber(String phoneNumber){
        return User.sanitizePhoneNumber(phoneNumber);
    }
}
