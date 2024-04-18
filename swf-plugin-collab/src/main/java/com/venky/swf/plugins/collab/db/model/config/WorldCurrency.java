package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.model.Model;

public interface WorldCurrency extends Model {

    @Index
    public String getName();
    public void setName(String name);

    @Index
    public String getCode();
    public void setCode(String code);

    @Index
    public String getSymbol();
    public void setSymbol(String symbol);
}
