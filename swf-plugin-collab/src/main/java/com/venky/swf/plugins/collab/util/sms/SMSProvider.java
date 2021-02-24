package com.venky.swf.plugins.collab.util.sms;

public interface SMSProvider {
    public void sendOtp(String mobile,String otp,boolean fresh);
}
