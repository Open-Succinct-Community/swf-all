package com.venky.swf.db.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.venky.swf.db.model.Model;

public interface ModelReader<M extends Model,T> {
	public List<M> read(InputStream in) throws IOException;
	public List<M> read(InputStream in, String rootElementName) throws IOException;
	public M read(T source);
}
