package com.venky.swf.db.model.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.FormatHelper;

public abstract class AbstractModelWriter<M extends Model,T> extends ModelIO<M> implements ModelWriter<M, T>{

	protected AbstractModelWriter(Class<M> beanClass) {
		super(beanClass);
	}
	@SuppressWarnings("unchecked")
	public Class<T> getFormatClass(){
		ParameterizedType pt = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<T>) pt.getActualTypeArguments()[1];
	}
	
	public MimeType getMimeType(){
		return FormatHelper.getMimeType(getFormatClass());
	}
	
	private List<String> getFields(List<String> includeFields) {
		List<String> fields = includeFields;
		if (fields == null){
			fields = getReflector().getFields();
			Iterator<String> fi = fields.iterator();
			while (fi.hasNext()){
				String field = fi.next();
				if (getReflector().isFieldHidden(field) && !"ID".equalsIgnoreCase(field)){
					fi.remove();
				}
			}
		}
		return fields;
	}
	

	public void write(List<M> records, OutputStream os,List<String> fields) throws IOException {
		Map<Class<? extends Model> , List<String>> mapFields = new HashMap<Class<? extends Model>, List<String>>();
		Set<Class<? extends Model>> parentsWritten = new HashSet<>();
		write (records,os,fields,parentsWritten,mapFields);
	}
	public void write (List<M> records,OutputStream os, List<String> fields, Set<Class<? extends Model>> ignoreParents , Map<Class<? extends Model>,List<String>> childFields) throws IOException {
		FormatHelper<T> helper  = FormatHelper.instance(getMimeType(),StringUtil.pluralize(getBeanClass().getSimpleName()),true);
		for (M record: records){
			T childElement = helper.createChildElement(getBeanClass().getSimpleName());
			write(record,childElement,fields,ignoreParents, childFields);
		}
		os.write(helper.toString().getBytes());
	}
	public void write(M record,T into, List<String> fields){
		write(record,into,fields,new HashSet<>(), new HashMap<>());
	}
	private boolean isParentIgnored(Class<? extends Model> parent, Set<Class<? extends Model>> ignoredParents) {
		for (Class<? extends Model> ignoredParent : ignoredParents) {
			if (ObjectUtil.equals(ignoredParent.getSimpleName(),parent.getSimpleName())) {
				return true;
			}
		}
		return false;
	}
	public void write(M record,T into, List<String> fields, Set<Class<? extends Model>> ignoreParents , Map<Class<? extends Model>,List<String>> childFields) {
		FormatHelper<T> formatHelper = FormatHelper.instance(into);
		ModelReflector<M> ref = getReflector();
		for (String field: getFields(fields)){
			Object value = ref.get(record, field);
			if (value == null){
				continue;
			}
			Method fieldGetter = ref.getFieldGetter(field);
			Method referredModelGetter = ref.getReferredModelGetterFor(fieldGetter);
			if (referredModelGetter != null){
				Class<? extends Model> aParent = ref.getReferredModelClass(referredModelGetter);
				if (!isParentIgnored(aParent,ignoreParents)) {
					String refElementName = referredModelGetter.getName().substring("get".length());
					T refElement = formatHelper.createElementAttribute(refElementName);
					write(aParent , ((Number)value).intValue(),refElement);
				}
			}else {
				String attributeName = getAttributeName(field);
                String sValue = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(fieldGetter.getReturnType()).getTypeConverter().toStringISO(value);
				formatHelper.setAttribute(attributeName, sValue);
			}
		}

		if (!childFields.isEmpty()){
			ignoreParents.add(ref.getModelClass());
			List<Method> childGetters = ref.getChildGetters();
			for (Method childGetter : childGetters) {
				write(formatHelper,record,childGetter,ignoreParents,childFields);
			}
		}		
	}
	private <R extends Model> void write(FormatHelper<T> formatHelper, M record, Method childGetter, Set<Class<? extends Model>> parentsWritten ,  Map<Class<? extends Model>, List<String>> childFields){
		@SuppressWarnings("unchecked")
		Class<R> childModelClass = (Class<R>) getReflector().getChildModelClass(childGetter);
		if (!childFields.containsKey(childModelClass)){
			return;
		}
		
		ModelWriter<R,T> childWriter = ModelIOFactory.getWriter(childModelClass, getFormatClass());
		try {
			@SuppressWarnings("unchecked")
			List<R> children = (List<R>)childGetter.invoke(record);
			for (R child : children){
				T childElement = formatHelper.createChildElement(childModelClass.getSimpleName());
				childWriter.write(child,childElement,childFields.get(childModelClass),parentsWritten,childFields);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private <R extends Model> void write(Class<R> referredModelClass, int id , T referredModelElement){
		Class<T> formatClass = getFormatClass();
		ModelWriter<R,T> writer = ModelIOFactory.getWriter(referredModelClass,formatClass);
		R referredModel = Database.getTable(referredModelClass).get(id);
		ModelReflector<R> referredModelReflector = ModelReflector.instance(referredModelClass);
		List<String> uniqueFields = referredModelReflector.getUniqueFields(); 
		for (Iterator<String> fi = uniqueFields.iterator(); fi.hasNext();){
			String f = fi.next();
			if (referredModelReflector.isFieldHidden(f)){
				fi.remove();
			}
		}
		uniqueFields.add("ID");
		writer.write(referredModel , referredModelElement, uniqueFields);
	}
	
}
