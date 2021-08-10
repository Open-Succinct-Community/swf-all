package com.venky.swf.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.venky.cache.Cache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.string.Inflector;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.json.JSONModelWriter;
import com.venky.swf.routing.Config;
import freemarker.cache.NullCacheStorage;
import freemarker.core.ArithmeticEngine;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.json.simple.JSONObject;
import org.w3c.tidy.Tidy;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateProcessor {


    private static Cache<String,TemplateProcessor>  instance = new Cache<String, TemplateProcessor>() {
        @Override
        protected TemplateProcessor getValue(String directory) {
            return new TemplateProcessor(directory);
        }
    };
    public static TemplateProcessor getInstance(){
        return getInstance(null);
    }
    public static TemplateProcessor getInstance(String directory){
        return instance.get(directory == null? Config.instance().getProperty("swf.ftl.dir") : directory);
    }
    Configuration cfg = null;
    private TemplateProcessor(String directory){
        cfg = new Configuration(Configuration.VERSION_2_3_28);
        try {
            File dir = null;
            if (!ObjectUtil.isVoid(directory)) {
                dir = new File(directory);
            }
            if (dir != null && dir.exists() && dir.isDirectory()){
                cfg.setDirectoryForTemplateLoading(dir);
            }else{
                cfg.setClassForTemplateLoading(TemplateProcessor.class, directory);
            }
        }catch (Exception ex){
            cfg.setClassForTemplateLoading(TemplateProcessor.class, "/templates");
        }
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
    public String publish(String templateName, Map<String,Object> root) {
        StringWriter writer = new StringWriter();
        Config.instance().getLogger(getClass().getName()).info("Data for " + templateName +":" + root.toString());
        publish(templateName,root,writer);
        return writer.toString();
    }
    public void publish(String templateName, Map<String,Object> root, Writer output){
        try {
            Template template = cfg.getTemplate(templateName);
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

            Config.instance().getLogger(getClass().getName()).info(root.toString());
            template.process(root,output);
        }catch (IOException | TemplateException ex) {
            throw new RuntimeException(root +"\n"+ ex.getMessage(),ex);
        }
    }

    public byte[] htmlToPdf(byte[] htmlBytes){
        StringWriter tidyWriter = new StringWriter();

        Tidy tidy =  new Tidy();
        tidy.setXHTML(true);
        tidy.parse(new ByteArrayInputStream(htmlBytes), tidyWriter);


        if (tidyWriter.getBuffer().length() > 0){
            try (SeekableByteArrayOutputStream os = new SeekableByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(tidyWriter.toString(), ".");
                builder.toStream(os);
                builder.run();
                return os.toByteArray();
            } catch (Exception e) {
                return new byte[]{};
            }
        }
        return new byte[]{};
    }
    public boolean exists(String templateName){
        try {
            return (null != cfg.getTemplate(templateName,null,null,false,true));
        }catch (Exception ex){
            return false;
        }
    }
    public Map<String,Object> createEntityMap(List<Model> entities) {
        Map<String, Object> entityMap = new HashMap<>();
        Set<Class<?>> pluralEntities = new HashSet<>();
        for (Model entity :entities){
            String keyName = entity.getReflector().getModelClass().getSimpleName();
            boolean isPlural = false;
            if (pluralEntities.contains(entity.getReflector().getModelClass())){
                isPlural = true;
                keyName = Inflector.pluralize(keyName);
            }

            Object value = entityMap.get(keyName);
            if (value == null){
                entityMap.put(keyName,entity);
            }else{
                if (!isPlural) {
                    pluralEntities.add(entity.getReflector().getModelClass());
                    List<Model> tmp  = new ArrayList<>();
                    tmp.add((Model)value);
                    tmp.add(entity);
                    entityMap.remove(keyName);
                    entityMap.put(Inflector.pluralize(keyName),tmp);
                }else {
                    ((List<Model>)value).add(entity);
                }
            }
        }
        return entityMap;
    }

    public Map<String,Object> formatEntityMap(Map<String,Object> entityMap, Map<Class<? extends Model>, List<String>> entityFieldMap) {
        Map<String, Object> root = new HashMap<>();
        for (String entityName :entityMap.keySet()){
            Object entityOrEntityList = entityMap.get(entityName);
            List<Model> entityList  = entityOrEntityList instanceof List ? (List<Model>)entityOrEntityList : null;
            Model entity = entityOrEntityList instanceof Model ? (Model)entityOrEntityList : null;

            if (entity != null){
                root.put(entityName,format(entity,entityFieldMap));
            }else if (entityList != null){
                List<JSONObject> out = new ArrayList<>();
                for (Model e : entityList){
                    out.add(format(e,entityFieldMap));
                }
                root.put(entityName,out);
            }
        }
        return root;
    }
    public JSONObject format(Model entity,Map<Class<? extends Model>, List<String>> entityFieldMap){
        JSONObject into = new JSONObject();
        JSONModelWriter<Model> modelJSONModelWriter = new JSONModelWriter<>(entity.getReflector().getModelClass());
        modelJSONModelWriter.write(entity,into,entityFieldMap == null ? null : entityFieldMap.get(entity.getReflector().getModelClass()), new HashSet<>(),
                entityFieldMap == null ? new HashMap<>() : entityFieldMap);
        return  into;
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
