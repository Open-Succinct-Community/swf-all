package com.venky.swf.plugins.collab.util.sms;

import com.venky.swf.routing.Config;

public class SMSProviderFactory {
    private SMSProviderFactory(){

    }
    private static SMSProviderFactory _instance = null;
    public static SMSProviderFactory getInstance(){
        if (_instance != null){
            return _instance;
        }
        synchronized (SMSProviderFactory.class){
            if (_instance == null){
                _instance = new SMSProviderFactory();
            }
            return _instance;
        }

    }
    public SMSProvider getDefaultProvider(){
        String provider = Config.instance().getProperty("swf.otp.provider","msg91");
        switch (provider){
            case "msg91":
                return new Msg91Provider(provider);
            case "2f":
                return new TwoFProvider(provider);
            default:
                throw new RuntimeException("Unknown SMS Provider:" + provider);
        }
    }
}
