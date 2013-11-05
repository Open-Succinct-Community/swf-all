package com.venky.swf.db.model.io;

import com.venky.swf.db.model.Model;

public interface ModelWriterFactory<T> {
	public <M extends Model> ModelWriter<M,T> createModelWriter(Class<M> modelClass);
}
