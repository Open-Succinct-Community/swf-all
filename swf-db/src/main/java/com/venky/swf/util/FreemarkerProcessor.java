package com.venky.swf.util;

import com.venky.cache.Cache;
import com.venky.swf.routing.Config;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class FreemarkerProcessor {
    private static final Cache<String,FreemarkerProcessor> sSoleInstance = new Cache<>() {
        @Override
        protected FreemarkerProcessor getValue(String directory) {
            return new FreemarkerProcessor(directory);
        }
    };

    //private constructor.
    final String directory ;
    private FreemarkerProcessor(String directory) {
        //Prevent form the reflection api.
        if (sSoleInstance.containsKey(directory)) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
        this.directory = directory;
    }
    public static FreemarkerProcessor getInstance() {
        return getInstance(null);
    }

    public static FreemarkerProcessor getInstance( String directory) {
        return sSoleInstance.get(directory);
    }

    //Make singleton from serialize and deserialize operation.
    protected FreemarkerProcessor readResolve() {
        return getInstance();
    }

    public String publish(String templateName, Map<String,Object> root) {
        StringWriter writer = new StringWriter();
        publish(templateName,root,writer);
        return writer.toString();
    }
    public Configuration getConfiguration(){
        return ConfigurationFactory.getInstance().getConfiguration(directory);
    }
    public void publish(String templateName, Map<String,Object> root, Writer output){
        try {
            Template template = getConfiguration().getTemplate(templateName);
            publish(template,root,output);
        } catch (IOException ex) {
            throw new RuntimeException(root +"\n"+ ex.getMessage(),ex);
        }
    }
    public void publish (Template template, Map<String,Object> root,Writer output){
        try {
            for (String key : Config.instance().getPropertyKeys("swf.*host")){
                putEnvKey(root,key,Config.instance().getProperty(key));
            }

            Config.instance().getLogger(getClass().getName()).info("Data for " + template.getName() +":" + root.toString());
            template.process(root,output);
        }catch (IOException | TemplateException ex) {
            throw new RuntimeException(root +"\n"+ ex.getMessage(),ex);
        }
    }
    public boolean exists(String templateName){
        try {
            return (null != getConfiguration().getTemplate(templateName,null,null,false,true));
        }catch (Exception ex){
            return false;
        }
    }
    public void putEnvKey(Map<String,Object> root, String key, String value){
        String[] path = key.split("\\.");
        if (path.length == 0){
            return;
        }
        Map previousLevelMap = (Map)root.get("env");
        if (previousLevelMap == null){
            previousLevelMap =  new HashMap<>();
            root.put("env",previousLevelMap);
        }
        for (int i = 0 ; i < path.length ; i ++ ){
            Object o = previousLevelMap.get(path[i]);
            if (o == null) {
                o = new HashMap<>();
                previousLevelMap.put(path[i], o);
            }
            if (i < path.length - 1){
                previousLevelMap = (Map)o;
            }else {
                previousLevelMap.put(path[i],value);
                break;
            }
        }
    }
}
