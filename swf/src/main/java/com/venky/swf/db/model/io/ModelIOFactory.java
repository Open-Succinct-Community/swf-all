package com.venky.swf.db.model.io;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.json.JSONModelReader;
import com.venky.swf.db.model.io.json.JSONModelWriter;
import com.venky.swf.db.model.io.xls.XLSModelReader;
import com.venky.swf.db.model.io.xls.XLSModelWriter;
import com.venky.swf.db.model.io.xml.XMLModelReader;
import com.venky.swf.db.model.io.xml.XMLModelWriter;
import com.venky.swf.integration.FormatHelper;


public class ModelIOFactory {
	
	@SuppressWarnings("unchecked")
	public static <M extends Model, T, R extends ModelReader<M,T>> R getReader(Class<M> modelClass, Class<T> formatClass){
		R reader = null ;
		MimeType mimeType = FormatHelper.getMimeType(formatClass);
		switch (mimeType) {
			case APPLICATION_XLS:
				reader =  (R) new XLSModelReader<M>(modelClass);
				break;
			case APPLICATION_XML:
				reader =  (R) new XMLModelReader<M>(modelClass);
				break;
			case APPLICATION_JSON:
				reader =  (R) new JSONModelReader<M>(modelClass); 
				break;
			default:
				break;
		} 
		if (reader == null){
			throw new UnsupportedMimeTypeException("No Reader available for Mimetype:" +  mimeType.toString());
		}
		return reader;
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model,T , W extends ModelWriter<M,T>> W getWriter(Class<M> modelClass, Class<T> formatClass){
		W writer = null ;
		MimeType mimeType = FormatHelper.getMimeType(formatClass);
		switch (mimeType) {
			case APPLICATION_XLS:
				writer =  (W)new XLSModelWriter<M>(modelClass);
				break;
			case APPLICATION_XML:
				writer =  (W)new XMLModelWriter<M>(modelClass);
				break;
			case APPLICATION_JSON:
				writer =  (W)new JSONModelWriter<M>(modelClass);
				break;
			default:
				break;
		}
		if (writer == null){
			throw new UnsupportedMimeTypeException("No Writer available for Mimetype:" +  mimeType.toString());
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
