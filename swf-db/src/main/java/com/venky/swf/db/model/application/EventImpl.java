package com.venky.swf.db.model.application;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.extensions.SignatureAuthExtension;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.util.FreemarkerProcessor;
import com.venky.swf.util.ToWords;
import freemarker.cache.NullCacheStorage;
import freemarker.core.ArithmeticEngine;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EventImpl extends ModelImpl<Event> {
    public EventImpl(Event event){
        super(event);
    }

    public void raise(Object payload){
        String sPayLoad = payload.toString();

        for (ApplicationEvent applicationEvent : getProxy().getApplicationEvents()) {
            Application application = applicationEvent.getApplication();
            if (!ObjectUtil.isVoid(applicationEvent.getNotificationUrl())){
                Map<String,String> headers = new IgnoreCaseMap<>();
                headers.put("content-type",applicationEvent.getContentType());
                ApplicationUtil.addAuthorizationHeader(applicationEvent,headers,sPayLoad);

                new Call<InputStream>().url(applicationEvent.getNotificationUrl()).headers(headers).
                        inputFormat(InputFormat.INPUT_STREAM).
                        input(new ByteArrayInputStream(sPayLoad.getBytes(StandardCharsets.UTF_8))).hasErrors();

            }
        }
    }
}
