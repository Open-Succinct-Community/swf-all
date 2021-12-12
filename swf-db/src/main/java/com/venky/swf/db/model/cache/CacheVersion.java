package com.venky.swf.db.model.cache;

import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Select;

import java.util.List;

public interface CacheVersion extends Model {
    public Bucket getVersionNumber();
    public void setVersionNumber(Bucket version);

    public static CacheVersion getLastVersion(){
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
}
