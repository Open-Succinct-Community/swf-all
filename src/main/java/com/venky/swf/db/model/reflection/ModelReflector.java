/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model.reflection;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private List<Method> allMethods = new ArrayList<Method>();
    private ModelReflector(Class<M> reflectedModelClass){
        this.reflectedModelClass = reflectedModelClass;
        if (!Model.class.isAssignableFrom(reflectedModelClass)){
            throw new RuntimeException(reflectedModelClass + " is not extending Model! ");
        }
        
        Class<?> modelClass = reflectedModelClass;
        do {
            if (modelClass.getInterfaces().length > 1){
                throw new RuntimeException ("Model interfaces must extend atmost one model Interface");
            }
            allMethods.addAll(0,getDeclaredMethods(modelClass));
            
            if (modelClass.getInterfaces().length == 0){
                modelClass = null ;
            }else {
                modelClass = modelClass.getInterfaces()[0];
            }
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
        String column = "name";
        HAS_DESCRIPTION_COLUMN descColumn = reflectedModelClass.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
        if (descColumn != null){
            column = descColumn.value();
        }
        return column;
    }
    public List<Method> getFieldGetters(){
        return getMethods(getFieldGetterMatcher());
    }
    
    public List<Method> getFieldSetters(){
        return getMethods(getFieldSetterMatcher());
    }
    
    public List<Method> getParentGetters(){ 
        return getMethods(getParentGetterMatcher());
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
            //Retain later presence of fields.
            Collections.reverse(allfields);
            Set<String> fieldSet = new HashSet<String>();
            Iterator<String> fieldIterator = allfields.iterator();
            while (fieldIterator.hasNext()){
            	String fieldName = fieldIterator.next();
            	if (fieldSet.contains(fieldName)){
            		fieldIterator.remove();
            	}
            	fieldSet.add(fieldName);
            }
            Collections.reverse(allfields);
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
            COLUMN_NAME name = getter.getAnnotation(COLUMN_NAME.class);
            COLUMN_SIZE size = getter.getAnnotation(COLUMN_SIZE.class);
            DATA_TYPE type = getter.getAnnotation(DATA_TYPE.class);
            DECIMAL_DIGITS digits = getter.getAnnotation(DECIMAL_DIGITS.class);
            IS_NULLABLE isNullable = getter.getAnnotation(IS_NULLABLE.class);
            IS_AUTOINCREMENT isAutoIncrement = getter.getAnnotation(IS_AUTOINCREMENT.class);
            IS_VIRTUAL isVirtual = getter.getAnnotation(IS_VIRTUAL.class);
            
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
    
    private static final MethodMatcher getterMatcher = new GetterMatcher();
    public static MethodMatcher getGetterMatcher(){
        return getterMatcher;
    }

    private static final MethodMatcher fieldGetterMatcher = new FieldGetterMatcher();
    public static MethodMatcher getFieldGetterMatcher() {
        return fieldGetterMatcher;
    }
    
    private static final MethodMatcher fieldSetterMatcher = new FieldSetterMatcher();
    public static MethodMatcher getFieldSetterMatcher() {
        return fieldSetterMatcher;
    }
    
    private static final MethodMatcher parentGetterMatcher=  new ParentGetterMatcher();
    public static MethodMatcher getParentGetterMatcher(){ 
        return parentGetterMatcher;
    }
    
    private static final MethodMatcher childrenGetterMatcher=  new ChildrenGetterMatcher();
    public static MethodMatcher getChildrenGetterMatcher(){ 
        return childrenGetterMatcher;
    }

    public static interface MethodMatcher {
        public boolean matches(Method method);
    }
    public static interface FieldMatcher {
        public boolean matches(ColumnDescriptor cd);
    }

    private static class RealFieldMatcher implements FieldMatcher {
        public boolean matches(ColumnDescriptor cd) {
            return !cd.isVirtual();
        }
    }
    private static class VirtualFieldMatcher implements FieldMatcher {
        public boolean matches(ColumnDescriptor cd) {
            return cd.isVirtual();
        }
    }
    private static class GetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            String mName = method.getName();
            Class<?> retType = method.getReturnType();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (  ((mName.startsWith("get") && retType != null) || 
                    mName.startsWith("is") && (boolean.class == retType || Boolean.class == retType) ) &&
                    (paramTypes == null || paramTypes.length == 0)){
                 return true;
            }
            return false;

        }
    }
    
    private static class ParentGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return getParentModelClass(method) != null;
        }
    }
    public static Class<? extends Model> getChildModelClass(Method method){
        Class<?> possibleChildClass = null;
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
    private static class ChildrenGetterMatcher implements MethodMatcher{
        public boolean matches(Method method){
            return (getChildModelClass(method) != null);
        }
    }
    
    
    private static class FieldGetterMatcher extends GetterMatcher{ 
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

    public static class SetterMatcher implements MethodMatcher{
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
    private static class FieldSetterMatcher extends SetterMatcher{ 
        @Override
        public boolean matches(Method method){
            if (super.matches(method) && Database.getInstance().getJdbcTypeHelper().getTypeRef(method.getParameterTypes()[0]) != null){
                 return true;
            }
            return false;

        }
    }

    public static Class<? extends Model> getParentModelClass(Method method){
        Class<? extends Model> modelClass = (Class<? extends Model>)method.getDeclaringClass();
        if (!Model.class.isAssignableFrom(modelClass)){
            return null;
        }
        ModelReflector<? extends Model> reflector = ModelReflector.instance(modelClass);
        Class<? extends Model> parentClass = null;
        Class<?> possibleParentClass = method.getReturnType();
        if (getGetterMatcher().matches(method) && Model.class.isAssignableFrom(possibleParentClass)){
            String parentIdFieldName = StringUtil.underscorize(method.getName().substring(3) + "Id");
            if (reflector.getFields().contains(parentIdFieldName)){
                parentClass = (Class<? extends Model>)possibleParentClass;
            }
         }
        return parentClass;
        
    }
    
    public static Method getParentModelGetterFor(Method parentIdGetter){
        Class<? extends Model> modelClass = (Class<? extends Model>)parentIdGetter.getDeclaringClass();
        if (!Model.class.isAssignableFrom(modelClass)){
            return null;
        }
        if (!getFieldGetterMatcher().matches(parentIdGetter)){
            return null;
        }
        String methodName = parentIdGetter.getName();
        if (methodName.startsWith("get") && methodName.endsWith("Id") && !methodName.equals("getId") && 
        		(parentIdGetter.getReturnType() == int.class || parentIdGetter.getReturnType() == Integer.class)){
            String parentModelMethodName = methodName.substring(0,methodName.length()-"Id".length());
            try {
                Method parentModelGetter = modelClass.getMethod(parentModelMethodName);
                if (Model.class.isAssignableFrom(parentModelGetter.getReturnType())){
                    return parentModelGetter;
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
            // TODO Auto-generated method stub
            
        }

        public void visitSource(String source, String debug) {
            // TODO Auto-generated method stub
            
        }

        public void visitOuterClass(String owner, String name, String desc) {
            // TODO Auto-generated method stub
            
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            // TODO Auto-generated method stub
            return null;
        }

        public void visitAttribute(Attribute attr) {
            // TODO Auto-generated method stub
            
        }

        public void visitInnerClass(String name, String outerName,
                String innerName, int access) {
            // TODO Auto-generated method stub
            
        }

        public FieldVisitor visitField(int access, String name, String desc,
                String signature, Object value) {
            // TODO Auto-generated method stub
            return null;
        }

        public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {
            // TODO Auto-generated method stub
            methodSequenceMap.put(name,methodSequenceMap.size());
            return null;
        }

        public void visitEnd() {
            // TODO Auto-generated method stub
            
        }
    }
    

}
