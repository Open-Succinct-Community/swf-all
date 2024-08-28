package com.venky.swf.db.model.io;

import java.util.HashMap;
import java.util.Map;

import com.venky.swf.db.model.Model;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.routing.Config;


public class ModelIOFactory {
	private static Map<Class<?>, ModelReaderFactory<?>> readerfactories = new HashMap<Class<?>,ModelReaderFactory<?>>();
	private static Map<Class<?>, ModelWriterFactory<?>> writerfactories = new HashMap<Class<?>,ModelWriterFactory<?>>();
	
	public static <T> void registerIOFactories(Class<T> formatClass, ModelReaderFactory<T>readerFactory, ModelWriterFactory<T> writerFactory){
		readerfactories.put(formatClass, readerFactory);
		writerfactories.put(formatClass,writerFactory);
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model, T> ModelReader<M, T> getReader(Class<M> modelClass, Class<T> formatClass){
		ModelReader<M, T> reader = null; 
		ModelReaderFactory<T> readerFactory = (ModelReaderFactory<T>) readerfactories.get(formatClass);
		if (readerFactory != null){
			reader = (ModelReader<M, T>) readerFactory.createModelReader(modelClass);
		}
		
		if (reader == null){
			throw new UnsupportedMimeTypeException("No Reader available for Mimetype:" +  FormatHelper.getMimeType(formatClass).toString());
		}
		reader.setInvalidReferencesAllowed(Config.instance().getBooleanProperty("swf.db.auto.create.references", false));
		return reader;
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model,T>  ModelWriter<M,T> getWriter(Class<M> modelClass, Class<T> formatClass){
		ModelWriter<M, T> writer = null ;
		ModelWriterFactory<T> writerFactory = (ModelWriterFactory<T>) writerfactories.get(formatClass);
		if (writerFactory != null){
			writer = (ModelWriter<M, T>)writerFactory.createModelWriter(modelClass);
		}
		
		if (writer == null){
			throw new UnsupportedMimeTypeException("No Writer available for Mimetype:" +  FormatHelper.getMimeType(formatClass).toString());
		}
		return writer;
	}
	public static class UnsupportedMimeTypeException extends RuntimeException {
		private static final long serialVersionUID = 5295876737650879813L;
		public UnsupportedMimeTypeException(String message){
			super(message);
		}
	}
}
