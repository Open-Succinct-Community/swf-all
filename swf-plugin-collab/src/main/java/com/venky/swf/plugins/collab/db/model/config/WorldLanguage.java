package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.model.Model;

public interface WorldLanguage extends Model {
    @UNIQUE_KEY
    @Index
    public String getName();
    public void setName(String name);

    @UNIQUE_KEY("ISO")
    @Index
    public String getIsoCode();
    public void setIsoCode(String isoCode);

    @UNIQUE_KEY("ISO2")
    @Index
    public String getIsoCode2();
    public void setIsoCode2(String isoCode);

}
