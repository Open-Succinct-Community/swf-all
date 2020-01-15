package com.venky.swf.plugins.collab.agents;

import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.collab.db.model.user.OtpEnabled;

public class SendOtp implements Task {
    OtpEnabled otpEnabled;
    public SendOtp(OtpEnabled otpEnabled){
        this.otpEnabled = otpEnabled;
    }
    public SendOtp(){

    }

    @Override
    public void execute() {
        otpEnabled.resendOtp();
    }
}