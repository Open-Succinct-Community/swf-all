package com.venky.swf.db.model.io;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerUtils;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelper.KeyCase;
import com.venky.swf.routing.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModelWriter<M extends Model,T> extends ModelIO<M> implements ModelWriter<M, T>{
	boolean parentIdExposed = true;
	public void setParentIdExposed(boolean parentIdExposed){
		this.parentIdExposed = parentIdExposed;
	}
	public boolean isParentIdExposed() {
		return this.parentIdExposed;
	}
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
	private List<String> getFields(List<String> includeFields){
		return getFields(getReflector(),includeFields);
	}
	private static <R extends Model> List<String> getFields(ModelReflector<R> reflector, List<String> includeFields) {
		List<String> fields = includeFields;
		if (fields == null){
			fields = reflector.getVisibleFields();
		}
		return fields;
	}
	

	public void write(List<M> records, OutputStream os,List<String> fields) throws IOException {
		FormatHelper<T> helper  = FormatHelper.instance(getMimeType(),StringUtil.pluralize(getBeanClass().getSimpleName()),true);
		write (records,helper.getRoot(),fields);
		os.write(helper.toString().getBytes());
	}

	public void write(List<M> records, T into,List<String> fields) throws IOException {
		Map<Class<? extends Model> , List<String>> mapFields = new HashMap<>();
		Set<Class<? extends Model>> parentsWritten = new HashSet<>();
		write (records,into,fields,parentsWritten,mapFields);
	}

	public void write (List<M> records,OutputStream os, List<String> fields, Set<Class<?extends Model>> parentsAlreadyConsidered,
					   Map<Class<? extends Model>,List<String>> templateFields) throws IOException {
		FormatHelper<T> helper  = FormatHelper.instance(getMimeType(),StringUtil.pluralize(getBeanClass().getSimpleName()),true);
		write(records,helper.getRoot(),fields,parentsAlreadyConsidered,templateFields);
		os.write(helper.toString().getBytes());

	}
	public void write (List<M> records,T into, List<String> fields, Set<Class<?extends Model>> parentsAlreadyConsidered,
					   Map<Class<? extends Model>,List<String>> templateFields) throws IOException {
		write(records,into,fields,parentsAlreadyConsidered,getChildrenToConsider(templateFields),templateFields);
	}

	public void write (List<M> records,OutputStream os, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered ,
					   Map<Class<? extends Model>,List<Class<? extends Model>>> childrenToBeConsidered,
					   Map<Class<? extends Model>,List<String>> templateFields) throws IOException {
		FormatHelper<T> helper  = FormatHelper.instance(getMimeType(),StringUtil.pluralize(getBeanClass().getSimpleName()),true);
		write(records,helper.getRoot(),fields,parentsAlreadyConsidered,childrenToBeConsidered,templateFields);
		os.write(helper.toString().getBytes());
	}
	public void write (List<M> records,T into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered ,
					   Map<Class<? extends Model>,List<Class<? extends Model>>> childrenToBeConsidered,
					   Map<Class<? extends Model>,List<String>> templateFields) throws IOException {
		FormatHelper<T> helper  = FormatHelper.instance(into);
		for (M record: records){
			T childElement = helper.createArrayElement(getBeanClass().getSimpleName());
			write(record,childElement,fields,parentsAlreadyConsidered, childrenToBeConsidered, templateFields);
		}
		if (Config.instance().getBooleanProperty("swf.api.keys.title_case",false)){
			helper.change_key_case(KeyCase.TITLE);
		}
	}

	public void write(M record,T into, List<String> fields){
		write(record,into,fields,new HashSet<>(), new HashMap<>());
	}
	private boolean isParentIgnored(Class<? extends Model> parent, Set<String> ignoredParents) {
		return ignoredParents.contains(parent.getSimpleName());
	}
	private final SWFLogger cat = Config.instance().getLogger(getClass().getName());
	public Map<Class<? extends Model> , List<Class<? extends Model>>> getChildrenToConsider(Map<Class<? extends Model>, List<String>> templateFields){
		return getReflector().getChildrenToBeConsidered(templateFields);
	}
	public void write(M record,T into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered , Map<Class<? extends Model>,List<String>> templateFields) {
		//Consider first level children.
		write(record,into,fields,parentsAlreadyConsidered,getChildrenToConsider(templateFields),templateFields);
	}
	public void write(M record,T into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered ,
					  Map<Class<? extends Model>, List<Class<? extends  Model>>> considerChildren,
					  Map<Class<? extends Model>, List<String>> templateFields) {

		Set<String> simplifiedParentsConsidered = new HashSet<>();
		parentsAlreadyConsidered.forEach(c->simplifiedParentsConsidered.add(c.getSimpleName()));

		Map<String,List<String>> simplifiedConsiderChildren = new Cache<String, List<String>>() {
			@Override
			protected List<String> getValue(String s) {
				return new SequenceSet<>();
			}
		};
		considerChildren.forEach((m,l)->{
			for (Class<? extends Model> child: l){
				simplifiedConsiderChildren.get(m.getSimpleName()).add(child.getSimpleName());
			}
		});
		Map<String,List<String>> simplifiedTemplateFields = new Cache<String, List<String>>() {
			@Override
			protected List<String> getValue(String s) {
				return new SequenceSet<>();
			}
		};

		templateFields.forEach((m,fl)->{
			for (String f: fl != null ? fl : ModelReflector.instance(m).getVisibleFields()){
				simplifiedTemplateFields.get(m.getSimpleName()).add(f);
			}
		});
		writeSimplified(record,into,fields,simplifiedParentsConsidered,simplifiedConsiderChildren,simplifiedTemplateFields);

	}
	public void writeSimplified(M record,T into, List<String> fields,
					  Set<String> parentsAlreadyConsidered ,
					  Map<String, List<String>> considerChildren,
					  Map<String, List<String>> templateFields) {


		FormatHelper<T> formatHelper = FormatHelper.instance(into);
		ModelReflector<M> ref = getReflector();
		for (String field: getFields(fields)){
			Object value = TimerUtils.time(cat,"ref.get", ()-> ref.get(record, field));
			if (value == null){
				continue;
			}
			Method fieldGetter = TimerUtils.time(cat,"getFieldGetter" , () ->ref.getFieldGetter(field));
			Method referredModelGetter = TimerUtils.time(cat,"getReferredModelGetterFor" , ()->ref.getReferredModelGetterFor(fieldGetter));

			if (referredModelGetter != null){
				Class<? extends Model> aParent = ref.getReferredModelClass(referredModelGetter);
				if (!isParentIgnored(aParent,parentsAlreadyConsidered) || fields != null) {
					String refElementName = referredModelGetter.getName().substring("get".length());
					T refElement = formatHelper.createElementAttribute(refElementName);
					parentsAlreadyConsidered.add(aParent.getSimpleName());
					try {
						write(aParent, ((Number) value).longValue(), refElement, parentsAlreadyConsidered, considerChildren,templateFields);
					}finally {
						parentsAlreadyConsidered.remove(aParent.getSimpleName());
					}
				}
			}else {
				String attributeName = TimerUtils.time(cat,"getAttributeName()" , ()->getAttributeName(field));
				String sValue = TimerUtils.time(cat, "toStringISO" ,
						() -> Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(fieldGetter.getReturnType()).getTypeConverter().toStringISO(value));

				if (InputStream.class.isAssignableFrom(fieldGetter.getReturnType())) {
				    formatHelper.setElementAttribute(attributeName,sValue);
                }else {
                    TimerUtils.time(cat,"setAttribute" , ()-> {
						formatHelper.setAttribute(attributeName, sValue);
						return true;
					});
                }
			}
		}

		TimerUtils.time(cat,"Writing all Children Objects",()->{
			if (!templateFields.isEmpty()){
				parentsAlreadyConsidered.add(ref.getModelClass().getSimpleName());
				try {
					List<Method> childGetters = ref.getChildGetters();
					for (Method childGetter : childGetters) {
						write(formatHelper,record,childGetter,parentsAlreadyConsidered,considerChildren,templateFields);
					}
				}finally {
					parentsAlreadyConsidered.remove(ref.getModelClass().getSimpleName());
				}
			}
			return  true;
		});
	}
	private <R extends  Model> boolean containsChild(Class<R> childModelClass, Collection<String> considerChildren){
		return considerChildren != null && considerChildren.contains(childModelClass.getSimpleName());
	}
	private <R extends Model> void write(FormatHelper<T> formatHelper, M record, Method childGetter, Set<String> parentsWritten ,
										 Map<String, List<String>> considerChildren,
										 Map<String, List<String>> templateFields){
		@SuppressWarnings("unchecked")
		Class<R> childModelClass = (Class<R>) getReflector().getChildModelClass(childGetter);
		if (!containsChild(childModelClass,considerChildren.get(getBeanClass().getSimpleName()))){
			return;
		}

		if (!containsChild(childModelClass,templateFields.keySet())){
			return;
		}
		
		ModelWriter<R,T> childWriter = ModelIOFactory.getWriter(childModelClass, getFormatClass());
		try {
			@SuppressWarnings("unchecked")
			List<R> children = (List<R>)childGetter.invoke(record);
			List<String> fields = new ArrayList<>();
			List<String> f = templateFields.get(childModelClass.getSimpleName());
			if (f != null) {
				fields.addAll(f);
			}
			if (fields.isEmpty()){
				fields = null;
			}
			Map<String,List<String>> newConsiderChilden = new HashMap<>(considerChildren);
			newConsiderChilden.remove(getBeanClass().getSimpleName()); //Don't print children of parent via children. !! Duplication.
			for (R child : children) {
				T childElement = formatHelper.createArrayElement(childModelClass.getSimpleName());
				childWriter.writeSimplified(child, childElement, fields, parentsWritten, newConsiderChilden, templateFields);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private <R extends Model> void write(Class<R> referredModelClass, long id, T referredModelElement,  Set<String> parentsAlreadyConsidered,
										 Map<String, List<String>> considerChildren,
										 Map<String, List<String>> templateFields){
		Class<T> formatClass = getFormatClass();
		ModelWriter<R,T> writer = ModelIOFactory.getWriter(referredModelClass,formatClass);
		R referredModel = Database.getTable(referredModelClass).get(id);
		if (referredModel == null){
			return;
		}
		ModelReflector<R> referredModelReflector = ModelReflector.instance(referredModelClass);

		List<String> parentFieldsToAdd = referredModelReflector.getUniqueFields();
		parentFieldsToAdd.removeIf(referredModelReflector::isFieldHidden);

		if (isParentIdExposed() || parentFieldsToAdd.isEmpty()) {
			parentFieldsToAdd.add("ID");
		}

		if (templateFields != null){
			List<String> parentFieldsToAddBasedOnTemplate = new SequenceSet<>();
			if (templateFields.get(referredModelClass.getSimpleName()) != null) {
				//Never add parent class with null template.
				parentFieldsToAddBasedOnTemplate.addAll(getFields(referredModelReflector,templateFields.get(referredModelClass.getSimpleName())));
			}
			if (!parentFieldsToAddBasedOnTemplate.isEmpty()){
				parentFieldsToAdd.clear();
				parentFieldsToAdd.addAll(parentFieldsToAddBasedOnTemplate);
			}
        }
		//* Which parent's children to include */
        List<String> newChildModelsToConsider = new SequenceSet<>();
		if (considerChildren != null){
			newChildModelsToConsider = considerChildren.getOrDefault(referredModelClass.getSimpleName(),newChildModelsToConsider);
		}

		List<Class<? extends Model>> childModels = referredModelReflector.getChildModels();
		Map<String, List<String>> newTemplateFields = null;
		if (templateFields != null){
			newTemplateFields = new HashMap<>(templateFields);
			for (Class<? extends  Model> childModelClass : childModels){
				boolean keepChildModelClassInTemplate = newChildModelsToConsider.contains(childModelClass.getSimpleName());
				if (!keepChildModelClassInTemplate){
					newTemplateFields.remove(childModelClass.getSimpleName());
				}
			}
		}

		writer.writeSimplified(referredModel , referredModelElement, parentFieldsToAdd, parentsAlreadyConsidered, considerChildren, newTemplateFields);
	}
	
}
