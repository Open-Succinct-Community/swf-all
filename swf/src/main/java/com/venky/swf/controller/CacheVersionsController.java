package com.venky.swf.controller;

import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectHolder;
import com.venky.extension.Registry;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.cache.CacheVersion;
import com.venky.swf.path.Path;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.Arrays;
import java.util.List;


public class CacheVersionsController extends ModelController<CacheVersion> {
    public CacheVersionsController(Path path) {
        super(path);
    }



    @RequireLogin(false)
    public View last(){
        CacheVersion version = CacheVersion.getLastVersion();
        if (getReturnIntegrationAdaptor() == null){
            return back();
        }else {
            return getReturnIntegrationAdaptor().createResponse(getPath(),version, Arrays.asList("VERSION_NUMBER","UPDATED_AT"));
        }
    }

    public View increment(){
        CacheVersion version = CacheVersion.getLastVersion();
        version.getVersionNumber().increment();
        version.save();
        Registry.instance().callExtensions(Controller.CLEAR_CACHED_RESULT_EXTENSION, CacheOperation.CLEAR,getPath(),new ObjectHolder<>(null));
        if (getReturnIntegrationAdaptor() == null){
            return back();
        }else {
            return getReturnIntegrationAdaptor().createResponse(getPath(),version);
        }
    }

}
