package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import com.venky.swf.plugins.collab.db.model.user.UserEmail;

public class UserEmailsController extends OtpEnabledController<UserEmail> {


    public UserEmailsController(Path path) {
        super(path);
    }

    @SingleRecordAction(icon = "fa-envelope")
    public View sendOtp(long id){
        return super.sendOtp(id,"EMAIL");
    }

    @SingleRecordAction(icon = "fa-user-check")
    public View validateOtp(long id) throws Exception {
        return super.validateOtp(id,"EMAIL");
    }
}
