package com.venky.swf.plugins.lucene.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.RecordNotFoundException;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import com.venky.swf.views.View;

public class IndexDirectoriesController extends ModelController<IndexDirectory> {
    public IndexDirectoriesController(Path path) {
        super(path);
    }
    public View destroy(String directoryName){
        IndexDirectory directory = Database.getTable(getModelClass()).newRecord();
        directory.setName(directoryName);
        directory = Database.getTable(getModelClass()).find(directory,true);
        if (directory == null) {
            throw new RecordNotFoundException();
        }
        destroy(directory);
        return getSuccessView();
    }
}
