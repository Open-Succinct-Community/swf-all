/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.string.StringUtil;
import com.venky.reflection.Reflector;
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
import com.venky.swf.db.table.Table.ColumnDescriptor;

/**
 *
 * @author venky
 */
public class ModelReflector<M extends Model> extends Reflector<Model,M>{
    
    private static final Map<Class , ModelReflector>  map = new HashMap<Class, ModelReflector>();
    
    public static <M extends Model> ModelReflector<M> instance(Class<M> modelClass){
        ModelReflector<M> reflector = map.get(modelClass);
        if (reflector != null){
            return reflector;
        }
            
        synchronized(map){
            reflector = map.get(modelClass);
            if (reflector == null){
                reflector = new ModelReflector<M>(modelClass);
                map.put(modelClass, reflector);
            }
        }
        return reflector;
    }

    public <U extends Model> Class<U> getRealModelClass(){
    	List<Class<?>> modelClasses = getClassHierarchy();
    	Class<?> lastRealClass = null;
    	for (Class<?> claz:modelClasses){
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
    		return null;
    	}
		return (Class<U>) lastRealClass;
    }
    
    private ModelReflector(Class<M> reflectedModelClass){
    	super(reflectedModelClass,Model.class);
    }
    
    public String getDescriptionColumn(){
        String column = "NAME";
        HAS_DESCRIPTION_COLUMN descColumn = getAnnotation(HAS_DESCRIPTION_COLUMN.class);
        if (descColumn != null){
            column = descColumn.value();
        }
        if (!getFields().contains(column)){
        	column = "ID";
        }
        return column;
    }
    public List<Method> getFieldGetters(){
        return getMethods(getFieldGetterMatcher());
    }
    
    public List<Method> getFieldSetters(){
        return getMethods(getFieldSetterMatcher());
    }
    
    public List<Method> getReferredModelGetters(){ 
        return getMethods(getReferredModelGetterMatcher());
    }
    
    public List<Method> getParentModelGetters(){
    	return getMethods(getParentModelGetterMatcher());
    }
    public List<Method> getChildGetters(){
        return getMethods(getChildrenGetterMatcher());
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
        throw new RuntimeException(unrecognizedGetters.toString());
    }
    
    public Method getFieldSetter(String fieldName){
        try {
            Method getter = getFieldGetter(fieldName);
            
            String mName = "set"+StringUtil.camelize(fieldName);
            Method setter = reflectedClass.getMethod(mName,getter.getReturnType());
            if (getFieldSetterMatcher().matches(setter)){
                return setter; 
            }else {
                throw new RuntimeException(setter.getName() +" not recognized as a setter. Check Parameters and return type" );
            }
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }

    }
    
    public ColumnDescriptor getColumnDescriptor(String fieldName){
        return getColumnDescriptor(getFieldGetter(fieldName));
    }
    
    private Map<Method,ColumnDescriptor> columnDescriptors = new HashMap<Method,ColumnDescriptor>();
    
    public ColumnDescriptor getColumnDescriptor(Method getter){
        if (!getFieldGetterMatcher().matches(getter)){
            throw new RuntimeException("Method:" + getter.getName() + " is not recognizable as a a FieldGetter");
        }
        ColumnDescriptor cd = columnDescriptors.get(getter);
        
        if (cd == null){
            COLUMN_NAME name = getAnnotation(getter,COLUMN_NAME.class);
            COLUMN_SIZE size = getAnnotation(getter,COLUMN_SIZE.class);
            DATA_TYPE type = getAnnotation(getter,DATA_TYPE.class);
            DECIMAL_DIGITS digits = getAnnotation(getter,DECIMAL_DIGITS.class);
            IS_NULLABLE isNullable = getAnnotation(getter,IS_NULLABLE.class);
            IS_AUTOINCREMENT isAutoIncrement = getAnnotation(getter,IS_AUTOINCREMENT.class);
            IS_VIRTUAL isVirtual = getAnnotation(getter,IS_VIRTUAL.class);
            
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
        }
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
                ModelReflector<? extends Model> childReflector = ModelReflector.instance(childClass);
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
        ModelReflector<? extends Model> reflector = ModelReflector.instance(modelClass);
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
    

}
