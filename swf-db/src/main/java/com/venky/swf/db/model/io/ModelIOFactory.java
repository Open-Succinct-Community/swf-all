package com.venky.swf.db.model.io;

import java.util.HashMap;
import java.util.Map;

import com.venky.swf.db.model.Model;
import com.venky.swf.integration.FormatHelper;


public class ModelIOFactory {
	private static Map<Class<?>, ModelReaderFactory<?>> readerfactories = new HashMap<Class<?>,ModelReaderFactory<?>>();
	private static Map<Class<?>, ModelWriterFactory<?>> writerfactories = new HashMap<Class<?>,ModelWriterFactory<?>>();
	
	public static <T> void registerIOFactories(Class<T> formatClass, ModelReaderFactory<T>readerFactory, ModelWriterFactory<T> writerFactory){
		readerfactories.put(formatClass, readerFactory);
		writerfactories.put(formatClass,writerFactory);
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model, T, R extends ModelReader<M,T>> R getReader(Class<M> modelClass, Class<T> formatClass){
		R reader = null; 
		ModelReaderFactory<T> readerFactory = (ModelReaderFactory<T>) readerfactories.get(formatClass);
		if (readerFactory != null){
			reader = (R) readerFactory.createModelReader(modelClass);
		}
		
		if (reader == null){
			throw new UnsupportedMimeTypeException("No Reader available for Mimetype:" +  FormatHelper.getMimeType(formatClass).toString());
		}
		return reader;
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model,T , W extends ModelWriter<M,T>> W getWriter(Class<M> modelClass, Class<T> formatClass){
		W writer = null ;
		ModelWriterFactory<T> writerFactory = (ModelWriterFactory<T>) writerfactories.get(formatClass);
		if (writerFactory != null){
			writer = (W)writerFactory.createModelWriter(modelClass);
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
