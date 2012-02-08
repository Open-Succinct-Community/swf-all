/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.string.StringUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefaulter;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.validations.processors.EnumerationValidator;
import com.venky.swf.db.annotations.column.validations.processors.ExactLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.FieldValidator;
import com.venky.swf.db.annotations.column.validations.processors.MandatoryValidator;
import com.venky.swf.db.annotations.column.validations.processors.MaxLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.RegExValidator;
import com.venky.swf.db.annotations.operation.Operation;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.InvalidOperation;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Delete;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Insert;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Update;

/**
 *
 * @author venky
 */
public class ModelImpl<M extends Model> implements InvocationHandler {

    private Record record = null;
    private Class<M> modelClass = null;
    private M proxy = null;
    private ModelReflector<M> reflector = null;
    private List<String> virtualFields = new IgnoreCaseList();

    public Record getRecord() {
		return record;
	}

	public Class<M> getModelClass() {
		return modelClass;
	}

	public ModelReflector<M> getReflector() {
		return reflector;
	}

	public ModelImpl(Class<M> modelClass, Record record) {
        this.record = record;
        this.modelClass = modelClass;
        this.reflector = ModelReflector.instance(modelClass);
        this.virtualFields = reflector.getVirtualFields();
        record.startTracking();
    }
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        setProxy(modelClass.cast(proxy));

        String mName = method.getName();
        Class<?> retType = method.getReturnType();
        Class<?>[] parameters = method.getParameterTypes();

        if (getReflector().getFieldGetterMatcher().matches(method)) {
            String fieldName = getReflector().getFieldName(method);
            if (!virtualFields.contains(fieldName)){
                ColumnDescriptor cd = getReflector().getColumnDescriptor(method);
                String columnName = cd.getName();

                Object value = record.get(columnName);

                TypeRef<?> ref =Database.getInstance().getJdbcTypeHelper().getTypeRef(retType);
                TypeConverter<?> converter = ref.getTypeConverter();
                if (value == null) {
                	Object defaultValue = null;
                	COLUMN_DEF colDef = getReflector().getAnnotation(method,COLUMN_DEF.class);
                	if (colDef != null){
                		defaultValue = StandardDefaulter.getDefaultValue(colDef.value());
                	}

                	return cd.isNullable() ? defaultValue : converter.valueOf(defaultValue);
                } else if (retType.isInstance(value)) {
                    return value;
                } else {
                    return converter.valueOf(value);
                }
            }
        } else if (getReflector().getFieldSetterMatcher().matches(method) ) {
            String fieldName = StringUtil.underscorize(mName.substring(3));
            if (!virtualFields.contains(fieldName)){
                String columnName = getReflector().getColumnDescriptor(fieldName).getName(); 
                return record.put(columnName, args[0]);
            }
        } else if (getReflector().getReferredModelGetterMatcher().matches(method)) {
            return getParent(method);
        } else if (getReflector().getChildrenGetterMatcher().matches(method)) {
            if (Model.class.isAssignableFrom(method.getReturnType())){
                return getChild((Class<? extends Model>) method.getReturnType());
            }else {
            	CONNECTED_VIA join = reflector.getAnnotation(method,CONNECTED_VIA.class);
            	if (join != null){
            		return getChildren(getReflector().getChildModelClass(method),join.value());
            	}else {
            		return getChildren(getReflector().getChildModelClass(method));
            	}
            }
        }

        Method inCurrentClass = this.getClass().getMethod(mName, parameters);
        if (retType.isAssignableFrom(inCurrentClass.getReturnType())) {
        	if (isOperationValid(inCurrentClass)){
                return inCurrentClass.invoke(this, args);
        	}else {
        		throw new NoSuchMethodException("Invalid Operation");
        	}
        } else {
            throw new NoSuchMethodException("Donot know how to execute this method");
        }
        

    }

    public boolean isOperationValid(Method inCurrentClass){
    	if (!inCurrentClass.isAnnotationPresent(Operation.class)){
    		return true;
    	}
  
    	try {
    		Registry.instance().callExtensions("validate.operation", inCurrentClass.getName(), getProxy());
    		return true;
    	}catch (InvalidOperation ex){
        	return false;
    	}
    }
    @SuppressWarnings("unchecked")
	public <P extends Model> P getParent(Method parentGetter) {
    	Class<P> parentClass = (Class<P>) parentGetter.getReturnType();
    	
    	String parentIdFieldName =  StringUtil.underscorize(parentGetter.getName().substring(3) +"Id");
    	
    	Method parentIdGetter = this.reflector.getFieldGetter(parentIdFieldName);
    	
    	Integer parentId;
		try {
			parentId = (Integer)parentIdGetter.invoke(proxy);
		} catch (Exception e) {
			throw new RuntimeException(parentIdFieldName,e);
		} 
		P parent = null;
		if (parentId != null) {
			parent = Database.getInstance().getTable(parentClass).get(parentId);
		}
        return parent;
    }
    
    public <C extends Model> C getChild(Class<C> childClass){
    	C child = null; 
        List<C> children =  getChildren(childClass);
        if (children.size() > 0){
        	child = children.get(0);
        }
        return child;
    }
    public <C extends Model> List<C> getChildren(Class<C> childClass){
    	List<C> children = new ArrayList<C>();
    	
    	ModelReflector<? extends Model> childReflector = ModelReflector.instance(childClass);
        for (String fieldName: childReflector.getFields()){
            if (fieldName.endsWith(StringUtil.underscorize(getModelClass().getSimpleName() +"Id"))){
                children.addAll(getChildren(childClass, fieldName));
            }
        }

    	return children;
    }
    public <C extends Model> List<C> getChildren(Class<C> childClass, String parentIdFieldName){
    	int parentId = proxy.getId();
    	String parentIdColumnName = ModelReflector.instance(childClass).getColumnDescriptor(parentIdFieldName).getName();

    	Select  q = new Select();
    	q.from(childClass);
    	q.where(new Expression(parentIdColumnName,Operator.EQ,new BindVariable(parentId)));
    	return q.execute();
    }

    public void setProxy(M proxy) {
        this.proxy = proxy;
    }

    public M getProxy() {
		return proxy;
	}
    
    public boolean isAccessibleBy(User user){
    	Map<String,List<Integer>> columnNameValues = user.getParticipationOptions(modelClass);
    	if (columnNameValues.isEmpty()){
    		return true;
    	}
    	for (String fieldName:columnNameValues.keySet()){
    		List values = columnNameValues.get(fieldName);
    		String columnName = reflector.getColumnDescriptor(fieldName).getName();
    		if (values.contains(record.get(columnName))) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public Record getRawRecord(){
    	return record;
    }

	private static Map<Class<?>,Class<?>> modelImplMap = new HashMap<Class<?>, Class<?>>();
    
	@SuppressWarnings("unchecked")
	public static <M extends Model> M getProxy(Class<M> modelClass, Record record) {
		Class<?> modelImplClass = modelImplMap.get(modelClass)	;		
		if (modelImplClass == null){
			synchronized (modelImplMap) {
				modelImplClass = modelImplMap.get(modelClass);
				if (modelImplClass == null){
					modelImplClass = getModelImplClass(modelClass);
					modelImplMap.put(modelClass, modelImplClass);
				}
			}
		}
		try {
			Constructor<?> c = modelImplClass.getConstructor(Class.class, Record.class);
	    	ModelImpl<M> mImpl = (ModelImpl<M>) c.newInstance(modelClass, record);
	    	M m = modelClass.cast(Proxy.newProxyInstance(modelClass.getClassLoader(), new Class[]{modelClass}, mImpl));
	    	mImpl.setProxy(m);
	    	return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
    
	private static Class<?> getModelImplClass(Class<? extends Model> modelClass){

		List<Class<?>> modelClasses = ModelReflector.instance(modelClass).getModelInterfaceHierarchy();
		Class modelImplClass = null;
		for (Class<?> c : modelClasses){
			String modelImplClassName = c.getName()+"Impl";
			try { 
				modelImplClass = (Class<?>) Class.forName(modelImplClassName);
				return modelImplClass;
			}catch(ClassNotFoundException ex){
				modelImplClass = null;
			}
		}
		
		return ModelImpl.class;
		
	}

    public void save() {
        if (record.getDirtyFields().isEmpty()) {
            return;
        }
        validate();
        beforeSave();
        if (record.isNewRecord()) {
            create();
        } else {
            update();
        }
        afterSave();

    }
    public void init(){
    	
    }
    private final static List<FieldValidator<? extends Annotation>> validators = new ArrayList<FieldValidator<? extends Annotation>>();

    static {
        validators.add(new ExactLengthValidator());
        validators.add(new MaxLengthValidator());
        validators.add(new MandatoryValidator());
        validators.add(new RegExValidator());
        validators.add(new EnumerationValidator());
    }

    protected boolean isModelValid(StringBuilder totalMessage) {
        List<String> fields = reflector.getFields();
        totalMessage.append(modelClass.getSimpleName()).append(":<br/>");
        boolean ret = true;
        for (String field : fields) {
            StringBuilder message = new StringBuilder();
            Object value = record.get(field);
            Method getter = reflector.getFieldGetter(field);
            if (!isFieldValid(getter, value, message)) {
                totalMessage.append("<br/>").append(field).append("=").append(value).append(":").append(message);
                ret = false;
            }
        }
        return ret;
    }

    protected boolean isFieldValid(Method getter, Object value, StringBuilder message) {
        boolean ret = true;
        Iterator<FieldValidator<? extends Annotation>> i = validators.iterator();
        while (i.hasNext()) {
            FieldValidator<? extends Annotation> v = i.next();
            ret = v.isValid(getReflector().getAnnotation(getter,v.getAnnotationClass()), value, message) && ret;
        }
        return ret;

    }

    protected void validate(){
    	beforeValidate();
    	StringBuilder errmsg = new StringBuilder();
        if (!isModelValid(errmsg)) {
            throw new RuntimeException(errmsg.toString());
        }
        afterValidate();
    }
    protected void beforeValidate(){
    	defaultFields();
    }
    protected void defaultFields(){
        for (String field:reflector.getRealFields()){
        	String columnName = reflector.getColumnDescriptor(field).getName();
        	if (record.get(columnName) == null){
        		Method fieldGetter = reflector.getFieldGetter(field);
        		COLUMN_DEF cdef = reflector.getAnnotation(fieldGetter,COLUMN_DEF.class);
        		if (cdef != null){
        			Object defaultValue = StandardDefaulter.getDefaultValue(cdef.value());
        			record.put(columnName,defaultValue);
        		}
        	}
        }
    }
    protected void afterValidate(){}
    protected void beforeSave() {}
    protected void afterSave() {}
    protected void beforeDestory(){}
    protected void afterDestroy(){}
    
    public void destroy() {
    	beforeDestory();
        Delete q = new Delete(modelClass);
        Expression condition = new Expression(Conjunction.AND);
        condition.add(new Expression(getReflector().getColumnDescriptor("id").getName(),Operator.EQ,new BindVariable(proxy.getId())));
        condition.add(new Expression(getReflector().getColumnDescriptor("lock_id").getName(),Operator.EQ,new BindVariable(proxy.getLockId())));
        
        q.where(condition);
        
        q.executeUpdate();
        try {
			Database.getInstance().getCache(getModelClass()).remove(getProxy());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
        
        afterDestroy();
    }

    private void update() {
        int oldLockId = proxy.getLockId();
        int newLockId = oldLockId + 1;

        Table table = Database.getInstance().getTable(reflector.getRealModelClass());
        Update q = new Update(modelClass);
        Iterator<String> fI = record.getDirtyFields().iterator();
        while (fI.hasNext()) {
            String columnName = fI.next();
            q.set(columnName,new BindVariable(record.get(columnName),table.getColumnDescriptor(columnName).getJDBCType()));
        }
        
        String idColumn = getReflector().getColumnDescriptor("id").getName();
        String lockidColumn = getReflector().getColumnDescriptor("lock_id").getName();
        q.set(lockidColumn,new BindVariable(newLockId));
        
        Expression condition = new Expression(Conjunction.AND);
        condition.add(new Expression(idColumn,Operator.EQ,new BindVariable(proxy.getId())));
        condition.add(new Expression(lockidColumn,Operator.EQ,new BindVariable(oldLockId)));

        q.where(condition);
        
        q.executeUpdate();

        proxy.setLockId(newLockId);
        record.startTracking();
    }

    private void create() {
        proxy.setLockId(0);
        Table<M> table = Database.getInstance().getTable(modelClass);
        Insert insertSQL = new Insert(modelClass);
        Map<String,BindVariable> values = new HashMap<String, BindVariable>();
        
        Iterator<String> columnIterator = record.getDirtyFields().iterator();
        while (columnIterator.hasNext()) {
            String columnName = columnIterator.next();
            values.put(columnName, new BindVariable(record.get(columnName),table.getColumnDescriptor(columnName).getJDBCType()));
        }
        insertSQL.values(values);
        
        
        Record generatedValues = new Record();
        String[] generatedKeys = table.getAutoIncrementColumns();
        assert (generatedKeys.length <= 1); // atmost one auto increment id column
        
        insertSQL.executeUpdate(generatedValues, generatedKeys);
        if (generatedKeys.length == 1){
            assert (generatedValues.getDirtyFields().size() == 1);
            String fieldName = generatedKeys[0];
            String virtualFieldName = generatedValues.getDirtyFields().first();
            int id = ((Number)generatedValues.get(virtualFieldName)).intValue();
            record.put(fieldName, id);
        }

        record.setNewRecord(false);
        record.startTracking();
        try {
			Database.getInstance().getCache(getModelClass()).add(getProxy());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    }
    @Override
    public boolean equals(Object o){
    	if (!(o instanceof ModelImpl) && !modelClass.isInstance(o)){
    		return false;
    	}
    	if (o instanceof ModelImpl){
    		return equalImpl((ModelImpl)o);
    	}else {
    		return equalsProxy((M)o);
    	}
    }
    
    private boolean equalImpl(ModelImpl anotherImpl){
    	return (getProxy().getId() == anotherImpl.getProxy().getId()) && getModelClass().equals(anotherImpl.getModelClass()); 
    }
    
    private boolean equalsProxy(M anotherProxy){
    	boolean ret = false;
    	if (anotherProxy != null){
    		ret = getModelClass().isInstance(anotherProxy) && getProxy().getId() == anotherProxy.getId();
    	}
    	System.out.println("equalsProxyCalled. returning " + ret);
    	return ret;
    }
    
}
