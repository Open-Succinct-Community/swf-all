package com.venky.swf.db.model.application.api;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.model.Model;

public interface OpenApi extends Model {

    @UNIQUE_KEY
    public String getName();
    public void setName(String name);


    @IS_VIRTUAL
    public String getSpecificationUrl();


    @WATERMARK("/openApis/name_of_the_spec.yaml")
    public String getSpecificationLocation();
    public void setSpecificationLocation(String specificationLocation);

    static OpenApi  find(String name){
        return find(name,OpenApi.class);
    }
    static <T extends OpenApi>  T find(String name,Class<T> clazz){
        T openApi = Database.getTable(clazz).newRecord();
        openApi.setName(name);
        openApi = Database.getTable(clazz).find(openApi,false);
        if (openApi == null){
            throw new RuntimeException(" Open api " + openApi + " not found!");
        }
        return openApi;
    }

}
