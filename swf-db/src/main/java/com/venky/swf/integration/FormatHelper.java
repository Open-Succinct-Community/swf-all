package com.venky.swf.integration;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.io.ModelIOFactory.UnsupportedMimeTypeException;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class FormatHelper<T> {
	protected FormatHelper(){
	
	}

	protected void fixInputCase(){
		KeyCase keyCase = getKeyCase();

		if (keyCase != KeyCase.CAMEL ){
			change_key_case(keyCase,KeyCase.CAMEL);
		}
	}
	protected boolean isPlural(){
		return ObjectUtil.equals(StringUtil.pluralize(getRootName()), getRootName());
	}

	protected boolean isRootElementNameRequired(){
		return Config.instance().isRootElementNameRequiredForApis();
	}
	protected KeyCase getKeyCase(){
		return Config.instance().getApiKeyCase();
	}
	protected void fixOutputCase(){
		if (!isRootElementNameRequired() && !isPlural() && getElementAttribute(getRootName()) != null) {
			T element = getElementAttribute(getRootName());
			setRoot(element);
		}
		KeyCase keyCase = getKeyCase();

		if (keyCase != KeyCase.CAMEL){
			change_key_case(KeyCase.CAMEL,keyCase);
		}
	}

	private static Map<MimeType,Class<?>> formatClassMap = new HashMap<MimeType, Class<?>>();
	private static Map<MimeType,FormatHelperBuilder<?>> formatBuilderMap = new HashMap<MimeType,FormatHelperBuilder<?>>();
	
	public static <T> void registerFormat(MimeType mimeType, Class<T> formatClass, FormatHelperBuilder<T> formatBuilder){
		formatClassMap.put(mimeType, formatClass);
		formatBuilderMap.put(mimeType, formatBuilder);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getFormatClass(MimeType mimeType){
		Class<T> formatClass = (Class<T>) formatClassMap.get(mimeType);
		if (formatClass == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return formatClass;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> MimeType getMimeType(Class<T> clazz){
		for (MimeType mime :formatClassMap.keySet()){
			Class<T> formatClass = (Class<T>) formatClassMap.get(mime);
			if (formatClass.isAssignableFrom(clazz)){
				return mime;
			}
		}
		throw new UnsupportedMimeTypeException(clazz.getName());
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
	public static final <T> FormatHelper<T> instance(MimeType mimeType,String rootName,boolean isPlural){
		FormatHelper<T> helper = null;
		FormatHelperBuilder<T> formatHelperBuilder = (FormatHelperBuilder<T>) formatBuilderMap.get(mimeType);
		if (formatHelperBuilder != null){
			helper = formatHelperBuilder.constructFormatHelper(rootName, isPlural);
		}
		if (helper == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return helper;
	}

	public static final <T>  FormatHelper<T> instance(T element){
		return instance(getMimeType(element.getClass()),element);
	}
	@SuppressWarnings("unchecked")
	public static final <T> FormatHelper<T> instance(MimeType mimeType, InputStream in){
		FormatHelper<T> helper = null;
		FormatHelperBuilder<T> formatHelperBuilder = (FormatHelperBuilder<T>) formatBuilderMap.get(mimeType);
		if (formatHelperBuilder != null){
			helper = formatHelperBuilder.constructFormatHelper(in);
		}
		if (helper == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return helper;
	}
	@SuppressWarnings("unchecked")
	public static final <T> FormatHelper<T> instance(MimeType mimeType,T element){
		FormatHelper<T> helper = null;
		FormatHelperBuilder<T> formatHelperBuilder = (FormatHelperBuilder<T>) formatBuilderMap.get(mimeType);
		if (formatHelperBuilder != null){
			helper = formatHelperBuilder.constructFormatHelper(element);
		}
		if (helper == null){
			throw new UnsupportedMimeTypeException(mimeType.toString());
		}
		return (FormatHelper<T>) helper;
	}

	public abstract void setRoot(T root);
	public abstract T getRoot();
	public abstract String getRootName();
	public abstract T changeRootName(String toName);

	public abstract T createArrayElement(String name);
	public abstract List<T> getArrayElements(String name) ;
	public abstract Set<String> getArrayElementNames();
	public abstract void removeArrayElement(String name);
	public abstract void setArrayElement(String name, List<T> elements);

	public abstract T createElementAttribute(String name);
	public abstract T getElementAttribute(String name);
	public abstract Set<String> getElementAttributeNames();

	public abstract void setAttribute(String name,T element);
	public abstract void removeElementAttribute(String name);


	public abstract void setAttribute(String name, String value);
	public abstract void setElementAttribute(String name, String value);

	public abstract Set<String> getAttributes();
	public abstract <P> P getAttribute(String name);
	public abstract boolean hasAttribute(String name);

	public boolean isArrayAttribute(String name){
		return false;
	}

	public void setAttribute(String name, String[] value) {
		throw new UnsupportedOperationException("Array attribute not supported for " + getMimeType());
	}

	public abstract void removeAttribute(String name) ;

	public String change_case(String name , KeyCase fromKeyCase, KeyCase toKeyCase){
		if (fromKeyCase == toKeyCase){
			return name;
		}

		switch (toKeyCase){
			case SNAKE:
				return LowerCaseStringCache.instance().get(StringUtil.underscorize(name));
			case LOWER_CAMEL:
				if (fromKeyCase == KeyCase.SNAKE) {
					return StringUtil.camelize(name, false);
				}else {
					return name.substring(0,1).toLowerCase() + name.substring(1);
				}
			default:
				if (fromKeyCase == KeyCase.SNAKE) {
					return StringUtil.camelize(name);
				}else {
					return name.substring(0,1).toUpperCase() + name.substring(1);
				}
		}
	}

	public T change_key_case(KeyCase fromKeyCase ,KeyCase toKeyCase){
		FormatHelper<T> helper = this;
		for (String name : helper.getAttributes()){
			if (toKeyCase == KeyCase.CAMEL && !Character.isUpperCase(name.charAt(0)) ||
				(toKeyCase == KeyCase.SNAKE  || toKeyCase == KeyCase.LOWER_CAMEL) && Character.isUpperCase(name.charAt(0))){

				boolean isArray = isArrayAttribute(name);
				Object v = getAttribute(name) ;
				String newName = change_case(name,fromKeyCase,toKeyCase);

				if (isArray) {
					helper.setAttribute(newName, (String[])v);
				}else {
					helper.setAttribute(newName,(String)v);
				}
				helper.removeAttribute(name);
			}else {
				break;
			}
		}
		for (String name : helper.getArrayElementNames()) {
			if (toKeyCase == KeyCase.CAMEL && !Character.isUpperCase(name.charAt(0)) ||
				(toKeyCase == KeyCase.LOWER_CAMEL || toKeyCase == KeyCase.SNAKE) && Character.isUpperCase(name.charAt(0))){
				List<T> childElements = helper.getArrayElements(name);
				List<T> newChildElements = new ArrayList<>();
				for (T childElement : childElements){
					newChildElements.add(FormatHelper.instance(childElement).change_key_case(fromKeyCase,toKeyCase));
				}
				String newName = change_case(name,fromKeyCase,toKeyCase);
				String rootName = getRootName();
				if (rootName != null && rootName.equals(StringUtil.pluralize(name))){
					helper.setArrayElement(name, newChildElements);
				}else {
					helper.setArrayElement(newName, newChildElements);
					helper.removeArrayElement(name);
				}
			}else {
				break;
			}

		}
		for (String name : helper.getElementAttributeNames()) {
			if (toKeyCase == KeyCase.CAMEL && !Character.isUpperCase(name.charAt(0)) ||
					((toKeyCase == KeyCase.SNAKE || toKeyCase == KeyCase.LOWER_CAMEL) && Character.isUpperCase(name.charAt(0)))){
				FormatHelper.instance(helper.getElementAttribute(name)).change_key_case(fromKeyCase,toKeyCase);

				String newName = change_case(name,fromKeyCase,toKeyCase);
				helper.setAttribute(newName,helper.getElementAttribute(name));
				helper.removeElementAttribute(name);
			}else {
				break;
			}

		}
		String name = getRootName();
		if (name != null && (toKeyCase == KeyCase.CAMEL && !Character.isUpperCase(name.charAt(0)) ||
							 ((toKeyCase == KeyCase.SNAKE || toKeyCase == KeyCase.LOWER_CAMEL)  && Character.isUpperCase(name.charAt(0))))){
			String newName = change_case(name,fromKeyCase,toKeyCase);
			helper.changeRootName(newName);
		}
		return helper.getRoot();
	}

}
