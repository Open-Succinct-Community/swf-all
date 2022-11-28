package com.venky.swf.util;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import freemarker.cache.NullCacheStorage;
import freemarker.core.ArithmeticEngine;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;

public class ConfigurationFactory {
    private static volatile ConfigurationFactory sSoleInstance;

    //private constructor.
    private ConfigurationFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static ConfigurationFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (ConfigurationFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new ConfigurationFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected ConfigurationFactory readResolve() {
        return getInstance();
    }
    private final Cache<String,Configuration> cfgCache= new Cache<>() {
        @Override
        protected Configuration getValue(String directory) {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
            try {
                File dir = null;
                if (!ObjectUtil.isVoid(directory)) {
                    dir = new File(directory);
                }
                if (dir != null && dir.exists() && dir.isDirectory()) {
                    cfg.setDirectoryForTemplateLoading(dir);
                } else {
                    cfg.setClassForTemplateLoading(ConfigurationFactory.class, directory);
                }
            } catch (Exception ex) {
                cfg.setClassForTemplateLoading(ConfigurationFactory.class, "/templates");
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
            cfg.setSharedVariable("to_words", new ToWords());
            return cfg;
        }
    };
    public Configuration getConfiguration(String directory){
        return cfgCache.get(directory == null? Config.instance().getProperty("swf.ftl.dir") : directory);
    }
}
