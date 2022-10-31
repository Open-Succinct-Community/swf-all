package com.venky.swf.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.venky.cache.Cache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.string.Inflector;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.json.JSONModelWriter;
import com.venky.swf.routing.Config;
import freemarker.template.Template;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.w3c.tidy.Tidy;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        return instance.get(directory);
    }
    private final FreemarkerProcessor freemarkerProcessor;
    private TemplateProcessor(String directory){
        freemarkerProcessor = FreemarkerProcessor.getInstance(directory);
    }
    public String publish(String templateName, Map<String,Object> root) {
        return freemarkerProcessor.publish(templateName,root);
    }
    public void publish(String templateName, Map<String,Object> root, Writer output){
        freemarkerProcessor.publish(templateName,root,output);
    }
    public void publish (Template template, Map<String,Object> root,Writer output){
        freemarkerProcessor.publish(template,root,output);
    }
    public boolean exists(String templateName){
        return freemarkerProcessor.exists(templateName);
    }

    public void putEnvKey(Map<String,Object> root, String key, String value){
        freemarkerProcessor.putEnvKey(root,key,value);
    }

    W3CDom w3CDom = new W3CDom();

    public byte[] htmlToPdf(byte[] htmlBytes){
        StringWriter tidyWriter = new StringWriter();
        Tidy tidy =  new Tidy();
        tidy.setXHTML(true);
        tidy.setDropProprietaryTags(false);
        tidy.setErrfile(Config.instance().getProperty("swf.html.tidy.err.file"));
        tidy.parse(new ByteArrayInputStream(htmlBytes), tidyWriter);
        Logger logger = Config.instance().getLogger(TemplateProcessor.class.getName());

        if (logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE,"Original Html-START");
            logger.log(Level.FINE,new String(htmlBytes));
            logger.log(Level.FINE,"Original Html-END");
            logger.log(Level.FINE,"Tidy Html-START");
            logger.log(Level.FINE,tidyWriter.toString());
            logger.log(Level.FINE,"Tidy Html-END");
        }
        //tidyWriter.write(new String(htmlBytes));

        if (tidyWriter.getBuffer().length() > 0){
            try (SeekableByteArrayOutputStream os = new SeekableByteArrayOutputStream()) {

                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                Document document = Jsoup.parse(tidyWriter.toString());

                builder.withW3cDocument(w3CDom.fromJsoup(document),".");
                builder.toStream(os);
                builder.run();
                return os.toByteArray();

            } catch (Exception e) {
                Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"HTML to pdf Failed", e);
                return new byte[]{};
            }
        }
        return new byte[]{};
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
    public Map<String,Object> formatEntityMap(Map<String,Object> entityMap, Map<Class<? extends Model>, List<String>> entityFieldMap){
        return formatEntityMap(entityMap,entityFieldMap,getDefaultChildModelMap(entityMap,entityFieldMap));
    }
    public Map<Class<? extends Model>, List<Class<? extends Model>>> getDefaultChildModelMap(Map<String,Object> entityMap, Map<Class<? extends Model>, List<String>> entityFieldMap){
        Cache<Class<? extends Model>, List<Class<? extends Model>>> childModelMap = new Cache<Class<? extends Model>, List<Class<? extends Model>>>() {
            @Override
            protected List<Class<? extends Model>> getValue(Class<? extends Model> aClass) {
                return new ArrayList<>();
            }
        };

        for (String entityName :entityMap.keySet()) {
            Object entityOrEntityList = entityMap.get(entityName);
            List<Model> entityList = entityOrEntityList instanceof List ? (List<Model>) entityOrEntityList : null;
            Model entity = entityOrEntityList instanceof Model ? (Model) entityOrEntityList : null;
            if (entity == null && entityList != null && !entityList.isEmpty()){
                entity = entityList.get(0);
            }
            if (entity != null) {
                childModelMap.putAll(entity.getReflector().getChildrenToBeConsidered(entityFieldMap));
            }
        }
        return childModelMap;
    }
    public Map<String,Object> formatEntityMap(Map<String,Object> entityMap, Map<Class<? extends Model>, List<String>> entityFieldMap,Map<Class<? extends Model>, List<Class<? extends Model>>> childModelMap) {
        Map<String, Object> root = new HashMap<>();
        for (String entityName :entityMap.keySet()){
            Object entityOrEntityList = entityMap.get(entityName);
            List<Model> entityList  = entityOrEntityList instanceof List ? (List<Model>)entityOrEntityList : null;
            Model entity = entityOrEntityList instanceof Model ? (Model)entityOrEntityList : null;

            if (entity != null){
                root.put(entityName,format(entity,entityFieldMap,childModelMap));
            }else if (entityList != null){
                List<JSONObject> out = new ArrayList<>();
                for (Model e : entityList){
                    out.add(format(e,entityFieldMap,childModelMap));
                }
                root.put(entityName,out);
            }
        }
        return root;
    }
    public JSONObject format(Model entity,Map<Class<? extends Model>, List<String>> entityFieldMap){
        return format(entity,entityFieldMap, entity.getReflector().getChildrenToBeConsidered(entityFieldMap));
    }
    public JSONObject format(Model entity,Map<Class<? extends Model>, List<String>> entityFieldMap, Map<Class<? extends Model>, List<Class<? extends Model>>> childModelMap){
        JSONObject into = new JSONObject();
        JSONModelWriter<Model> modelJSONModelWriter = new JSONModelWriter<>(entity.getReflector().getModelClass());
        modelJSONModelWriter.write(entity,into,entityFieldMap == null ? null : entityFieldMap.get(entity.getReflector().getModelClass()), new HashSet<>(),
                childModelMap,
                entityFieldMap == null ? new HashMap<>() : entityFieldMap);
        return  into;
    }


}
