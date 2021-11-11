package com.venky.swf.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.controller.Controller;
import com.venky.swf.db.model.cache.CacheVersion;

public class LastCacheVersion implements Extension {
    private LastCacheVersion(){

    }
    private static LastCacheVersion instance = null;
    public static LastCacheVersion getInstance(){
        if (instance == null){
            synchronized (LastCacheVersion.class){
                if (instance == null){
                    instance = new LastCacheVersion();
                }
            }
        }
        return instance;
    }
    static {
        Registry.instance().registerExtension(Controller.CLEAR_CACHED_RESULT_EXTENSION,getInstance());
    }


    private CacheVersion cacheVersion = null;
    @Override
    public void invoke(Object... context) {
        load(true);
    }
    public CacheVersion get(){
        load(false);
        return cacheVersion;
    }
    public void load(boolean reset){
        if (cacheVersion == null || reset){
            synchronized (this){
                if (cacheVersion == null || reset){
                    cacheVersion = CacheVersion.getLastVersion();
                }
            }
        }
    }
}
