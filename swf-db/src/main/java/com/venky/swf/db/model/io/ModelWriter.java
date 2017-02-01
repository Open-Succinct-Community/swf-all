package com.venky.swf.db.model.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.venky.swf.db.model.Model;

public interface ModelWriter<M extends Model,T> {
	public void write (M record, T into , List<String> fields);
	public void write (M record, T into , List<String> fields,Map<Class<? extends Model>,List<String>> childfields);
	public void write (List<M> records,OutputStream os, List<String> fields) throws IOException;
	public void write (List<M> records,OutputStream os, List<String> fields, Map<Class<? extends Model>,List<String>> childfields) throws IOException ;
}
