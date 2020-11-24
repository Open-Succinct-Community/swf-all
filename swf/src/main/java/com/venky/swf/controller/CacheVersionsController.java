package com.venky.swf.controller;

import com.venky.core.util.Bucket;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.cache.CacheVersion;
import com.venky.swf.path.Path;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.List;


public class CacheVersionsController extends ModelController<CacheVersion> {
    public CacheVersionsController(Path path) {
        super(path);
    }

    public CacheVersion getLastVersion(){
        List<CacheVersion> versionList = new Select().from(CacheVersion.class).orderBy("VERSION_NUMBER DESC").execute(1);
        CacheVersion version = null;
        if (!versionList.isEmpty()){
            version = versionList.get(0);
        }else {
            version = Database.getTable(CacheVersion.class).newRecord();
            version.setVersionNumber(new Bucket(1));
            version.save();
        }
        return version;
    }

    @RequireLogin(false)
    public View last(){
        CacheVersion version = getLastVersion();
        if (getReturnIntegrationAdaptor() == null){
            return back();
        }else {
            return getReturnIntegrationAdaptor().createResponse(getPath(),version);
        }
    }

    public View increment(){
        CacheVersion version = getLastVersion();
        version.getVersionNumber().increment();
        version.save();
        if (getReturnIntegrationAdaptor() == null){
            return back();
        }else {
            return getReturnIntegrationAdaptor().createResponse(getPath(),version);
        }
    }

}
