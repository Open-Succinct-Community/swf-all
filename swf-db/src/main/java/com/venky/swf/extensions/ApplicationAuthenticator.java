package com.venky.swf.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.swf.db.model.application.Application;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class ApplicationAuthenticator implements Extension {


    @Override
    @SuppressWarnings({"unchecked"})
    public void invoke(Object... context) {
        ByteArrayInputStream payload = (ByteArrayInputStream) context[0];
        Map<String,String> headers = (Map<String,String>)context[1];
        ObjectHolder<Application> applicationObjectHolder =  (ObjectHolder<Application>) context[2];

        String authorization = headers.get("Authorization");
        if (ObjectUtil.isVoid(authorization)) {
            return ;
        }

        if (applicationObjectHolder.get() != null){
            return;
        }

        StringTokenizer authTokenizer = new StringTokenizer(authorization);
        String scheme = authTokenizer.nextToken().toLowerCase();
        String schemeDetails = authorization.substring(scheme.length()).trim();


        authenticate(scheme,schemeDetails,payload,headers,applicationObjectHolder);
    }

    protected abstract void authenticate(String scheme, String schemeDetails, ByteArrayInputStream payload, Map<String, String> headers, ObjectHolder<Application> applicationObjectHolder);


}
