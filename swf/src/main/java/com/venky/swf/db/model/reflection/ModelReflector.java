/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.string.StringUtil;
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
import com.venky.swf.db.annotations.column.validations.Mandatory;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;

/**
 *
 * @author venky
 */
public class ModelReflector {
    
    private static final Map<String , ModelReflector>  map = new HashMap<String, ModelReflector>();
    
    public static <M extends Model> ModelReflector instance(Class<M> modelClass){
    	String tableName =Table.tableName(modelClass);
        ModelReflector reflector = map.get(tableName);
        
        if (reflector != null){
        	reflector.registerModelClass(modelClass);
            return reflector;
        }
            
        synchronized(map){
            reflector = map.get(tableName);
            if (reflector == null){
                reflector = new ModelReflector(tableName);
                map.put(tableName, reflector);
            }
        }
        reflector.registerModelClass(modelClass);
        return reflector;
    }

    private List<Class<? extends Model>> modelClasses  = new ArrayList<Class<? extends Model>>();
    private Map<Class<? extends Model>, MReflector<? extends Model>> modelReflectors = new HashMap<Class<? extends Model>, ModelReflector.MReflector<? extends Model>>();
    
    private class MReflector<M extends Model> extends Reflector<Model, M>{
		protected MReflector(Class<M> reflectedClass) {
			super(reflectedClass, Model.class);
		}
    }
    
    public <M extends Model> void  registerModelClass(Class<M> modelClass){
    	if (!modelReflectors.containsKey(modelClass)){
    		modelClasses.add(modelClass);
    		modelReflectors.put(modelClass,new MReflector<M>(modelClass));
    	}
    }
    
    public <U extends Model> Class<U> getRealModelClass(){
    	Class<?> lastRealClass = null;
    	Class<?> modelClass = modelClasses.get(0); // First class in class path. (Application model if it exists.)
		MReflector<? extends Model> ref = modelReflectors.get(modelClass);
    	List<Class<? extends Model>> modelHierarchyClasses = ref.getClassHierarchy(); 
    	for (Class<?> claz:modelHierarchyClasses){
    		IS_VIRTUAL isVirtual = claz.getAnnotation(IS_VIRTUAL.class);
    		if (isVirtual != null){
    			if (isVirtual.value()){
    				lastRealClass = null;
    			}else if (lastRealClass == null){
    				lastRealClass = claz;
    			}
    		}else if (lastRealClass == null){
    			lastRealClass = claz;
    		}
    	}
    	if (lastRealClass == Model.class){
    		lastRealClass = null;
    	}
    	return (Class<U>) lastRealClass;
    }
    
    private final String modelName; 
    private ModelReflector(String modelName){
    	this.modelName = modelName;
    }
    
    public String getModelName(){
    	return modelName;
    }
    
    public String getTableName(){
    	Class<? extends Model> realModelClass = getRealModelClass();
    	if (realModelClass != null){
    		return Table.tableName(realModelClass);
    	}
    	return null;
    }
    
    public String getDescriptionColumn(){
        for (Class<? extends Model> modelClass: modelClasses){
        	MReflector<? extends Model> ref = modelReflectors.get(modelClass);
            HAS_DESCRIPTION_COLUMN descColumn = ref.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
            if (descColumn != null){
            	String column = descColumn.value();
                if (getFields().contains(column)){
                	return column;
                }
            }
        }
        if (getFields().contains("NAME")){
        	return "NAME";
        }
        return "ID";
    }
    
    private void loadMethods(List<Method> into , MethodMatcher matcher ){
    	if (!into.isEmpty()){
    		return;
    	}
    	synchronized (into) {
    		if (into.isEmpty()){
    			for (Class<? extends Model> modelClass:modelClasses){
    				into.addAll(modelReflectors.get(modelClass).getMethods(matcher));
    			}
    		}		
		}
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
    
    private List<Method> referredModelGetters = new ArrayList();
    public List<Method> getReferredModelGetters(){ 
    	loadMethods(referredModelGetters, getReferredModelGetterMatcher());
    	return referredModelGetters;
    }
    
    private List<Method> parentModelGetters = new ArrayList<Method>();
    public List<Method> getParentModelGetters(){
    	loadMethods(parentModelGetters,getParentModelGetterMatcher());
    	return parentModelGetters;
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
    
    public Method getFieldGetter(String fieldName){
        StringBuilder unrecognizedGetters = new StringBuilder();
        
    	for (Class<? extends Model> reflectedClass: modelClasses){
	        for (String getterPrefix:getterPrefixes){
	            String getterName = getterPrefix + StringUtil.camelize(fieldName);
        		try {
	                Method getter = reflectedClass.getMethod(getterName, new Class<?>[]{});
	                if (getFieldGetterMatcher().matches(getter)){
	                    return getter;
	                }
	                unrecognizedGetters.append("GetterNotRecognized. Check Return type").append(" ").append(getterName);
                }catch(NoSuchMethodException ex){
                    unrecognizedGetters.append(ex.toString()).append(" ").append(getterName);
                }catch (SecurityException ex){
                    unrecognizedGetters.append(ex.toString()).append(" ").append(getterName);
                }
        	}
        }
        throw new RuntimeException(unrecognizedGetters.toString());
    }
    
    public Method getFieldSetter(String fieldName){
    	StringBuilder unrecognizedSetters = new StringBuilder();
    	
        Method getter = getFieldGetter(fieldName);
    	String setterName = "set"+StringUtil.camelize(fieldName);
    	for (Class<? extends Model> reflectedClass: modelClasses){
	        try {
                Method setter = reflectedClass.getMethod(setterName,getter.getReturnType());
                if (getFieldSetterMatcher().matches(setter)){
                    return setter; 
                }else {
                    unrecognizedSetters.append(reflectedClass.getName() + "." + setter.getName() +" not recognized as a setter. Check Parameters and return type " );
                }
	        } catch (NoSuchMethodException ex) {
                unrecognizedSetters.append(ex.toString() + reflectedClass.getName() + "." + setterName + " ");
	        } catch (SecurityException ex) {
                unrecognizedSetters.append(ex.toString() + reflectedClass.getName() + "." + setterName + " ");
	        }
    	}
    	throw new RuntimeException(unrecognizedSetters.toString());
    }
    
    public ColumnDescriptor getColumnDescriptor(String fieldName){
        return getColumnDescriptor(getFieldGetter(fieldName));
    }
    
    private Map<Method,ColumnDescriptor> columnDescriptors = new HashMap<Method,ColumnDescriptor>();
    
    public ColumnDescriptor getColumnDescriptor(Method getter){
        if (!getFieldGetterMatcher().matches(getter)){
            throw new RuntimeException("Method:" + getter.getName() + " is not recognizable as a a FieldGetter");
        }

        ColumnDescriptor cd = columnDescriptors.get(Reflector.computeMethodSignature(getter));
        if (cd != null){
        	return cd;
        }
         
        Map<Class<? extends Annotation>, Annotation> map = new HashMap<Class<? extends Annotation>, Annotation>();
        for (Class<? extends Model> modelClass:modelClasses){
        	MReflector<? extends Model> ref = modelReflectors.get(modelClass);
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
        
        cd = new ColumnDescriptor();
        cd.setName(name == null ? getFieldName(getter) : name.value());
        TypeRef<?> typeRef = Database.getInstance().getJdbcTypeHelper().getTypeRef(getter.getReturnType());
        assert typeRef != null;
        cd.setJDBCType(type == null ? typeRef.getJdbcType() : type.value());
        cd.setNullable(isNullable != null ? isNullable.value() : !getter.getReturnType().isPrimitive());
        cd.setSize(size == null? typeRef.getSize() : size.value());
        cd.setScale(digits == null ? typeRef.getScale() : digits.value());
        cd.setAutoIncrement(isAutoIncrement == null? false : true);
        cd.setVirtual(isVirtual == null ? false : isVirtual.value());
        columnDescriptors.put(getter,cd);

        return cd;
    }
    
    private final MethodMatcher getterMatcher = new GetterMatcher();
    public  MethodMatcher getGetterMatcher(){
        return getterMatcher;
    }

    private final MethodMatcher fieldGetterMatcher = new FieldGetterMatcher();
    public MethodMatcher getFieldGetterMatcher() {
        return fieldGetterMatcher;
    }
    
    private final MethodMatcher fieldSetterMatcher = new FieldSetterMatcher();
    public MethodMatcher getFieldSetterMatcher() {
        return fieldSetterMatcher;
    }
    
    private final MethodMatcher referredModelGetterMatcher=  new ReferredModelGetterMatcher();
    public MethodMatcher getReferredModelGetterMatcher(){ 
        return referredModelGetterMatcher;
    }
    
    private final MethodMatcher parentModelGetterMatcher = new ParentModelGetterMatcher();
    
    private MethodMatcher getParentModelGetterMatcher() {
    	return parentModelGetterMatcher;
    }
    
    private final MethodMatcher childrenGetterMatcher=  new ChildrenGetterMatcher();
    public MethodMatcher getChildrenGetterMatcher(){ 
        return childrenGetterMatcher;
    }

    public interface FieldMatcher {
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
    
    private class ReferredModelGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return getReferredModelClass(method) != null;
        }
    }
    
    private class ParentModelGetterMatcher extends ReferredModelGetterMatcher{
    	public boolean matches(Method method){
            boolean isReferredModelGetter =  super.matches(method);
            if (isReferredModelGetter){
            	String referredModelIdFieldName = getReferredModelIdFieldName(method);
            	return isAnnotationPresent(getFieldGetter(referredModelIdFieldName),Mandatory.class);
            }
            return isReferredModelGetter;
        }
    }
    
    public boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass ){
    	for (Class<? extends Model> modelClass: modelClasses){
    		MReflector<? extends Model> ref = modelReflectors.get(modelClass);
    		if (ref.isAnnotationPresent(method,annotationClass)){
    			return true;
    		}
    	}
    	return false;
    }
    
    public <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass){
    	A annotation = null; 
    	for (Class <? extends Model> modelClass: modelClasses){
    		MReflector<? extends Model> ref = modelReflectors.get(modelClass);
    		annotation = ref.getAnnotation(method,annotationClass);
    		if (annotation != null){
    			break;
    		}
    	}
    	return annotation;
    }
    
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass){
    	for (Class <? extends Model> modelClass: modelClasses){
    		MReflector<? extends Model> ref = modelReflectors.get(modelClass);
    		if (ref.isAnnotationPresent(annotationClass)){
    			return true;
    		}
    	}
    	return false;
    }
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass){
    	A annotation = null;
    	for (Class <? extends Model> modelClass: modelClasses){
    		MReflector<? extends Model> ref = modelReflectors.get(modelClass);
    		annotation = ref.getAnnotation(annotationClass);
    		if (annotation != null){
    			break;
    		}
    	}
    	return annotation;
    }
        
    public Class<? extends Model> getChildModelClass(Method method){
        Class<?> possibleChildClass = null;
        @SuppressWarnings("unchecked")
		Class<? extends Model> parentModelClass = (Class<? extends Model>)method.getDeclaringClass();
        if (!Model.class.isAssignableFrom(parentModelClass)){
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
                ModelReflector childReflector = ModelReflector.instance(childClass);
                for (String fieldName: childReflector.getFields()){
                    if (fieldName.endsWith(StringUtil.underscorize(parentModelClass.getSimpleName() +"Id"))){
                        return childClass;
                    }
                }
            }
        }
        return null;
    }
    
    private class ChildrenGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return (getChildModelClass(method) != null);
        }
    }
    
    
    private class FieldGetterMatcher extends GetterMatcher{ 
        @Override
        public boolean matches(Method method){
            if (super.matches(method) && 
                    !Model.class.isAssignableFrom(method.getReturnType()) &&
                    Database.getInstance().getJdbcTypeHelper().getTypeRef(method.getReturnType()) != null){
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
    private class FieldSetterMatcher extends SetterMatcher{ 
        @Override
        public boolean matches(Method method){
            if (super.matches(method) && Database.getInstance().getJdbcTypeHelper().getTypeRef(method.getParameterTypes()[0]) != null){
                 return true;
            }
            return false;

        }
    }

    public Class<? extends Model> getReferredModelClass(Method method){
        Class<? extends Model> modelClass = (Class<? extends Model>)method.getDeclaringClass();
        if (!Model.class.isAssignableFrom(modelClass)){
            return null;
        }
        ModelReflector reflector = ModelReflector.instance(modelClass);
        Class<? extends Model> referredModelClass = null;
        Class<?> possibleReferredModelClass = method.getReturnType();
        if (Model.class.isAssignableFrom(possibleReferredModelClass) && getGetterMatcher().matches(method)){
            String referredIdFieldName = getReferredModelIdFieldName(method);
            if (reflector.getFields().contains(referredIdFieldName)){
                referredModelClass = (Class<? extends Model>)possibleReferredModelClass;
            }
         }
        return referredModelClass;
        
    }
    
    public String getReferredModelIdFieldName(Method parentGetter){
    	return StringUtil.underscorize(parentGetter.getName().substring(3) + "Id");
    }
    
    public Method getReferredModelGetterFor(Method referredModelIdGetter){
        Class<? extends Model> modelClass = (Class<? extends Model>)referredModelIdGetter.getDeclaringClass();
        if (!Model.class.isAssignableFrom(modelClass)){
            return null;
        }
        if (!getFieldGetterMatcher().matches(referredModelIdGetter)){
            return null;
        }
        String methodName = referredModelIdGetter.getName();
        if (methodName.startsWith("get") && methodName.endsWith("Id") && !methodName.equals("getId") && 
        		(referredModelIdGetter.getReturnType() == int.class || referredModelIdGetter.getReturnType() == Integer.class)){
            String referredModelMethodName = methodName.substring(0,methodName.length()-"Id".length());
            try {
                Method referredModelGetter = modelClass.getMethod(referredModelMethodName);
                if (Model.class.isAssignableFrom(referredModelGetter.getReturnType())){
                    return referredModelGetter;
                }
            } catch (Exception ex) {
                //
            }
        }
        return null;
    }
    
    List<Class<?>> classHierarchies = new ArrayList<Class<?>>();
    public List<Class<?>> getClassHierarchies(){
    	if (classHierarchies.isEmpty()){
    		synchronized (classHierarchies) {
				if (classHierarchies.isEmpty()){
					Set<Class<?>> classesAdded = new HashSet<Class<?>>();
					
					for (Class<? extends Model> modelClass : modelClasses){
						for (Class <? extends Model> classInHeirarchy: modelReflectors.get(modelClass).getClassHierarchy()){
							if (!classesAdded.contains(classInHeirarchy)){ //Simpler to do a hash Lookup.
								classHierarchies.add(classInHeirarchy);
								classesAdded.add(classInHeirarchy);
							}
						}
					}
				}
			}
    	}
    	return classHierarchies;
    }
}
