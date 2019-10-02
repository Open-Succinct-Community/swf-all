package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import com.venky.swf.plugins.collab.db.model.user.UserPhone;

public class UserPhonesController extends OtpEnabledController<UserPhone> {
    public UserPhonesController(Path path) {
        super(path);
    }


    @SingleRecordAction(icon = "glyphicon-question-sign")
    public View sendOtp(long id){
        return super.sendOtp(id,"PHONE_NUMBER");
    }

    public View validateOtp(long id) throws Exception {
        return super.validateOtp(id,"PHONE_NUMBER");
    }
}
