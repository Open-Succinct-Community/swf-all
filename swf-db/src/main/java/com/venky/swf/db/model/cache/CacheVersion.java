package com.venky.swf.db.model.cache;

import com.venky.core.util.Bucket;
import com.venky.swf.db.model.Model;

public interface CacheVersion extends Model {
    public Bucket getVersionNumber();
    public void setVersionNumber(Bucket version);
}
