package com.venky.swf.db.model.io;

import com.venky.swf.db.model.Model;


public interface ModelReaderFactory<T> {
	public <M extends Model> ModelReader<M,T> createModelReader(Class<M> modelClass);
}
