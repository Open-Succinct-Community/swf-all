package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.Grant;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class BearerAuthentication extends ApplicationAuthenticator {
    static {
        Registry.instance().registerExtension(ApplicationUtil.APPLICATION_AUTHENTICATOR_EXTENSION,new BearerAuthentication());
    }

    @Override
    protected void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder) {
        if (!ObjectUtil.equals(scheme,"bearer")){
            return;
        }
        Grant grant = Database.getTable(Grant.class).newRecord();
        grant.setAccessToken(schemeDetails);
        grant = Database.getTable(Grant.class).find(grant,false);
        if (grant!= null){
            if (grant.getAccessTokenExpiry() > System.currentTimeMillis()) {
                applicationObjectHolder.set(grant.getApplication());
            }else {
                grant.destroy();
            }
        }
    }
}
