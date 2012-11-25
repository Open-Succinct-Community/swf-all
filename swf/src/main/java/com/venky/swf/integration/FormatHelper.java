package com.venky.swf.integration;

import java.lang.reflect.ParameterizedType;
import java.util.Set;

import org.json.simple.JSONObject;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.io.ModelIOFactory.UnsupportedMimeTypeException;
import com.venky.xml.XMLElement;


public abstract class FormatHelper<T> {
	protected FormatHelper(){
	
	}
	
	@SuppressWarnings("unchecked")
	public Class<T> getFormatClass(){
		ParameterizedType pt = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<T>) pt.getActualTypeArguments()[0];
	}
	
	public MimeType getMimeType(){
		return getMimeType(getFormatClass());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getFormatClass(MimeType mimeType){
		Class<?> formatClass = null; 
		switch (mimeType){
		case APPLICATION_XML:
			formatClass = XMLElement.class;
			break;
		case APPLICATION_JSON:
			formatClass = JSONObject.class;
			break;
		default: 
			break;
		}
		if (formatClass == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return (Class<T>) formatClass;
	}
	
	public static <T> MimeType getMimeType(Class<T> clazz){
		if (XMLElement.class.isAssignableFrom(clazz)){
			return MimeType.APPLICATION_XML;
		}else if (JSONObject.class.isAssignableFrom(clazz)){
			return MimeType.APPLICATION_JSON;
		}
		throw new UnsupportedMimeTypeException(clazz.getName());
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> FormatHelper<T> instance(MimeType mimeType,String rootName,boolean isPlural){
		FormatHelper<?> helper = null;
		switch (mimeType){
		case APPLICATION_XML:
			helper = new XML(rootName,isPlural);
			break;
		case APPLICATION_JSON:
			helper = new JSON(rootName,isPlural);
			break;
		default:
			break;
		}
		if (helper == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return (FormatHelper<T>) helper;
	}

	public static final <T>  FormatHelper<T> instance(T element){
		return instance(getMimeType(element.getClass()),element);
	}

	@SuppressWarnings("unchecked")
	public static final <T> FormatHelper<T> instance(MimeType mimeType,T element){
		FormatHelper<?> helper = null;
		switch (mimeType){
		case APPLICATION_XML:
			helper = new XML((XMLElement)element);
			break;
		case APPLICATION_JSON:
			helper = new JSON((JSONObject)element);
			break;
		default:
			break;
		}
		if (helper == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return (FormatHelper<T>) helper;
	}
	
	public abstract T getRoot();
	public abstract T createChildElement(String name);
	
	public abstract T createElementAttribute(String name);
	public abstract T getElementAttribute(String name);
	
	public abstract void setAttribute(String name, String value);
	
	public abstract Set<String> getAttributes();
	public abstract String getAttribute(String name);
	
}
