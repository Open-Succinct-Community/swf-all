package com.venky.swf.db.model.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
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
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTED;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.TableReflector.MReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;

public class ModelReflector<M extends Model> {
    private static final Map<Class<? extends Model> , ModelReflector>  modelReflectorByModelClass = new HashMap<Class<? extends Model>, ModelReflector>();
    
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

    public String getDescriptionColumn(){
    	HAS_DESCRIPTION_COLUMN descColumn = getAnnotation(HAS_DESCRIPTION_COLUMN.class);
    	
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
    
    public void set(Model record, String fieldName, Object value){
    	Timer timer = Timer.startTimer();
        try {
            Method getter = getFieldGetter(fieldName);
            Method setter = getFieldSetter(fieldName);
    		TypeRef<?> typeRef = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType());

        	if (ObjectUtil.isVoid(value) && getColumnDescriptor(getter).isNullable()){
                setter.invoke(record, getter.getReturnType().cast(null));
    		}else {
                setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
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


    public boolean isFieldEditable(String fieldName){
        return isFieldVisible(fieldName) && !isFieldVirtual(fieldName) && !isFieldProtected(fieldName);
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
    	PROTECTED p = getAnnotation(getter,PROTECTED.class);
    	return (p == null ? false : p.value());
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
    public Method getFieldGetter(String fieldName){
		Timer timer = Timer.startTimer();
		try {
	    	if (fieldGetterMap.isEmpty()){
	    		List<Method> fieldGetters = getFieldGetters();
	    		for (Method fieldGetter: fieldGetters){
	    			fieldGetterMap.put(getFieldName(fieldGetter), fieldGetter);
	    		}
	    	}
	    	Method getter = fieldGetterMap.get(fieldName);
	    	if (getter == null){
	    		String getterName = "get/is" + StringUtil.camelize(fieldName);
	    		throw new RuntimeException("Method " + getterName + "() with appropriate return type is missing");
	    	}
	    	return getter;
		}finally{
			timer.stop();
		}
    }
    
    private Map<String,Method> fieldSetterMap = new IgnoreCaseMap<Method>();
    public Method getFieldSetter(String fieldName){
		Timer timer = Timer.startTimer();
		try {
	
	    	if (fieldSetterMap.isEmpty()){
	    		List<Method> fieldSetters = getFieldSetters();
	    		for (Method fieldSetter: fieldSetters){
	    			fieldSetterMap.put(getFieldName(fieldSetter), fieldSetter);
	    		}
	    	}
	    	Method setter = fieldSetterMap.get(fieldName);
	    	if (setter == null){
	            Method getter = getFieldGetter(fieldName);
	        	String setterName = "set"+StringUtil.camelize(fieldName) +"(" + getter.getReturnType().getName() + ")";
	    		throw new RuntimeException("Method: public void " + setterName + " missing!");
	    	}
	    	return setter;
		}finally{
			timer.stop();
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
		@Override
		protected Annotation getValue(Class<? extends Annotation> annotationClass) {
			Annotation a = reflector.getAnnotation(getModelClass(), annotationClass);
			return a;
		}
    	 
     };
     public <A extends Annotation> A getAnnotation(Class<A> annotationClass){
		return (A)classAnnotationCache.get(annotationClass);
     }
     
     private Cache<Method,Cache<Class<? extends Annotation>,Annotation>> methodAnnotationCache = new Cache<Method, Cache<Class<? extends Annotation>,Annotation>>(){
		@Override
		protected Cache<Class<? extends Annotation>, Annotation> getValue(final Method k) {
			Timer timer = Timer.startTimer();
			try {
				return new Cache<Class<? extends Annotation>, Annotation>() {
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
    private interface FieldMatcher {
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

    private class GetterMatcher implements MethodMatcher{
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
    
    private class FieldGetterMatcher extends GetterMatcher{ 
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

    private class SetterMatcher implements MethodMatcher{
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

    private class FieldSetterMatcher extends SetterMatcher{ 
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
