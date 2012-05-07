package com.venky.swf.db.model.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.reflection.Reflector;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
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
        Method getter = getFieldGetter(fieldName);
        Method setter = getFieldSetter(fieldName);

		TypeRef<?> typeRef = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType());

        try {
        	if (ObjectUtil.isVoid(value) && getColumnDescriptor(getter).isNullable()){
                setter.invoke(record, getter.getReturnType().cast(null));
    		}else {
                setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
        	}
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

    }

	public void loadMethods(List<Method> into, MethodMatcher matcher) {
		reflector.loadMethods(getModelClass(), into, matcher);
	}

    private List<Method> fieldGetters = new ArrayList<Method>(); 
    public List<Method> getFieldGetters(){
    	loadMethods(fieldGetters, getFieldGetterMatcher());
    	return fieldGetters;
    }
    
    private List<Method> fieldSetters = new ArrayList<Method>() ;
    public List<Method> getFieldSetters(){
    	loadMethods(fieldSetters, getFieldSetterMatcher());
    	return fieldSetters;
    }
    
    private List<Method> referredModelGetters = new ArrayList<Method>();
    public List<Method> getReferredModelGetters(){ 
    	loadMethods(referredModelGetters, getReferredModelGetterMatcher());
    	return referredModelGetters;
    }
    
    private List<Method> participantModelGetters = new ArrayList<Method>();
    public List<Method> getParticipantModelGetters(){
    	loadMethods(participantModelGetters, getParticipantModelGetterMatcher());
    	return participantModelGetters;
    }
    
    private List<Method> childModelGetters = new ArrayList<Method>();
    public List<Method> getChildGetters(){
    	loadMethods(childModelGetters,getChildrenGetterMatcher());
    	return childModelGetters;
    }

    private List<String> allfields = new IgnoreCaseList();
    private void loadAllFields(){
        if (!allfields.isEmpty()){
            return;
        }
        synchronized (allfields) {
            if (!allfields.isEmpty()){
                return;
            }
            List<Method> fieldGetters = getFieldGetters();
            for (Method fieldGetter : fieldGetters){
                allfields.add(getFieldName(fieldGetter));
            }
        }
    }
    public List<String> getFields(){
       loadAllFields();
       return new IgnoreCaseList(allfields);
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
    	loadAllFields();
    	return getColumns(new RealFieldMatcher());
    }
    
    public List<String> getColumns(FieldMatcher matcher){
    	List<String> fields = getFields(matcher);
    	List<String> columns = new IgnoreCaseList();
    	for (String field:fields){
    		columns.add(getColumnDescriptor(field).getName());
    	}
    	return columns;
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

    
    public String getFieldName(Method method){
        if (getFieldGetterMatcher().matches(method) ){
            for (String getterPrefix:getterPrefixes){
                if (method.getName().startsWith(getterPrefix)){
                    return StringUtil.underscorize(method.getName().substring(getterPrefix.length()));
                }
            }
        }else if(getFieldSetterMatcher().matches(method)){
            return StringUtil.underscorize(method.getName().substring(3));
        }
        return null;
    }


    private static final String[] getterPrefixes = new String[]{"get" , "is"};
    
    private Map<String,Method> fieldGetterMap = new IgnoreCaseMap<Method>();
    public Method getFieldGetter(String fieldName){
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
    }
    
    private Map<String,Method> fieldSetterMap = new IgnoreCaseMap<Method>();
    public Method getFieldSetter(String fieldName){
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
    }

    public ColumnDescriptor getColumnDescriptor(String fieldName){
        return getColumnDescriptor(getFieldGetter(fieldName));
    }

    private Map<String,ColumnDescriptor> columnDescriptors = new HashMap<String,ColumnDescriptor>();
    public ColumnDescriptor getColumnDescriptor(Method getter ){
        if (!getFieldGetterMatcher().matches(getter)){
            throw new RuntimeException("Method:" + getter.getName() + " is not recognizable as a a FieldGetter");
        }
        String getterSignature = Reflector.computeMethodSignature(getter);
        ColumnDescriptor cd = columnDescriptors.get(getterSignature);
        if (cd != null){
        	return cd;
        }
         
        Map<Class<? extends Annotation>, Annotation> map = new HashMap<Class<? extends Annotation>, Annotation>();
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
        }
        COLUMN_NAME name = (COLUMN_NAME) map.get(COLUMN_NAME.class);
        COLUMN_SIZE size = (COLUMN_SIZE) map.get(COLUMN_SIZE.class);
        DATA_TYPE type 	 = (DATA_TYPE) map.get(DATA_TYPE.class);
        DECIMAL_DIGITS digits = (DECIMAL_DIGITS) map.get(DECIMAL_DIGITS.class);
        IS_NULLABLE isNullable = (IS_NULLABLE)map.get(IS_NULLABLE.class);
        IS_AUTOINCREMENT isAutoIncrement = (IS_AUTOINCREMENT)map.get(IS_AUTOINCREMENT.class);
        IS_VIRTUAL isVirtual = (IS_VIRTUAL)map.get(IS_VIRTUAL.class);
        cd = null;
        if (name != null && !getFieldName(getter).equalsIgnoreCase(name.value())){
        	// Check if there is a field by that name
        	if (getFields().contains(name.value())){
        		cd = getColumnDescriptor(name.value());
        	}
        }
        if (cd == null){
	        cd = new ColumnDescriptor();
	        cd.setName(name == null ? getFieldName(getter) : name.value());
			TypeRef<?> typeRef = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType());
	        assert typeRef != null;
	        cd.setJDBCType(type == null ? typeRef.getJdbcType() : type.value());
	        cd.setNullable(isNullable != null ? isNullable.value() : !getter.getReturnType().isPrimitive());
	        cd.setSize(size == null? typeRef.getSize() : size.value());
	        cd.setScale(digits == null ? typeRef.getScale() : digits.value());
	        cd.setAutoIncrement(isAutoIncrement == null? false : true);
	        cd.setVirtual(isVirtual == null ? false : isVirtual.value());
        }
        columnDescriptors.put(getterSignature,cd);

        return cd;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass){
     	return reflector.isAnnotationPresent(getModelClass(), annotationClass);
     }

     public boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass ){
     	return reflector.isAnnotationPresent(getModelClass() ,method,  annotationClass) ; 
     }
     

     public <A extends Annotation> A getAnnotation(Class<A> annotationClass){
     	return reflector.getAnnotation(getModelClass(), annotationClass);
     }
     
     public <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass){
     	return reflector.getAnnotation(getModelClass() ,method, annotationClass);
     }
     
     public Class<? extends Model> getChildModelClass(Method method){
         Class<?> possibleChildClass = null;
         if (!getClassForests().contains(method.getDeclaringClass())){
         	return null;
         }
         if (getGetterMatcher().matches(method)){
             Class<?> retType = method.getReturnType();
             if (List.class.isAssignableFrom(retType)){
                 ParameterizedType parameterizedType = (ParameterizedType)method.getGenericReturnType();
                 possibleChildClass = (Class<?>)parameterizedType.getActualTypeArguments()[0];
             }else {
                 possibleChildClass = retType;
             }
             if (Model.class.isAssignableFrom(possibleChildClass)){
                 // Validate That child has a parentReferenceId. 
                 Class<? extends Model> childClass = (Class<? extends Model>)possibleChildClass;
                 ModelReflector<? extends Model> childReflector = ModelReflector.instance(childClass);
                 for (String fieldName: childReflector.getFields()){
                 	for (Class<? extends Model> parentModelClass: reflector.getSiblingModelClasses(getModelClass())){
 	                    if (fieldName.endsWith(StringUtil.underscorize(parentModelClass.getSimpleName() +"Id"))){
 	                        return childClass;
 	                    }
                 	}
                 }
             }
         }
         return null;
     }


     public Class<? extends Model> getReferredModelClass(Method method){
     	if (!getClassForests().contains(method.getDeclaringClass())) {
     		return null;
     	}
         Class<? extends Model> referredModelClass = null;
         Class<?> possibleReferredModelClass = method.getReturnType();
         if (Model.class.isAssignableFrom(possibleReferredModelClass) && getGetterMatcher().matches(method)){
             String referredIdFieldName = getReferredModelIdFieldName(method);
             if (getFields().contains(referredIdFieldName)){
                 referredModelClass = (Class<? extends Model>)possibleReferredModelClass;
             }
          }
         return referredModelClass;
         
     }

     public String getReferredModelIdFieldName(Method parentGetter){
		return StringUtil.underscorize(parentGetter.getName().substring(3) + "Id");
	}



    public Method getReferredModelGetterFor(Method referredModelIdGetter){
        if (!getFieldGetterMatcher().matches(referredModelIdGetter)){
            return null;
        }
        String methodName = referredModelIdGetter.getName();
        if (methodName.startsWith("get") && methodName.endsWith("Id") && !methodName.equals("getId") && 
        		(referredModelIdGetter.getReturnType() == int.class || referredModelIdGetter.getReturnType() == Integer.class)){
            String referredModelMethodName = methodName.substring(0,methodName.length()-"Id".length());
        	for (Class<? extends Model> modelClass : reflector.getSiblingModelClasses(getModelClass()) ){
        		try {
                	Method referredModelGetter = modelClass.getMethod(referredModelMethodName);
                    if (Model.class.isAssignableFrom(referredModelGetter.getReturnType())){
                        return referredModelGetter;
                    }
        		}catch (NoSuchMethodException ex){
        			//
        		}
        	}
        }
        return null;
    }
	public SequenceSet<Class<? extends Model>> getClassHierarchies() {
		return reflector.getClassHierarchies(getModelClass());
	}
	
	public SequenceSet<Class<?>> getClassForests(){
		return reflector.getClassForests(getModelClass());
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
            String mName = method.getName();
            Class<?> retType = method.getReturnType();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (  ((mName.startsWith("get") && retType != Void.TYPE) || 
                    mName.startsWith("is") && (boolean.class == retType || Boolean.class == retType) ) &&
                    (paramTypes == null || paramTypes.length == 0)){
                 return true;
            }
            return false;

        }
    }
    private final MethodMatcher fieldGetterMatcher = new FieldGetterMatcher();
    public MethodMatcher getFieldGetterMatcher() {
        return fieldGetterMatcher;
    }
    
    private class FieldGetterMatcher extends GetterMatcher{ 
        @Override
        public boolean matches(Method method){
			if (super.matches(method) && 
                    !Model.class.isAssignableFrom(method.getReturnType()) &&
                    Database.getJdbcTypeHelper().getTypeRef(method.getReturnType()) != null){
                 return true;
            }
            return false;

        }
    }

    private class SetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            String mName = method.getName();
            Class<?> retType = method.getReturnType();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (mName.startsWith("set") && (Void.TYPE == retType) && 
                    (paramTypes != null && paramTypes.length == 1) ){
                 return true;
            }
            return false;

        }
    }

    private final MethodMatcher fieldSetterMatcher = new FieldSetterMatcher();
    public MethodMatcher getFieldSetterMatcher() {
        return fieldSetterMatcher;
    }

    private class FieldSetterMatcher extends SetterMatcher{ 
        @Override
        public boolean matches(Method method){
			if (super.matches(method) && Database.getJdbcTypeHelper().getTypeRef(method.getParameterTypes()[0]) != null){
                 return true;
            }
            return false;

        }
    }
    
    //RelationShip Methods matchers
    private final MethodMatcher referredModelGetterMatcher=  new ReferredModelGetterMatcher();
    public MethodMatcher getReferredModelGetterMatcher(){ 
        return referredModelGetterMatcher;
    }
    
    private class ReferredModelGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return getReferredModelClass(method) != null;
        }
    }

    private final MethodMatcher participantModelGetterMatcher = new ParticipantModelGetterMatcher();
    public MethodMatcher getParticipantModelGetterMatcher(){ 
        return participantModelGetterMatcher;
    }
    
    private class ParticipantModelGetterMatcher extends ReferredModelGetterMatcher{
        public boolean matches(Method method){
            return isAnnotationPresent(method, PARTICIPANT.class) && super.matches(method);
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
