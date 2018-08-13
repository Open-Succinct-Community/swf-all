package com.venky.swf.db.model.reflection;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.util.*;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.DATA_TYPE;
import com.venky.swf.db.annotations.column.DECIMAL_DIGITS;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_AUTOINCREMENT;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.CLONING_PROTECT;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.defaulting.StandardDefaulter;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.model.Count;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.TableReflector.MReflector;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKey;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKeyFieldDescriptor;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.util.WordWrapUtil;

public class ModelReflector<M extends Model> {

	@SuppressWarnings("rawtypes")
	private static final Map<Class<? extends Model> , ModelReflector>  modelReflectorByModelClass = new HashMap<Class<? extends Model>, ModelReflector>();
    
    @SuppressWarnings("unchecked")
	public static <M extends Model> ModelReflector<M> instance(Class<M> modelClass){
    	ModelReflector<M> ref = modelReflectorByModelClass.get(modelClass);
    	if (ref == null){
    		synchronized (modelReflectorByModelClass) {
				ref = modelReflectorByModelClass.get(modelClass);
				if (ref == null){
					ref = new ModelReflector<M>(modelClass, TableReflector.instance(modelClass)) ;
					modelReflectorByModelClass.put(modelClass, ref);
				}
			}
    	}
    	return ref;
    }
    
	private Cache<String, SequenceSet<String>> extensionPointsCache = new Cache<String, SequenceSet<String>>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3938846263913578958L;

		@Override
		protected SequenceSet<String> getValue(String k) {
			StringTokenizer tok = new StringTokenizer(k, "@");
			String prefix = tok.nextToken();
			String suffix = tok.nextToken();
			SequenceSet<String> extnPoints = new SequenceSet<String>();
			for (Class<? extends Model> inHierarchy : getClassHierarchies()){
				String extnPoint = prefix + "."+ inHierarchy.getSimpleName() + "." + suffix;
				if (Registry.instance().hasExtensions(extnPoint)){
					extnPoints.add(extnPoint);
				}
			}
			return extnPoints;
		}
	};
	
	/** 
	 * Find extension points of the form prefix.<i>modelClass.getSimpleName()</i>.suffix for all relevant models in the right sequence.
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	public List<String> getExtensionPoints(String prefix,String suffix){
		return extensionPointsCache.get(prefix+"@"+suffix);
	}
        
	private Class<M> modelClass  ;  
	private TableReflector reflector ;
	private ModelReflector(Class<M> modelClass,TableReflector reflector){
		this.modelClass = modelClass;
		this.reflector = reflector;
		this.cat = Config.instance().getLogger(modelClass.getName());
	}
	
	private ModelReflector() {
		
	}

	public Class<M> getModelClass(){
		return modelClass;
	}

	public String getTableName() {
 		return reflector.getTableName();
 	}
 	
	public Class<? extends Model> getRealModelClass(){
		return TableReflector.getRealModelClass(getModelClass());
	}
	public SequenceSet<Class<? extends Model>> getModelClasses(){
		return reflector.getModelClasses();
	}
	public boolean reflects(Class<? extends Model> referredModelClass) {
		return reflector.reflects(referredModelClass);
	}

	public boolean canReflect(Object o) {
		return reflector.canReflect(o);
	}

    public String getDescriptionField(){
    	HAS_DESCRIPTION_FIELD descColumn = getAnnotation(HAS_DESCRIPTION_FIELD.class);
    	
        if (descColumn != null){
        	String column = descColumn.value();
            if (getFields().contains(column)){
            	return column;
            }
        }
        if (getFields().contains("NAME")){
        	return "NAME";
        }
        return "ID";
    }
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(Object o, String fieldName){
		if (o == null){
			return null;
		}
		Model record = null; 
		Record rawRecord = null ;
		if (Record.class.isInstance(o)){
			rawRecord = (Record)o;
		}else if (Proxy.isProxyClass(o.getClass()) && (o instanceof Model)){
			record = (Model)o;
		}else {
			throw new RuntimeException ("Don't know how to get " + fieldName );
		}
		if (rawRecord == null){
			rawRecord = record.getRawRecord();
		}
				
    	Timer timer = cat.startTimer();
        try {
        	T ret = (T)rawRecord.get(fieldName);
        	if (ret == null){
        		ColumnDescriptor cd = getColumnDescriptor(fieldName);
                if (!cd.isVirtual()){
                	ret = (T)rawRecord.get(cd.getName());
                }
        	} 
            Method getter = getFieldGetter(fieldName);
        	if (ret == null || !(getter.getReturnType().isAssignableFrom(ret.getClass()))) {
            	if (record == null){
            		record = rawRecord.getAsProxy(getModelClass());
            	}
            	ret = (T)getter.invoke(record); 
        	}
        	return ret;
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        } finally {
        	timer.stop();
        }

    	
    }
    public void set(Model record, String fieldName, Object value){
    	Timer timer = cat.startTimer();
        try {
            Method getter = getFieldGetter(fieldName);
            Method setter = getFieldSetter(fieldName);
    		TypeRef<?> typeRef = Database.getJdbcTypeHelper(getPool()).getTypeRef(getter.getReturnType());

        	if (!ObjectUtil.isVoid(value) || getter.getReturnType().isPrimitive() ){
                setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
        	}else {
                setter.invoke(record, getter.getReturnType().cast(null));
        	}
        } catch (Exception e1) {
            throw new RuntimeException(fieldName +":" + value , e1);
        } finally {
        	timer.stop();
        }

    }

	public void loadMethods(List<Method> into, MethodMatcher matcher) {
		Timer timer = cat.startTimer();
		try {
			reflector.loadMethods(getModelClass(), into, matcher);
		}finally{
			timer.stop();
		}
	}

    private SequenceSet<Method> fieldGetters = null; 
    public List<Method> getFieldGetters(){
    	if (fieldGetters == null){
	    	synchronized (this) {
	    		if (fieldGetters == null){
	    			SequenceSet<Method> fieldGetters = new SequenceSet<Method>();
		        	loadMethods(fieldGetters, getFieldGetterMatcher());
		        	this.fieldGetters = fieldGetters;
	    		}
			}
    	}
    	return fieldGetters;
    }
    
    private SequenceSet<String> fieldGetterSignatures = null;
    public List<String> getFieldGetterSignatures(){
    	if (fieldGetterSignatures == null){
    		synchronized (this) {
    			if (fieldGetterSignatures == null ){ 
    				SequenceSet<String> fieldGetterSignatures = new SequenceSet<String>();
            		for (Method m : getFieldGetters()){
            			fieldGetterSignatures.add(getSignature(m));
            		}
            		this.fieldGetterSignatures = fieldGetterSignatures;
    			}
			}
    	}
    	return fieldGetterSignatures;
    }
    
    private SequenceSet<Method> indexedFieldGetters = null;
    public List<Method> getIndexedFieldGetters(){
    	if (indexedFieldGetters == null) {
        	synchronized (this) {
        		if (indexedFieldGetters == null){
        			SequenceSet<Method> indexedFieldGetters =  new SequenceSet<Method>();
                	loadMethods(indexedFieldGetters, getIndexedFieldGetterMatcher());
                	this.indexedFieldGetters = indexedFieldGetters;
        		}
    		}
    	}
    	return indexedFieldGetters;
    }
    
    private SequenceSet<Method> fieldSetters = null;
    public List<Method> getFieldSetters(){
    	if (fieldSetters == null) {
    		synchronized (this) {
    			if (fieldSetters == null) {
    				SequenceSet<Method> fieldSetters =  new SequenceSet<Method>() ;
    		    	loadMethods(fieldSetters, getFieldSetterMatcher());
    		    	this.fieldSetters = fieldSetters;
    			}
			}
    	}
    	return fieldSetters;
    }
    
    private SequenceSet<Method> referredModelGetters = null;
    public List<Method> getReferredModelGetters(){ 
    	if (referredModelGetters == null ){
    		synchronized (this) {
    			if (referredModelGetters == null) {
    				SequenceSet<Method> referredModelGetters = new SequenceSet<Method>();
	            	loadMethods(referredModelGetters, getReferredModelGetterMatcher());
	            	this.referredModelGetters =  referredModelGetters;
    			}
			}
    	}
    	return referredModelGetters;
    }
    
    private SequenceSet<Method> participantModelGetters = null ;
    public List<Method> getParticipantModelGetters(){
    	if (participantModelGetters == null){
    		synchronized (this) {
    			if (participantModelGetters == null){
    				SequenceSet<Method> participantModelGetters = new SequenceSet<Method>();
	            	loadMethods(participantModelGetters, getParticipantModelGetterMatcher());
	            	this.participantModelGetters = participantModelGetters;
    			}
			}
    	}
    	return participantModelGetters;
    }
    
    private SequenceSet<Method> childModelGetters = null ;
    public List<Method> getChildGetters(){
    	if (childModelGetters == null){
    		synchronized (this) {
				if (childModelGetters == null) {
					SequenceSet<Method> childModelGetters = new SequenceSet<Method>();
		        	loadMethods(childModelGetters,getChildrenGetterMatcher());
		        	this.childModelGetters = childModelGetters;
				}
			}
    	}
    	return childModelGetters;
    }
    public List<Class<? extends Model>> getChildModels(){
    	return getChildModels(false,false);
    }
    public List<Class<? extends Model>> getChildModels(boolean onlyMultipleChildren ,boolean onlyVisible){
    	SequenceSet<Class<? extends Model>> childModels = new SequenceSet<Class<? extends Model>>();
		for (Method childGetter: getChildGetters()){
        	if (onlyMultipleChildren && !List.class.isAssignableFrom(childGetter.getReturnType())){
        		continue;
        	}
    		HIDDEN hidden = getAnnotation(childGetter, HIDDEN.class);
        	if (onlyVisible && hidden != null && hidden.value()){
        		continue;
        	}
        	Class<? extends Model> childModelClass = getChildModelClass(childGetter);
        	childModels.add(childModelClass);
        }
    	return childModels;
    }
    

    private List<String> allfields = null;
    private Map<String,List<String>> columnFields = new IgnoreCaseMap<List<String>>();
    private Map<String,String> fieldColumn = new IgnoreCaseMap<String>();

    private void loadAllFields(){
		Timer timer = cat.startTimer();
		try {
	        if (allfields != null){
	            return;
	        }
	        synchronized (this) {
	            if (allfields != null){
	                return;
	            }
	            List<Method> fieldGetters = getFieldGetters();
	            List<String> allfields = new IgnoreCaseList(false);
                
	            for (Method fieldGetter : fieldGetters){
	        		String fieldName = getFieldName(fieldGetter);
	
	        		Map<Class<? extends Annotation>,Annotation> map = getAnnotationMap(fieldGetter);
	        		COLUMN_NAME name = (COLUMN_NAME)map.get(COLUMN_NAME.class);
	        		String columnName = ( name == null ? fieldName : name.value());
	        		
	        		allfields.add(fieldName);
	        		List<String> fields = columnFields.get(columnName);
	        		if (fields == null){
	        			fields = new IgnoreCaseList(false);
	        			columnFields.put(columnName, fields);
	        		}
	        		fields.add(fieldName);
	        		fieldColumn.put(fieldName, columnName);
	            }
	            this.allfields=allfields;
	        }
		}finally {
			timer.stop();
		}
    }
    
    public List<String> getFields(){
		Timer timer = cat.startTimer();
		try {
	    	loadAllFields();
	    	return new IgnoreCaseList(false,allfields);
		}finally{
			timer.stop();
		}
    }
    
    private SequenceSet<String> indexedColumns = null;
    public List<String> getIndexedColumns(){
    	if (indexedColumns == null){
    		synchronized (this) {
    			if (indexedColumns == null) {
    				SequenceSet<String> indexedColumns = new SequenceSet<String>();
	            	for (Method indexedFieldGetter : getIndexedFieldGetters()){
	            		String indexColumnName = getColumnDescriptor(getFieldName(indexedFieldGetter)).getName();
	            		indexedColumns.add(indexColumnName);
	            	}
	            	this.indexedColumns = indexedColumns;
    			}
			}
    	}
    	return indexedColumns;
    }
    private SequenceSet<String> indexedFields = null ;
    public SequenceSet<String> getIndexedFields(){
    	if (indexedFields == null){
    		synchronized (this) {
				if (indexedFields == null){
					SequenceSet<String> indexedFields = new SequenceSet<String>();
		    		for (Method indexedFieldGetter : getIndexedFieldGetters()){
		    			indexedFields.add(getFieldName(indexedFieldGetter));
		    		}
		    		this.indexedFields = indexedFields;
				}
			}
    	}
    	return indexedFields;
    }
    private Cache<String,UniqueKey<M>> uniqueKeys = null; 
    public Collection<UniqueKey<M>> getUniqueKeys(){
    	if (uniqueKeys == null){
    		synchronized (this) {
    			if (uniqueKeys == null){
    				Cache<String, UniqueKey<M>> uniqueKeys = new Cache<String, UniqueKey<M>>() {
	    				private static final long serialVersionUID = 1892299842617679145L;
	
	    				@Override
	    				protected UniqueKey<M> getValue(String keyName) {
	    					return new UniqueKey<M>(getModelClass(), keyName);
	    				}
	    			};
	        		for (Method fieldGetter : getFieldGetters()){
	        			UNIQUE_KEY key = this.getAnnotation(fieldGetter, UNIQUE_KEY.class);
	        			if (key != null){
	            			String fieldName = getFieldName(fieldGetter);
	            			StringTokenizer keys = new StringTokenizer(key.value(),",");
	            			while (keys.hasMoreTokens()){
	            				String keyName = keys.nextToken();
	                			UniqueKey<M> uk = uniqueKeys.get(keyName);
	                			uk.addField(fieldName,key.exportable(),key.allowMultipleRecordsWithNull());
	            			}
	        			}
	        		}
	        		this.uniqueKeys = uniqueKeys;
    			}
			}
    	}
    	return uniqueKeys.values();
    }
    public <T> boolean isDirty(T recordOrProxy,String fieldName) {
    	Model record = null; 
		Record rawRecord = null ;
		if (Record.class.isInstance(recordOrProxy)){
			rawRecord = (Record)recordOrProxy;
		}else if (Proxy.isProxyClass(recordOrProxy.getClass()) && (recordOrProxy instanceof Model)){
			record = (Model)recordOrProxy;
		}else {
			throw new RuntimeException ("Don't know how to get " + fieldName );
		}
		if (rawRecord == null){
			rawRecord = record.getRawRecord();
		}
		return rawRecord.isFieldDirty(fieldName);
    }
    public <T> Collection<Expression> getUniqueKeyConditions(T recordOrProxy){
    	return getUniqueKeyConditions(recordOrProxy, false);
    }
    public <T> Collection<Expression> getUniqueKeyConditions(T recordOrProxy,boolean onlyIfContainsDirtyFields){
    	List<Expression> col = new ArrayList<Expression>();
    	for (UniqueKey<M> key : getUniqueKeys() ){
			Expression where = new Expression(getPool(),Conjunction.AND);
			boolean ignoreWhereClause = false;
			boolean ret = false;
			for (UniqueKeyFieldDescriptor<M> fd: key.getFields()){
				String fieldName = fd.getFieldName();
				ret = ret || !onlyIfContainsDirtyFields || isDirty(recordOrProxy,fieldName); 
				Object value = get(recordOrProxy, fieldName);
				if (value != null){
					where.add(new Expression(getPool(),getColumnDescriptor(fd.getFieldName()).getName(),Operator.EQ, value));
				}else {
					if (!fd.isMultipleRecordsWithNullAllowed()){
						where.add(new Expression(getPool(),getColumnDescriptor(fd.getFieldName()).getName(),Operator.EQ));
					}else {
						ignoreWhereClause = true;
						break;
					}
				}
			}
			if (!ignoreWhereClause && ret){
				col.add(where);
			}
		}
    	return col;
    }
    
    public List<String> getUniqueFields(){
    	return getUniqueFields(null);
    }
	public List<String> getUniqueFields(String keyName){
		SequenceSet<String> uniqueFields = new SequenceSet<String>();
		for (UniqueKey<M> key : getUniqueKeys()) {
			if (keyName == null || key.getKeyName().equalsIgnoreCase(keyName)){
				for (UniqueKeyFieldDescriptor<M> fd: key.getFields()){
					uniqueFields.add(fd.getFieldName());
				}
				if (keyName != null) {
					break;
				}
			}
		}
		return uniqueFields;
	}

    private List<UniqueKey<M>> singleColumnUniqueKeys = null; 
    public Collection<UniqueKey<M>> getSingleColumnUniqueKeys(){
    	if (singleColumnUniqueKeys == null){
    		synchronized (this) {
    			if (singleColumnUniqueKeys == null){
    				ArrayList<UniqueKey<M>> singleColumnUniqueKeys = new ArrayList<UniqueKey<M>>();
            		for (UniqueKey<M> key: getUniqueKeys()){
            			if (key.size() == 1){
            				singleColumnUniqueKeys.add(key);
            			}
            		}
            		this.singleColumnUniqueKeys = singleColumnUniqueKeys;
    			}
			}
    	}
    	return singleColumnUniqueKeys;
    }
    
    private List<String> editableFields = null;
    public List<String> getEditableFields(){
    	if (editableFields == null){
    		synchronized (this) {
    			SequenceSet<String> editableFields = new SequenceSet<String>();
            	for (String field:getFields()){
            		if (isFieldEditable(field)){
            			editableFields.add(field);
            		}
            	}
            	this.editableFields = editableFields;
			}
    	}
    	return editableFields;
    }
    public List<String> getRealFields(){
        return getFields(new RealFieldMatcher());
    }
    public List<String> getVirtualFields(){
        return getFields(new VirtualFieldMatcher());
    }
    public List<String> getFields(FieldMatcher matcher){
		loadAllFields();
        List<String> fields = new IgnoreCaseList(false);
        for (String field: allfields){
            if (matcher == null || matcher.matches(getColumnDescriptor(field))){
                fields.add(field);
            }
        }
        return fields;
    }
    public List<String> getRealColumns(){
    	return getColumns(new RealFieldMatcher());
    }
    
    public List<String> getColumns(FieldMatcher matcher){
		Timer timer = cat.startTimer();
		try {
	    	List<String> fields = getFields(matcher);
	    	List<String> columns = new IgnoreCaseList(false);
	    	for (String field:fields){
	    		columns.add(getColumnDescriptor(field).getName());
	    	}
	    	return columns;
		}finally{
			timer.stop();
		}
    }

    public boolean isFieldExportable(String fieldName){
    	Method fieldGetter = getFieldGetter(fieldName);
    	EXPORTABLE exportable = getAnnotation(fieldGetter,EXPORTABLE.class);
    	if (exportable != null && !exportable.value()){
    		return false;
    	}
    	if (isHouseKeepingField(fieldName)){
    		if (getUniqueKeys().size() > 0 || !"ID".equals(fieldName)){
    			return false;
    		}
    	}
    	return true;
    }
    
    public boolean isFieldCopiedWhileCloning(String fieldName){
    	Method fieldGetter = getFieldGetter(fieldName);
		CLONING_PROTECT cloningProtect = getAnnotation(fieldGetter, CLONING_PROTECT.class);
		boolean protectedFromCloning = false; 
		
		if (cloningProtect == null){
			protectedFromCloning = isHouseKeepingField(fieldName);
		}else {
			protectedFromCloning = cloningProtect.value();
		}
		
		return !protectedFromCloning;
    }
    
    public boolean isFieldMandatory(String fieldName){
    	return !getColumnDescriptor(fieldName).isNullable();
    }
    public boolean isFieldEditable(String fieldName){
        return isFieldVisible(fieldName) && isFieldSettable(fieldName) && !isFieldProtected(fieldName) ;
    }
    
    public boolean isFieldSettable(String fieldName){
    	loadFieldSetters();
    	return fieldSetterMap.containsKey(fieldName);
    }
    
    public boolean isFieldGettable(String fieldName){
    	loadFieldGetters();
    	return fieldGetterMap.containsKey(fieldName);
    }

    public boolean isFieldVisible(String fieldName) {
        return !isFieldHidden(fieldName);
    }
    
    public boolean isFieldHidden(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	HIDDEN hidden = getAnnotation(getter,HIDDEN.class);
    	boolean isHidden = (hidden == null ? false : hidden.value());
    	if (!isHidden){
    		boolean hideHouseKeepingFields = (Boolean) Database.getJdbcTypeHelper(getPool()).getTypeRef(Boolean.class).getTypeConverter().valueOf(Config.instance().getProperty("swf.hide.housekeeping.fields","N"));
			isHidden = hideHouseKeepingFields && isHouseKeepingField(fieldName); 
    	}
    	return isHidden;
	}
    
    public String getFieldName(final String columnOrFieldName){
		Timer timer = cat.startTimer();
		try {
	    	loadAllFields();
	    	//Mostly column name and fieldnames are same.
	    	if (!fieldColumn.containsKey(columnOrFieldName)){
		    	List<String> fields = columnFields.get(columnOrFieldName);
		    	int numFields = fields == null ? 0 : fields.size();
		    	if (numFields == 1){
		    		return fields.get(0); 
		    	}else if (numFields == 0){
		    		cat.finest("Field not found for Column " + columnOrFieldName + " in " + getTableName() + ", Model Is " + getModelClass().getName());
		    		return null;
		    	}
	    	}
			return columnOrFieldName;
    	}finally{
    		timer.stop();
    	}
    }
    private SWFLogger cat = null;
    
    public boolean isHouseKeepingField(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return isAnnotationPresent(getter,HOUSEKEEPING.class);
    }
    
    public boolean isFieldPassword(String fieldName){
        Method getter = getFieldGetter(fieldName);
        return  isAnnotationPresent(getter,PASSWORD.class);
    }

    public boolean isFieldProtected(String fieldName){
    	return (getFieldProtection(fieldName) != Kind.EDITABLE);
    }

    public boolean isFieldDisabled(String fieldName){
    	return (getFieldProtection(fieldName) == Kind.DISABLED || !isFieldSettable(fieldName));
    }
    
    public Kind getFieldProtection(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	PROTECTION p = getAnnotation(getter,PROTECTION.class);
    	return p == null ? Kind.EDITABLE : p.value(); 
    }
    
    public boolean isFieldVirtual(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	IS_VIRTUAL p = getAnnotation(getter,IS_VIRTUAL.class);
    	return (p == null ? false : p.value());
    }
    
    
    public boolean isFieldEnumeration(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return isAnnotationPresent(getter,Enumeration.class);
    }
    public boolean isVirtual(){
		IS_VIRTUAL isVirtual = getAnnotation(IS_VIRTUAL.class);
    	if (isVirtual != null && isVirtual.value()) {
    		return true;
    	}
    	return false;
    }
	public String getPool(){
		return reflector.getPool();
	}
	public JdbcTypeHelper getJdbcTypeHelper() { 
		return Database.getJdbcTypeHelper(getPool());
	}
	
	public boolean isVoid(Object o) {
		return getJdbcTypeHelper().isVoid(o);
	}
	
	TimeZone zone = null;
	public TimeZone getTimeZone(){
		if (zone == null) {
			synchronized (this) {
				if (zone == null){
					String zoneId = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+getPool()+".timezone"));
					if (ObjectUtil.isVoid(zoneId)){
						zone = TimeZone.getDefault();
					}else {
						zone = TimeZone.getTimeZone(zoneId);
						if (!StringUtil.equals(zone.getID(),zoneId)){
							throw new RuntimeException("Invalid timezone specified for pool " +getPool());
						}
					}
				}
			}
		}
		return zone;
	}
	public Timestamp getNow(){
        return StandardDefaulter.getNow(getTimeZone());
    }

    public int getMaxDataLength(String fieldName){
		ColumnDescriptor fieldDescriptor = getColumnDescriptor(fieldName);
		String fieldColumnName = fieldDescriptor.getName();
		int size = fieldDescriptor.getSize();
		Class<?> javaClass = getFieldGetter(fieldName).getReturnType();
		if (String.class.isAssignableFrom(javaClass) || Reader.class.isAssignableFrom(javaClass)){
	    	List<Count> counts  = new ArrayList<Count>();
	    	if (!isVirtual() && !fieldDescriptor.isVirtual()){
	    		counts = new Select("MAX(LENGTH("+ fieldColumnName + ")) AS COUNT").from(modelClass).execute(Count.class);
		    	if (!counts.isEmpty()){
		    		size = counts.get(0).getCount(); 
		    	}else {
		    		size = MAX_DATA_LENGTH_FOR_TEXT_BOX;
		    	}
	    	}else {
	    		size = fieldDescriptor.getSize();
	    		size = size == 0 ? MAX_DATA_LENGTH_FOR_TEXT_BOX + 1 :  size;
	    	}
		}
    	return size;
    }
    

    public boolean isFieldDisplayLongForTextBox(String fieldName){
    	if (isFieldValueALongText(fieldName)){
    		return true;
    	}else {
        	Method getter = getFieldGetter(fieldName);
        	Method referredModelGetter = getReferredModelGetterFor(getter);
        	if (referredModelGetter != null){
        		Class<? extends Model> referredModelClass = getReferredModelClass(referredModelGetter);
        		ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
        		if (referredModelReflector != this || !ObjectUtil.equals(fieldName,referredModelReflector.getDescriptionField())){
        			return referredModelReflector.isFieldValueALongText(referredModelReflector.getDescriptionField());
        		}
        	}
        	return false;
    	}
    }
    
    public static final int MAX_DATA_LENGTH_FOR_TEXT_BOX = 80 ;
    public boolean isFieldValueALongText(String fieldName){
    	return isFieldValueALongText(fieldName,null);
    }
    public boolean isFieldValueALongText(String fieldName,Object fieldValue){
		Method getter = getFieldGetter(fieldName);
		Class<?> returnType = getter.getReturnType();
		if (Reader.class.isAssignableFrom(returnType) || 
    					(String.class.isAssignableFrom(returnType))){

	    	int len = getMaxDataLength(fieldName);
			if (!isFieldEditable(fieldName) && String.class.isAssignableFrom(returnType)){
				len = WordWrapUtil.getNumRowsRequired(StringUtil.valueOf(fieldValue),MAX_DATA_LENGTH_FOR_TEXT_BOX) * MAX_DATA_LENGTH_FOR_TEXT_BOX;
	    	}
			return (len > MAX_DATA_LENGTH_FOR_TEXT_BOX) ;	
		}
		return false;
    	
    }
    
    
    
    private Cache<Method,String> fieldNameCache = new Cache<Method, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4626497380273214264L;

		@Override
		protected String getValue(Method method) {
			String fieldName = null;
	        if (getFieldGetterMatcher().matches(method) ){
	            for (String getterPrefix:getterPrefixes){
	                if (method.getName().startsWith(getterPrefix)){
	                    fieldName = StringUtil.underscorize(method.getName().substring(getterPrefix.length()));
	                    break;
	                }
	            }
	        }else if(getFieldSetterMatcher().matches(method)){
	            fieldName = StringUtil.underscorize(method.getName().substring(3));
	        }
	        return fieldName;
		}
    
    };
    
    public String getFieldName(Method method){
		Timer timer = cat.startTimer();
		try {
			return fieldNameCache.get(method);
		}finally{
			timer.stop();
		}
    }

    public String getContentType(Model record, String fieldName){
    	String mimeType = null ;
    	
    	List<String> fields = getFields();
    	if (fields.contains(fieldName)){
        	Method getter = getFieldGetter(fieldName);
        	if (InputStream.class.isAssignableFrom(getter.getReturnType())){
    			CONTENT_TYPE ct = getAnnotation(getter,CONTENT_TYPE.class);
    			if (ct  != null){
    				mimeType = ct.value().toString();
    			}else {
    				String contentTypeFieldName = fieldName + "_CONTENT_TYPE";
    				if (getFields().contains(contentTypeFieldName)){
    					mimeType = get(record,contentTypeFieldName);
    				}
    			}
    		}
    	}
    	if (mimeType == null){
			mimeType = getDefaultContentType();
		}
    	return mimeType; 
    }
    public String getDefaultContentType(){
    	return MimeType.APPLICATION_OCTET_STREAM.toString();
    }
    
    public String getContentName(Model record, String fieldName){
    	List<String> fields = getFields();
    	if (fields.contains(fieldName)){
        	Method getter = getFieldGetter(fieldName);
        	if (InputStream.class.isAssignableFrom(getter.getReturnType())){
	    		String fileName = fieldName + "_CONTENT_NAME";
	    		if (fields.contains(fileName)){
	    			return get(record,fileName);
	    		}
        	}
    	}
    	return null;
    }

    public int getContentSize(Model record, String fieldName){
    	List<String> fields = getFields();
    	if (fields.contains(fieldName)){
        	Method getter = getFieldGetter(fieldName);
        	if (InputStream.class.isAssignableFrom(getter.getReturnType())){
	    		String sizeFieldName = fieldName + "_CONTENT_SIZE";
	    		if (fields.contains(sizeFieldName)){
            Integer i = get(record,sizeFieldName);
            return i;
	    		}else {
	    			InputStream is = get(record,fieldName);
	    			try {
	    				if (is != null){
	    					return is.available();
	    				}else {
	    					return 0;
	    				}
					} catch (IOException e) {
						//
					}
	    		}
        	}
    	}
    	return -1;
    }
    private static final String[] getterPrefixes = new String[]{"get" , "is"};
    
    private Map<String,Method> fieldGetterMap = null ;
    public void loadFieldGetters(){
    	if (fieldGetterMap == null){
    		synchronized (this) {
    			if (fieldGetterMap == null) {
        			IgnoreCaseMap<Method> fieldGetterMap  = new IgnoreCaseMap<Method>();
            		List<Method> fieldGetters = getFieldGetters();
            		for (Method fieldGetter: fieldGetters){
            			fieldGetterMap.put(getFieldName(fieldGetter), fieldGetter);
            		}
            		this.fieldGetterMap = fieldGetterMap;
    			}
			}
    	}
    }
    public Method getFieldGetter(String fieldName){
		Timer timer = cat.startTimer();
		try {
			loadFieldGetters();
			Method getter = fieldGetterMap.get(fieldName);
	    	if (getter == null){
	    		String getterName = "get/is" + StringUtil.camelize(fieldName);
	    		throw new FieldGetterMissingException("Method " + getterName + "() with appropriate return type is missing");
	    	}
	    	return getter;
		}finally{
			timer.stop();
		}
    }
    
    private Map<String,Method> fieldSetterMap = null;
    private void loadFieldSetters(){
    	if (fieldSetterMap == null){
    		synchronized (this) {
    			if (fieldSetterMap == null){
    				IgnoreCaseMap<Method> fieldSetterMap = new IgnoreCaseMap<Method>();
    	    		List<Method> fieldSetters = getFieldSetters();
    	    		for (Method fieldSetter: fieldSetters){
    	    			fieldSetterMap.put(getFieldName(fieldSetter), fieldSetter);
    	    		}
    	    		this.fieldSetterMap = fieldSetterMap;
    			}
			}
    	}
    }
    public Method getFieldSetter(String fieldName){
		Timer timer = cat.startTimer();
		try {
			loadFieldSetters();
	    	Method setter = fieldSetterMap.get(fieldName);
	    	if (setter == null){
	            Method getter = getFieldGetter(fieldName);
	        	String setterName = "set"+StringUtil.camelize(fieldName) +"(" + getter.getReturnType().getName() + ")";
	    		throw new FieldSetterMissingException("Method: public void " + setterName + " missing!");
	    	}
	    	return setter;
		}finally{
			timer.stop();
		}
    }
    
    public static class FieldSetterMissingException extends RuntimeException {
		private static final long serialVersionUID = 5976842300991239658L;
		public FieldSetterMissingException(String message){
			super(message);
		}
    }
    public static class FieldGetterMissingException extends RuntimeException {
		private static final long serialVersionUID = 5976842300991239658L;
		public FieldGetterMissingException(String message){
			super(message);
		}
    }

    public String getSignature(Method method){
    	return reflector.getSignature(method);
    }
    
    private Map<String,Map<Class<? extends Annotation>, Annotation>> annotationMap = new HashMap<String, Map<Class<? extends Annotation>,Annotation>>(); 
    private Map<Class<? extends Annotation>, Annotation> getAnnotationMap(Method getter){
		Timer timer = cat.startTimer();
		try {
	    	String signature = getSignature(getter);
	    	Map<Class<? extends Annotation>, Annotation> map = annotationMap.get(signature);
	    	if (map != null){
	    		return map;
	    	}
	    	synchronized (this) {
	    		map = annotationMap.get(signature);
	    		if (map == null) {
	    			map = new HashMap<Class<? extends Annotation>, Annotation>();
	    	        for (Class<? extends Model> modelClass:reflector.getSiblingModelClasses(getModelClass())){
	    	        	MReflector<? extends Model> ref = MReflector.instance(modelClass);
	    	        	//We could have simple called getAnnotation(getter,Annotation class) but that would mean looping multiple times for 
	    	        	//each annotation needed. hence this optimization.
	    	        	if (map.get(COLUMN_NAME.class) == null){ map.put(COLUMN_NAME.class,ref.getAnnotation(getter,COLUMN_NAME.class)); }
	    	        	if (map.get(COLUMN_SIZE.class) == null){ map.put(COLUMN_SIZE.class,ref.getAnnotation(getter,COLUMN_SIZE.class)); }
	    	        	if (map.get(DATA_TYPE.class  ) == null){ map.put(DATA_TYPE.class  ,ref.getAnnotation(getter,  DATA_TYPE.class)); }
	    	        	if (map.get(DECIMAL_DIGITS.class) == null){ map.put(DECIMAL_DIGITS.class,ref.getAnnotation(getter,DECIMAL_DIGITS.class)); }
	    	        	if (map.get(IS_NULLABLE.class) == null){ map.put(IS_NULLABLE.class,ref.getAnnotation(getter,IS_NULLABLE.class)); }
	    	        	if (map.get(IS_AUTOINCREMENT.class) == null){ map.put(IS_AUTOINCREMENT.class,ref.getAnnotation(getter,IS_AUTOINCREMENT.class)); }
	    	        	if (map.get(IS_VIRTUAL.class) == null){ map.put(IS_VIRTUAL.class,ref.getAnnotation(getter,IS_VIRTUAL.class)); }
	    	        	if (map.get(COLUMN_DEF.class) == null){ map.put(COLUMN_DEF.class,ref.getAnnotation(getter,COLUMN_DEF.class)); }
	    	        }
	    			annotationMap.put(signature, map);
	    		}
			}
	        return map;
		}finally{
			timer.stop();
		}
    }
    
    public ColumnDescriptor getColumnDescriptor(String fieldName){
    	Timer timer =cat.startTimer(null, Config.instance().isTimerAdditive());
    	try {
    		return getColumnDescriptors().get(fieldName);
    	}finally{
    		timer.stop();
    	}
    }
    /*
    public ColumnDescriptor getColumnDescriptor(Method fieldGetter){
    	Timer timer =cat.startTimer(null, Config.instance().isTimerAdditive());
    	try {
    		return getColumnDescriptor(getFieldName(fieldGetter));
    	}finally{
    		timer.stop();
    	}
	}*/
    
    public boolean hasMultipleAccess(String columnName){
		Timer timer = cat.startTimer();
		try {
	    	List<String> fields = columnFields.get(columnName);
	    	return (fields != null && fields.size() > 1);
		}finally{
			timer.stop();
		}
    }

    private class ColumnDescriptorCache extends Cache<String,ColumnDescriptor>{
    	
		private static final long serialVersionUID = 302310307824397179L;
		
    	public ColumnDescriptorCache(){
    		super(Cache.MAX_ENTRIES_UNLIMITED,0);
    		loadAllFields();
    	}
    	
    	@Override
		protected ColumnDescriptor getValue(String fieldName) {
    		Timer timer =cat.startTimer(null, Config.instance().isTimerAdditive());
    		try {
    			Method fieldGetter = getFieldGetter(fieldName);
		        if (!getFieldGetters().contains(fieldGetter)){
		            throw new RuntimeException("Method:" + fieldGetter.getName() + " is not recognizable as a a FieldGetter");
		        }
	
		        //String fieldName = getFieldName(fieldGetter);
		        String columnName = fieldColumn.get(fieldName);
	
		        if (hasMultipleAccess(columnName)){
		        	if (!columnFields.get(columnName).contains(columnName)){
		        		throw new RuntimeException(columnName + " has multiple access while none of the field has the same name!(" + columnFields.get(columnName).toString() + ")");
		        	}else if (!columnName.equalsIgnoreCase(fieldName)){
		        		return get(columnName);
		        	}
		        }
		        
		        Map<Class<? extends Annotation>, Annotation> map = getAnnotationMap(fieldGetter);
		        COLUMN_SIZE size = (COLUMN_SIZE) map.get(COLUMN_SIZE.class);
		        DATA_TYPE type 	 = (DATA_TYPE) map.get(DATA_TYPE.class);
		        DECIMAL_DIGITS digits = (DECIMAL_DIGITS) map.get(DECIMAL_DIGITS.class);
		        IS_NULLABLE isNullable = (IS_NULLABLE)map.get(IS_NULLABLE.class);
		        IS_AUTOINCREMENT isAutoIncrement = (IS_AUTOINCREMENT)map.get(IS_AUTOINCREMENT.class);
		        IS_VIRTUAL isVirtual = (IS_VIRTUAL)map.get(IS_VIRTUAL.class);
		        COLUMN_DEF colDef = (COLUMN_DEF)map.get(COLUMN_DEF.class);
		        
		        ColumnDescriptor cd = new ColumnDescriptor(getPool());
		        cd.setName(columnName);
		        JdbcTypeHelper helper = Database.getJdbcTypeHelper(getPool());
				TypeRef<?> typeRef = helper.getTypeRef(fieldGetter.getReturnType());
		        assert typeRef != null;
		        cd.setJDBCType(type == null ? typeRef.getJdbcType() : type.value());
		        cd.setNullable(isNullable != null ? isNullable.value() : !fieldGetter.getReturnType().isPrimitive());
		        cd.setSize(size == null? typeRef.getSize() : size.value());
		        cd.setScale(digits == null ? typeRef.getScale() : digits.value());
		        cd.setAutoIncrement(isAutoIncrement == null? false : true);
		        cd.setVirtual(isVirtual == null ? false : isVirtual.value());
		        if (colDef != null && colDef.value() != StandardDefault.NONE){
		    		cd.setColumnDefault(toDefaultKW(typeRef,colDef));
		        }
		        return cd;
			}finally {
				timer.stop();
			}
		}
    	
    }
    
    private ColumnDescriptorCache columnDescriptors = null; 
    private ColumnDescriptorCache getColumnDescriptors(){
    	if (columnDescriptors == null){
    		synchronized (this) {
				if (columnDescriptors == null) {
		    		columnDescriptors = new ColumnDescriptorCache();
				}
			}
    	}
    	return columnDescriptors;
    }

    private String toDefaultKW(TypeRef<?> ref, COLUMN_DEF def){
    	return Database.getJdbcTypeHelper(getPool()).toDefaultKW(ref,def);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass){
		return getAnnotation(annotationClass) != null;
     }

     public boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass ){
		return getAnnotation(method,  annotationClass) != null;
     }
     

     
     private Cache<Class<? extends Annotation>,Annotation> classAnnotationCache = new Cache<Class<? extends Annotation>, Annotation>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = -4698644911072168124L;

		@Override
		protected Annotation getValue(Class<? extends Annotation> annotationClass) {
			Annotation a = reflector.getAnnotation(getModelClass(), annotationClass);
			return a;
		}
    	 
     };
     @SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass){
		return (A)classAnnotationCache.get(annotationClass);
     }
     
     private Cache<Method,Cache<Class<? extends Annotation>,Annotation>> methodAnnotationCache = new Cache<Method, Cache<Class<? extends Annotation>,Annotation>>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 4256698883995018084L;

		@Override
		protected Cache<Class<? extends Annotation>, Annotation> getValue(final Method k) {
			Timer timer = cat.startTimer();
			try {
				return new Cache<Class<? extends Annotation>, Annotation>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 4851400562811398820L;

					@Override
					protected Annotation getValue(
							Class<? extends Annotation> annotationClass) {
						Timer timer = cat.startTimer();
						try {
							return reflector.getAnnotation(getModelClass(),k,
									annotationClass);
						} finally {
							timer.stop();
						}
					}
				};
			} finally {
				timer.stop();
			}
		}
    	 
     };
     @SuppressWarnings("unchecked")
     public <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass){
		return (A)methodAnnotationCache.get(method).get(annotationClass);
     }
     
     public Class<? extends Model> getChildModelClass(Method method){
 		Timer timer = cat.startTimer();
 		try {
	         Class<?> possibleChildClass = null;
	         if (!getClassForests().contains(method.getDeclaringClass())){
	         	return null;
	         }
	         if (getGetterMatcher().matches(method)){
	             Class<?> retType = method.getReturnType();
	             if (List.class.isAssignableFrom(retType)){
	                 ParameterizedType parameterizedType = (ParameterizedType)method.getGenericReturnType();
	                 possibleChildClass = (Class<?>)parameterizedType.getActualTypeArguments()[0];
	             }
	             if (possibleChildClass != null && Model.class.isAssignableFrom(possibleChildClass)){
	                 // Validate That child has a parentReferenceId. 
	                 @SuppressWarnings("unchecked")
	                 Class<? extends Model> childClass = (Class<? extends Model>)possibleChildClass;
	                 ModelReflector<? extends Model> childReflector = ModelReflector.instance(childClass);
	                 if (!childReflector.getReferenceFields(getModelClass()).isEmpty()){
	                	return childClass; 
	                 }
	             }
	         }
	         return null;
 		}finally{
 			timer.stop();
 		}
     }
     
     public List<String> getReferenceFields(Class<? extends Model> referredModelClass){
 		Timer timer = cat.startTimer();
 		try {
 			List<String> names = new ArrayList<String>();
 			for (Method referredModelGetter : getReferredModelGetters(referredModelClass)){
 				names.add(getReferenceField(referredModelGetter));
	    	}
	    	return names;
 		}finally{
 			timer.stop();
 		}
     }
     
     @SuppressWarnings("unchecked")
     public List<Method> getReferredModelGetters(final Class<? extends Model> referredModelClass){
 		Timer timer = cat.startTimer();
 		try {
	    	 ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
	    	 List<Method> referredModelGetters = getReferredModelGetters();
	    	 List<Method> ret = new ArrayList<Method>();
	    	 for (Method aReferredModelGetter: referredModelGetters){
	    		 if (referredModelReflector.reflects((Class <? extends Model>)aReferredModelGetter.getReturnType())){
	    			 ret.add(aReferredModelGetter);
	    		 }
	    	 }
	    	 return ret;
 		}finally{
 			timer.stop();
 		}
     }

	@SuppressWarnings("unchecked")
	public Class<? extends Model> getReferredModelClass(Method referredModelGetter) {
		Timer timer = cat.startTimer();
		try {
			if (!getClassForests().contains(referredModelGetter.getDeclaringClass())) {
				return null;
			}
			Class<? extends Model> referredModelClass = null;
			Class<?> possibleReferredModelClass = referredModelGetter.getReturnType();
			if (Model.class.isAssignableFrom(possibleReferredModelClass)
					&& getGetterMatcher().matches(referredModelGetter)) {
				String referredIdFieldName = getReferenceField(referredModelGetter);
				if (getFields().contains(referredIdFieldName)) {
					referredModelClass = (Class<? extends Model>) possibleReferredModelClass;
				}
			}
			return referredModelClass;
		} finally {
			timer.stop();
		}
	}

	Map<Method,String> referredModelGetterToReferenceFieldMap = new HashMap<Method, String>();
	public String getReferenceField(Method parentGetter){
		String field = referredModelGetterToReferenceFieldMap.get(parentGetter);
		if (field == null && !referredModelGetterToReferenceFieldMap.containsKey(parentGetter)){
			field = StringUtil.underscorize(parentGetter.getName().substring(3) + "Id");  
			referredModelGetterToReferenceFieldMap.put(parentGetter, field);
		}
		return field;
    }



	private Map<Method,Method> referredModelIdGetterToReferredModelGetterMap = new HashMap<Method, Method>();
	public Method getReferredModelGetterFor(Method referredModelIdGetter) {
		if (!getFieldGetters().contains(referredModelIdGetter)) {
			return null;
		}

		Method ret = referredModelIdGetterToReferredModelGetterMap.get(referredModelIdGetter);
		if (ret == null && !referredModelIdGetterToReferredModelGetterMap.containsKey(referredModelIdGetter)) {
			String methodName = referredModelIdGetter.getName();
			if (methodName.startsWith("get")
					&& methodName.endsWith("Id")
					&& methodName.length() > 5 // Not = getId
					&& (Arrays.asList(int.class,Integer.class,long.class,Long.class).contains(referredModelIdGetter.getReturnType()))) {
				String referredModelMethodName = methodName.substring(0,
						methodName.length() - "Id".length());
				for (Class<? extends Model> modelClass : reflector
						.getSiblingModelClasses(getModelClass())) {
					try {
						Method referredModelGetter = modelClass
								.getMethod(referredModelMethodName);
						if (Model.class.isAssignableFrom(referredModelGetter
								.getReturnType())) {
							ret = referredModelGetter;
							break;
						}
					} catch (NoSuchMethodException ex) {
						//
					}
				}
			}
			referredModelIdGetterToReferredModelGetterMap.put(referredModelIdGetter, ret);
		}
		return ret;
	}

	private SequenceSet<Class<? extends Model>> classHierarchies = null;
	public SequenceSet<Class<? extends Model>> getClassHierarchies() {
		if (classHierarchies == null){
			synchronized (this) {
				if (classHierarchies == null) {
					classHierarchies = reflector.getClassHierarchies(getModelClass());
				}
			}
		}
		return classHierarchies;
	}
	private SequenceSet<Class<?>> classForests = null;
	public SequenceSet<Class<?>> getClassForests(){
		if (classForests == null){
			synchronized (this) {
				if (classForests == null){
					classForests = reflector.getClassForests(getModelClass());
				}
			}
		}
		return classForests;
	}


    //Field MAtchers
    public static interface FieldMatcher {
        public boolean matches(ColumnDescriptor cd);
    }

    private class RealFieldMatcher implements FieldMatcher {
        public boolean matches(ColumnDescriptor cd) {
            return !cd.isVirtual();
        }
    }
    private class VirtualFieldMatcher implements FieldMatcher {
        public boolean matches(ColumnDescriptor cd) {
            return cd.isVirtual();
        }
    }
    
    //Method Matcher
    private final MethodMatcher getterMatcher = new GetterMatcher();
    public  MethodMatcher getGetterMatcher(){
        return getterMatcher;
    }

    public class GetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
			Timer timer = cat.startTimer();
			try {
	            String mName = method.getName();
	            Class<?> retType = method.getReturnType();
	            Class<?>[] paramTypes = method.getParameterTypes();
	            if (  ((mName.startsWith("get") && retType != Void.TYPE) || 
	                    mName.startsWith("is") && (boolean.class == retType || Boolean.class == retType) ) &&
	                    (paramTypes == null || paramTypes.length == 0)){
	                 return true;
	            }
	            return false;
            } finally {
				timer.stop();
			}
        }
    }
    private final MethodMatcher fieldGetterMatcher = new FieldGetterMatcher();
    public MethodMatcher getFieldGetterMatcher() {
        return fieldGetterMatcher;
    }
    
    private final MethodMatcher indexedFieldGetterMatcher = new IndexedFieldGetterMatcher();
    public MethodMatcher getIndexedFieldGetterMatcher() {
        return indexedFieldGetterMatcher;
    }
    
    public class IndexedFieldGetterMatcher extends FieldGetterMatcher {
		@Override
        public boolean matches(Method method){
			return super.matches(method) && isAnnotationPresent(method,Index.class) ;
		}
	}

    public class FieldGetterMatcher extends GetterMatcher{
        @Override
        public boolean matches(Method method){
    		Timer timer = cat.startTimer();
        	try {
    			if (super.matches(method) && 
                        !Model.class.isAssignableFrom(method.getReturnType()) &&
                        Database.getJdbcTypeHelper(getPool()).getTypeRef(method.getReturnType()) != null){
                     return true;
                }
                return false;
        	}finally{
        		timer.stop();
        	}
        }
    }

    public class SetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
        	Timer timer = cat.startTimer();
        	try {
	            String mName = method.getName();
	            Class<?> retType = method.getReturnType();
	            Class<?>[] paramTypes = method.getParameterTypes();
	            if (mName.startsWith("set") && (Void.TYPE == retType) && 
	                    (paramTypes != null && paramTypes.length == 1) ){
	                 return true;
	            }
	            return false;
        	}finally{
        		timer.stop();
        	}
        }
    }

    private final MethodMatcher fieldSetterMatcher = new FieldSetterMatcher();
    public MethodMatcher getFieldSetterMatcher() {
        return fieldSetterMatcher;
    }

    public class FieldSetterMatcher extends SetterMatcher{
        @Override
        public boolean matches(Method method){
        	Timer timer = cat.startTimer();
			try {
				if (super.matches(method)
						&& Database.getJdbcTypeHelper(getPool()).getTypeRef(
								method.getParameterTypes()[0]) != null) {
					return true;
				}
				return false;
			} finally {
				timer.stop();
			}
        }
    }
    
    //RelationShip Methods matchers
    private final MethodMatcher referredModelGetterMatcher=  new ReferredModelGetterMatcher();
    public MethodMatcher getReferredModelGetterMatcher(){ 
        return referredModelGetterMatcher;
    }
    
    private class ReferredModelGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            Timer timer = cat.startTimer();
			try {
				return getReferredModelClass(method) != null;
			} finally {
				timer.stop();
			}
        }
    }

    private final MethodMatcher participantModelGetterMatcher = new ParticipantModelGetterMatcher();
    public MethodMatcher getParticipantModelGetterMatcher(){ 
        return participantModelGetterMatcher;
    }
    
    private class ParticipantModelGetterMatcher extends ReferredModelGetterMatcher{
        public boolean matches(Method method){
            Timer timer = cat.startTimer();
			try {
				if (super.matches(method)){
					return isAnnotationPresent(getFieldGetter(getReferenceField(method)), PARTICIPANT.class);
				}
				return false;
			} finally {
				timer.stop();
			}
        }
    }
    private final MethodMatcher childrenGetterMatcher=  new ChildrenGetterMatcher();
    public MethodMatcher getChildrenGetterMatcher(){ 
        return childrenGetterMatcher;
    }

    private class ChildrenGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return (getChildModelClass(method) != null);
        }
    }

    public String getOrderBy(){
        ORDER_BY order = getAnnotation(ORDER_BY.class);
        String orderBy = ORDER_BY.DEFAULT;
        if (order != null){
        	orderBy = order.value();
        }
        return orderBy;
    }
    
    private Cache<String,String> participatingRole = new Cache<String, String>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 163426440760440104L;

		@Override
		protected String getValue(String referencedModelIdFieldName) {
			return referencedModelIdFieldName.substring(0, referencedModelIdFieldName.length()-3) ; //Remove "_ID" from the end.
		}
	};
	public String getParticipatingRole(String referencedModelIdFieldName){
		return participatingRole.get(referencedModelIdFieldName);
	}
	
	private Set<String> participatableRoles = null;
	public Set<String> getParticipatableRoles(){
		if (participatableRoles == null){
			synchronized (this) {
				if (participatableRoles == null){
					HashSet<String> participatableRoles = new HashSet<String>();
					for (Method m : getParticipantModelGetters()){
						participatableRoles.add(getParticipatingRole(getReferenceField(m)));
					}
					this.participatableRoles = participatableRoles;
				}
			}
		}
		return new HashSet<String>(participatableRoles);
	}
	
	public static void dispose() {
		ModelReflector.modelReflectorByModelClass.clear();
	}

	private Set<String> autoIncrementColumns = null; 
    public Set<String> getAutoIncrementColumns() {
    	if (autoIncrementColumns == null){
    		synchronized (this) {
    			if (autoIncrementColumns == null){
    	        	autoIncrementColumns = new IgnoreCaseSet();
    	    	    for (String f : getFields()){
    	    	    	ColumnDescriptor d = getColumnDescriptor(f);
    	                if (d.isAutoIncrement()){ 
    	                    autoIncrementColumns.add(d.getName());
    	                }
    	            }
    			}
			}
    	}
        return Collections.unmodifiableSet(autoIncrementColumns);
	}    
}
