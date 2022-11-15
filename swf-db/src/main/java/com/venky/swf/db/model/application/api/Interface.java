package com.venky.swf.db.model.application.api;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

import java.io.Reader;

public interface Interface extends Model {

    public String getName();
    public void setName(String name);

    @Enumeration("OpenApi")
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "OpenApi")
    public String getSpecificationName();
    public void setSpecificationName(String specificationName);


    public Reader getSpecificationContent();
    public void setSpecificationContent(Reader specificationContent);


}
