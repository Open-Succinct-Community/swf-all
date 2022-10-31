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

    public void raise(Map<String,Object> variables){
        for (ApplicationEvent applicationEvent : getProxy().getApplicationEvents()) {
            Application application = applicationEvent.getApplication();
            Template template = getTemplate(applicationEvent.getTemplate());
            StringWriter writer = new StringWriter();
            FreemarkerProcessor.getInstance().publish(template,variables,writer);
            if (!ObjectUtil.isVoid(applicationEvent.getNotificationUrl())){
                Map<String,String> headers = new IgnoreCaseMap<>();
                headers.put("content-type",applicationEvent.getContentType());
                String payload = writer.toString();
                ApplicationUtil.addAuthorizationHeader(applicationEvent,headers,payload);

                new Call<InputStream>().url(applicationEvent.getNotificationUrl()).headers(headers).
                        inputFormat(InputFormat.INPUT_STREAM).
                        input(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8))).hasErrors();

            }
        }
    }

    public Template getTemplate(Reader template){
        try {
            return new Template("request", StringUtil.read(template), getConfiguration());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Configuration cfg = null ;
    static {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setLocalizedLookup(false);
        cfg.setWrapUncheckedExceptions(true);
        ArithmeticEngine engine = ArithmeticEngine.BIGDECIMAL_ENGINE;
        engine.setMinScale(2);
        engine.setMaxScale(2);
        cfg.setArithmeticEngine(engine);
        cfg.setCacheStorage(new NullCacheStorage()); //
        cfg.setSharedVariable("to_words",new ToWords());
    }
    private Configuration getConfiguration(){
        return cfg;
    }
}
