package com.venky.swf.db.model.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.SequenceSet;
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
import com.venky.swf.db.annotations.column.IS_AUTOINCREMENT;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.TableReflector.MReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;

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
					TableReflector tr = TableReflector.instance(modelClass);
					if (tr != null){
						ref = new ModelReflector<M>(modelClass, tr );
					}
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
	 * Find extension points of the form prefix.<modelClass.getSimpleName()>.suffix for all relevant models in the right sequence.
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	public List<String> getExtensionPoints(String prefix,String suffix){
		return extensionPointsCache.get(prefix+"@"+suffix);
	}
        
	private final Class<M> modelClass  ;  
	private final TableReflector reflector ;
	private ModelReflector(Class<M> modelClass,TableReflector reflector){
		this.modelClass = modelClass;
		this.reflector = reflector;
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
	public <T> T get(Model record, String fieldName){
    	Timer timer = Timer.startTimer();
        try {
            Method getter = getFieldGetter(fieldName);
    		return (T)getter.invoke(record);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        } finally {
        	timer.stop();
        }

    	
    }
    public void set(Model record, String fieldName, Object value){
    	Timer timer = Timer.startTimer();
        try {
            Method getter = getFieldGetter(fieldName);
            Method setter = getFieldSetter(fieldName);
    		TypeRef<?> typeRef = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType());

        	if (!ObjectUtil.isVoid(value) || getter.getReturnType().isPrimitive()){
                setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
        	}else {
                setter.invoke(record, getter.getReturnType().cast(null));
        	}
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        } finally {
        	timer.stop();
        }

    }

	public void loadMethods(List<Method> into, MethodMatcher matcher) {
		Timer timer = Timer.startTimer();
		try {
			reflector.loadMethods(getModelClass(), into, matcher);
		}finally{
			timer.stop();
		}
	}

    private SequenceSet<Method> fieldGetters = new SequenceSet<Method>(); 
    public List<Method> getFieldGetters(){
    	loadMethods(fieldGetters, getFieldGetterMatcher());
    	return fieldGetters;
    }
    
    private SequenceSet<String> fieldGetterSignatures = new SequenceSet<String>();
    public List<String> getFieldGetterSignatures(){
    	if (fieldGetterSignatures.isEmpty()){
    		for (Method m : getFieldGetters()){
    			fieldGetterSignatures.add(getSignature(m));
    		}
    	}
    	return fieldGetterSignatures;
    }
    
    private SequenceSet<Method> indexedFieldGetters = new SequenceSet<Method>();
    public List<Method> getIndexedFieldGetters(){
    	loadMethods(indexedFieldGetters, getIndexedFieldGetterMatcher());
    	return indexedFieldGetters;
    }
    
    private SequenceSet<Method> fieldSetters = new SequenceSet<Method>() ;
    public List<Method> getFieldSetters(){
    	loadMethods(fieldSetters, getFieldSetterMatcher());
    	return fieldSetters;
    }
    
    private SequenceSet<Method> referredModelGetters = null;
    public List<Method> getReferredModelGetters(){ 
    	if (referredModelGetters == null ){
    		referredModelGetters = new SequenceSet<Method>();
        	loadMethods(referredModelGetters, getReferredModelGetterMatcher());
    	}
    	return referredModelGetters;
    }
    
    private SequenceSet<Method> participantModelGetters = null ;
    public List<Method> getParticipantModelGetters(){
    	if (participantModelGetters == null){
    		participantModelGetters = new SequenceSet<Method>();
        	loadMethods(participantModelGetters, getParticipantModelGetterMatcher());
    	}
    	return participantModelGetters;
    }
    
    private SequenceSet<Method> childModelGetters = null ;
    public List<Method> getChildGetters(){
    	if (childModelGetters == null){
    		childModelGetters = new SequenceSet<Method>();
        	loadMethods(childModelGetters,getChildrenGetterMatcher());
    	}
    	return childModelGetters;
    }

    private List<String> allfields = new IgnoreCaseList();
    private Map<String,List<String>> columnFields = new IgnoreCaseMap<List<String>>();
    private Map<String,String> fieldColumn = new IgnoreCaseMap<String>();

    private void loadAllFields(){
		Timer timer = Timer.startTimer();
		try {
	        if (!allfields.isEmpty()){
	            return;
	        }
	        synchronized (allfields) {
	            if (!allfields.isEmpty()){
	                return;
	            }
	            List<Method> fieldGetters = getFieldGetters();
	            for (Method fieldGetter : fieldGetters){
	        		String fieldName = getFieldName(fieldGetter);
	
	        		Map<Class<? extends Annotation>,Annotation> map = getAnnotationMap(fieldGetter);
	        		COLUMN_NAME name = (COLUMN_NAME)map.get(COLUMN_NAME.class);
	        		String columnName = ( name == null ? fieldName : name.value());
	        		
	                allfields.add(fieldName);
	        		List<String> fields = columnFields.get(columnName);
	        		if (fields == null){
	        			fields = new ArrayList<String>();
	        			columnFields.put(columnName, fields);
	        		}
	        		fields.add(fieldName);
	        		fieldColumn.put(fieldName, columnName);
	            }
	        }
		}finally {
			timer.stop();
		}
    }
    
    public List<String> getFields(){
		Timer timer = Timer.startTimer();
		try {
	    	loadAllFields();
	    	return new IgnoreCaseList(allfields);
		}finally{
			timer.stop();
		}
    }
    
    private IgnoreCaseMap<Class<? extends Model>> indexedColumns = null;
    public List<String> getIndexedColumns(){
    	if (indexedColumns == null){
    		indexedColumns = new IgnoreCaseMap<Class<? extends Model>>();
        	for (Method indexedFieldGetter : getIndexedFieldGetters()){
        		String indexColumnName = getColumnDescriptor(indexedFieldGetter).getName();
        		indexedColumns.put(indexColumnName,null);
        		if (getReferredModelGetters().size() > 0){
					Method referredModelGetter = getReferredModelGetterFor(indexedFieldGetter) ; 
					if (referredModelGetter != null){
						Class<? extends Model> referredModelClass = getReferredModelClass(referredModelGetter);
						indexedColumns.put(indexColumnName, referredModelClass);
					}
				}
        	}
    	}
    	return new ArrayList<String>(indexedColumns.keySet());
    }
    
    private Cache<String,SequenceSet<String>> uniqueKeys = null; 
    public Cache<String,SequenceSet<String>> getUniqueKeys(){
    	if (uniqueKeys == null){
        	uniqueKeys = new Cache<String, SequenceSet<String>>(0,0) {
				private static final long serialVersionUID = 1L;

				@Override
				protected SequenceSet<String> getValue(String k) {
					return new SequenceSet<String>();
				} 
        		
        	};
    		for (Method fieldGetter : getFieldGetters()){
    			UNIQUE_KEY key = this.getAnnotation(fieldGetter, UNIQUE_KEY.class);
    			if (key != null){
        			String fieldName = getFieldName(fieldGetter);
        			StringTokenizer keys = new StringTokenizer(key.value(),",");
        			while (keys.hasMoreTokens()){
            			uniqueKeys.get(keys.nextToken()).add(fieldName);
        			}
    			}
    		}
    	}
    	return uniqueKeys;
    }
    private SequenceSet<SequenceSet<String>> singleColumnUniqueKeys = null;
    public Collection<SequenceSet<String>> getSingleColumnUniqueKeys(){
    	if (singleColumnUniqueKeys == null){
    		singleColumnUniqueKeys = new SequenceSet<SequenceSet<String>>();
    		for (SequenceSet<String> uk : getUniqueKeys().values()){
    			if (uk.size() == 1){
    				singleColumnUniqueKeys.add(uk);
    			}
    		}
    	}
    	return singleColumnUniqueKeys;
    }
    
    
    public List<String> getRealFields(){
        return getFields(new RealFieldMatcher());
    }
    public List<String> getVirtualFields(){
        return getFields(new VirtualFieldMatcher());
    }
    public List<String> getFields(FieldMatcher matcher){
		loadAllFields();
        List<String> fields = new IgnoreCaseList();
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
		Timer timer = Timer.startTimer();
		try {
	    	List<String> fields = getFields(matcher);
	    	List<String> columns = new IgnoreCaseList();
	    	for (String field:fields){
	    		columns.add(getColumnDescriptor(field).getName());
	    	}
	    	return columns;
		}finally{
			timer.stop();
		}
    }

    public boolean isFieldMandatory(String fieldName){
    	Method fieldGetter = getFieldGetter(fieldName);
    	return !getColumnDescriptor(fieldGetter).isNullable();
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
    	return (hidden == null ? false : hidden.value());
	}
    
    public String getFieldName(final String columnName){
		Timer timer = Timer.startTimer();
		try {
	    	loadAllFields();
	    	List<String> fields = columnFields.get(columnName);
	    	
	    	if (!fields.isEmpty()){
	    		if (fields.contains(columnName)){
	    			return columnName;
	    		}
	    		return fields.get(0);
	    	}
	    	return null;
    	}finally{
    		timer.stop();
    	}
    }
    
    public boolean isHouseKeepingField(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return isAnnotationPresent(getter,HOUSEKEEPING.class);
    }
    
    public boolean isFieldPassword(String fieldName){
        Method getter = getFieldGetter(fieldName);
        return  isAnnotationPresent(getter,PASSWORD.class);
    }

    public boolean isFieldProtected(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	PROTECTION p = getAnnotation(getter,PROTECTION.class);
    	return (p == null ? false : p.value() != Kind.EDITABLE);
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
		Timer timer = Timer.startTimer();
		try {
			return fieldNameCache.get(method);
		}finally{
			timer.stop();
		}
    }


    private static final String[] getterPrefixes = new String[]{"get" , "is"};
    
    private Map<String,Method> fieldGetterMap = new IgnoreCaseMap<Method>();
    public void loadFieldGetters(){
    	if (fieldGetterMap.isEmpty()){
    		List<Method> fieldGetters = getFieldGetters();
    		for (Method fieldGetter: fieldGetters){
    			fieldGetterMap.put(getFieldName(fieldGetter), fieldGetter);
    		}
    	}
    }
    public Method getFieldGetter(String fieldName){
		Timer timer = Timer.startTimer();
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
    
    private Map<String,Method> fieldSetterMap = new IgnoreCaseMap<Method>();
    private void loadFieldSetters(){
    	if (fieldSetterMap.isEmpty()){
    		List<Method> fieldSetters = getFieldSetters();
    		for (Method fieldSetter: fieldSetters){
    			fieldSetterMap.put(getFieldName(fieldSetter), fieldSetter);
    		}
    	}
    }
    public Method getFieldSetter(String fieldName){
		Timer timer = Timer.startTimer();
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
		Timer timer = Timer.startTimer();
		try {
	    	String signature = getSignature(getter);
	    	Map<Class<? extends Annotation>, Annotation> map = annotationMap.get(signature);
	    	if (map != null){
	    		return map;
	    	}
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
	        return map;
		}finally{
			timer.stop();
		}
    }
    
    public ColumnDescriptor getColumnDescriptor(String fieldName){
        return getColumnDescriptor(getFieldGetter(fieldName));
    }

    public boolean hasMultipleAccess(String columnName){
		Timer timer = Timer.startTimer();
		try {
	    	List<String> fields = columnFields.get(columnName);
	    	return (fields != null && fields.size() > 1);
		}finally{
			timer.stop();
		}
    }

    private Map<String,ColumnDescriptor> columnDescriptors = new HashMap<String,ColumnDescriptor>();
    public ColumnDescriptor getColumnDescriptor(Method fieldGetter ){
    	loadAllFields();
        if (!getFieldGetters().contains(fieldGetter)){
            throw new RuntimeException("Method:" + fieldGetter.getName() + " is not recognizable as a a FieldGetter");
        }

        String fieldName = getFieldName(fieldGetter);
        String columnName = fieldColumn.get(fieldName);

        ColumnDescriptor cd = columnDescriptors.get(columnName);
        if (cd != null){
        	return cd;
        }else if (hasMultipleAccess(columnName)){
        	if (!columnFields.get(columnName).contains(columnName)){
        		throw new RuntimeException(columnName + " has multiple access while none of the field has the same name!");
        	}else if (!columnName.equalsIgnoreCase(fieldName)){
        		return getColumnDescriptor(columnName);
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
        
        cd = new ColumnDescriptor();
        cd.setName(columnName);
        JdbcTypeHelper helper = Database.getJdbcTypeHelper();
		TypeRef<?> typeRef = helper.getTypeRef(fieldGetter.getReturnType());
        assert typeRef != null;
        cd.setJDBCType(type == null ? typeRef.getJdbcType() : type.value());
        cd.setNullable(isNullable != null ? isNullable.value() : !fieldGetter.getReturnType().isPrimitive());
        cd.setSize(size == null? typeRef.getSize() : size.value());
        cd.setScale(digits == null ? typeRef.getScale() : digits.value());
        cd.setAutoIncrement(isAutoIncrement == null? false : true);
        cd.setVirtual(isVirtual == null ? false : isVirtual.value());
        if (colDef != null){
    		cd.setColumnDefault(toDefaultKW(typeRef,colDef));
        }
        columnDescriptors.put(columnName,cd);
        return cd;
    }
    
    private String toDefaultKW(TypeRef<?> ref, COLUMN_DEF def){
    	return Database.getJdbcTypeHelper().toDefaultKW(ref,def);
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
			Timer timer = Timer.startTimer();
			try {
				return new Cache<Class<? extends Annotation>, Annotation>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 4851400562811398820L;

					@Override
					protected Annotation getValue(
							Class<? extends Annotation> annotationClass) {
						Timer timer = Timer.startTimer();
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
 		Timer timer = Timer.startTimer();
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
 		Timer timer = Timer.startTimer();
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
 		Timer timer = Timer.startTimer();
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
	public Class<? extends Model> getReferredModelClass(Method method) {
		Timer timer = Timer.startTimer();
		try {
			if (!getClassForests().contains(method.getDeclaringClass())) {
				return null;
			}
			Class<? extends Model> referredModelClass = null;
			Class<?> possibleReferredModelClass = method.getReturnType();
			if (Model.class.isAssignableFrom(possibleReferredModelClass)
					&& getGetterMatcher().matches(method)) {
				String referredIdFieldName = getReferenceField(method);
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
					&& (referredModelIdGetter.getReturnType() == int.class || referredModelIdGetter
							.getReturnType() == Integer.class)) {
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
			classHierarchies = reflector.getClassHierarchies(getModelClass());
		}
		return classHierarchies;
	}
	private SequenceSet<Class<?>> classForests = null;
	public SequenceSet<Class<?>> getClassForests(){
		if (classForests == null){
			classForests = reflector.getClassForests(getModelClass());
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

    public static class GetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
			Timer timer = Timer.startTimer();
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

    public static class FieldGetterMatcher extends GetterMatcher{ 
        @Override
        public boolean matches(Method method){
    		Timer timer = Timer.startTimer();
        	try {
    			if (super.matches(method) && 
                        !Model.class.isAssignableFrom(method.getReturnType()) &&
                        Database.getJdbcTypeHelper().getTypeRef(method.getReturnType()) != null){
                     return true;
                }
                return false;
        	}finally{
        		timer.stop();
        	}
        }
    }

    public static class SetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
        	Timer timer = Timer.startTimer();
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

    public static class FieldSetterMatcher extends SetterMatcher{ 
        @Override
        public boolean matches(Method method){
        	Timer timer = Timer.startTimer();
			try {
				if (super.matches(method)
						&& Database.getJdbcTypeHelper().getTypeRef(
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
            Timer timer = Timer.startTimer();
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
            Timer timer = Timer.startTimer();
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

    
}
