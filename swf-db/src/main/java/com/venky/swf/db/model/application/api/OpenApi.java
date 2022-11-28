package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface OpenApi extends Model {

    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    
    public String getSpecificationUrl();
    public void setSpecificationUrl(String specificationUrl);


}
