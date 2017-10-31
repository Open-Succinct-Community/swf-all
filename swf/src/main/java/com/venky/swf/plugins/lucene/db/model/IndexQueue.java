package com.venky.swf.plugins.lucene.db.model;

import com.venky.swf.db.model.Model;

import java.io.InputStream;

public interface IndexQueue extends Model{
    public InputStream getIndexTask();
    public void setIndexTask(InputStream inputStream);

}
