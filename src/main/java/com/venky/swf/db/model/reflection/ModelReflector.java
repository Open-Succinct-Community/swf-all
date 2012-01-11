/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model.reflection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.string.StringUtil;
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
public class ModelReflector<M extends Model> {
    
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
    
    private Class<M> reflectedModelClass  = null;
    
    private Map<Method,String> methodSignature = new HashMap<Method, String>();
    private Map<String,List<Method>> methodsWithSameSignature = new HashMap<String, List<Method>>();
    private List<Method> allMethods = null;
    
    private String getMethodSignature(Method method){
    	String ret = methodSignature.get(method);
    	if (ret != null){
    		return ret;
    	}
    	int modifiers = method.getModifiers();
    	StringBuilder sign = new StringBuilder();
		sign.append(Modifier.isPublic(modifiers) ? "public " : Modifier.isProtected(modifiers) ? "protected " : Modifier.isPrivate(modifiers)? "private " : "");
		sign.append(method.getReturnType().toString() + " ");
		sign.append(method.getName() + "(");
		Class<?>[] pt = method.getParameterTypes();
		for (int i = 0 ; i< pt.length ; i++ ){
			if (i > 0){
				sign.append(",");
			}
			sign.append(pt[i]);
		}
		sign.append(")");

    	
		ret = sign.toString();
    	methodSignature.put(method, ret);
    	return ret;
    }
    private List<Method> getMethodsForSignature(String signature){
    	List<Method> methods = methodsWithSameSignature.get(signature);
    	if (methods == null){
    		methods = new ArrayList<Method>();
    		methodsWithSameSignature.put(signature, methods);
    	}
    	return methods;
    }
    
    public boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass){
    	if (method.isAnnotationPresent(annotationClass)){
    		return true;
    	}
    	boolean present = false;
    	List<Method> methods = getMethodsForSignature(getMethodSignature(method)); 
    	for (int i = 0 ; !present && i < methods.size() ; i ++){
    		Method m = methods.get(i);
    		present = m.isAnnotationPresent(annotationClass);
    	}
    	return present;
    }
    
    public <T extends Annotation> T getAnnotation(Method method,Class<T> annotationClass){
    	if (method.isAnnotationPresent(annotationClass)){
    		return method.getAnnotation(annotationClass);
    	}
    	T annotation = null;
    	List<Method> methods = getMethodsForSignature(getMethodSignature(method)); 
    	for (int i = 0 ; annotation == null && i < methods.size() ; i ++){
    		Method m = methods.get(i);
    		annotation = m.getAnnotation(annotationClass);
    	}
    	return annotation;
    }
    
    private void loadAllInterfaces(Class<?> interfaceClass,List<Class<?>> interfaces){
    	interfaces.add(interfaceClass);
    	for (Class<?> infcClass: interfaceClass.getInterfaces()){
    		loadAllInterfaces(infcClass, interfaces);
    	}
    }
    private Class<?> getParentModelInterface(Class<?> aClass,List<Class<?>> otherInterfaces){
        if (!Model.class.isAssignableFrom(aClass)){
            throw new RuntimeException(aClass + " is not extending Model! ");
        }
        
    	List<Class<?>> modelInterfaces = new ArrayList<Class<?>>();
    	for (Class<?> infc: aClass.getInterfaces()){
    		if (Model.class.isAssignableFrom(infc)){
    			modelInterfaces.add(infc);
    		}else{ 
    			loadAllInterfaces(infc,otherInterfaces);
			}
    	}
    	
    	if (modelInterfaces.isEmpty()){
    		return null;
    	}else if (modelInterfaces.size() > 1){
    		throw new RuntimeException ("Model interfaces must extend atmost one model Interface");
    	}
    	else {
    		return modelInterfaces.get(0);
    	}
    }
    private ModelReflector(Class<M> reflectedModelClass){
        Class<?> modelClass = reflectedModelClass;
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        this.reflectedModelClass = reflectedModelClass;
        this.allMethods = new ArrayList<Method>(modelClass.getMethods().length);
        
        do {
        	interfaces.clear();
        	interfaces.add(modelClass);
        	Class<?> parentModelClass = getParentModelInterface(modelClass,interfaces);
        	int index = 0;
        	for (Class<?> clazz: interfaces){
                for (Method m :getDeclaredMethods(clazz)){
                	List<Method> methodsForSignature = getMethodsForSignature(getMethodSignature(m));
                	if (methodsForSignature.isEmpty()){
                		if (clazz.equals(Model.class)){
                			allMethods.add(m);
                		}else {
                			allMethods.add(index,m);
                    		index++;
                		}
                	}
                	methodsForSignature.add(m);
                }
        		
        	}
            modelClass = parentModelClass;
        }while(modelClass != null);
        
    }
    private List<Method> getDeclaredMethods(Class<?> forClass){ 
        List<Method> methods = new ArrayList<Method>(); 
        methods.addAll(Arrays.asList(forClass.getDeclaredMethods()));
        try {
            ClassReader reader = new ClassReader(getClass().getClassLoader().getResourceAsStream(forClass.getName().replace('.', '/')+ ".class"));
            ModelVisitor mv = new ModelVisitor();
            reader.accept(mv, 0);

            final Map<String,Integer> mSeq = mv.getMethodSequenceMap();
            Collections.sort(methods,new Comparator<Method>(){
                public int compare(Method o1, Method o2) {
                    return mSeq.get(o1.getName()).compareTo(mSeq.get(o2.getName()));
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        return methods;
    }
    
    public final List<Method> getMethods(MethodMatcher matcher){
        List<Method> methods = new ArrayList<Method>();
        for (Method method:allMethods){
            if (matcher.matches(method)){
                methods.add(method);
            }
        }
        return methods;

    }
    
    public String getDescriptionColumn(){
        String column = "NAME";
        HAS_DESCRIPTION_COLUMN descColumn = reflectedModelClass.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
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
                Method getter = reflectedModelClass.getMethod(getterName, new Class<?>[]{});
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
            Method setter = reflectedModelClass.getMethod(mName,getter.getReturnType());
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

    public interface MethodMatcher {
        public boolean matches(Method method);
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
    
    private class ModelVisitor implements ClassVisitor {
        private Map<String, Integer> methodSequenceMap = new HashMap<String, Integer>();
        
        public Map<String, Integer> getMethodSequenceMap() {
            return methodSequenceMap;
        }

        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {
            
            
        }

        public void visitSource(String source, String debug) {
            
            
        }

        public void visitOuterClass(String owner, String name, String desc) {
            
            
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            
            return null;
        }

        public void visitAttribute(Attribute attr) {
            
            
        }

        public void visitInnerClass(String name, String outerName,
                String innerName, int access) {
            
            
        }

        public FieldVisitor visitField(int access, String name, String desc,
                String signature, Object value) {
            
            return null;
        }

        public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {
            
            methodSequenceMap.put(name,methodSequenceMap.size());
            return null;
        }

        public void visitEnd() {
            
            
        }
    }
    

}
