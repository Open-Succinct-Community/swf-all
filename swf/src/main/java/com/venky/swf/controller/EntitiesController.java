package com.venky.swf.controller;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.entity.Entities;
import com.venky.swf.db.model.entity.Entity;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.JSON;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class EntitiesController extends Controller{
    public EntitiesController(Path path){
        super(path);
    }
    
    public View index(){
        Entities entities = new Entities();

        Database.getTableNames().forEach(name->{
            ModelReflector<? extends Model> ref = Objects.requireNonNull(Database.getTable(name)).getReflector();
            entities.add(meta(null,ref));
        });
        JSON json = new JSON(entities.getInner());

        return new BytesView(getPath(),json.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
    }
    public View search(String q){
        Entities entities = new Entities();

        Database.getTableNames().forEach(name->{
            if (name.matches("^.*%s.*$".formatted(q.toUpperCase()))) {
                ModelReflector<? extends Model> ref = Objects.requireNonNull(Database.getTable(name)).getReflector();
                entities.add(meta(null, ref));
            }
        });
        JSON json = new JSON(entities.getInner());

        return new BytesView(getPath(),json.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
    }

    public View describe(String name){
        ModelReflector<? extends Model> ref = Objects.requireNonNull(Database.getTable(name)).getReflector();
        Entity entity = meta(null,ref);
        JSON json = new JSON(entity.getInner());

        return new BytesView(getPath(),json.toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
    }
    public View erd(String name){
        ModelReflector<? extends Model> ref = Objects.requireNonNull(Database.getTable(name)).getReflector();
        return erd(ref);
    }



}
