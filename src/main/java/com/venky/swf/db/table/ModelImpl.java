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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.validations.processors.EnumerationValidator;
import com.venky.swf.db.annotations.column.validations.processors.ExactLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.FieldValidator;
import com.venky.swf.db.annotations.column.validations.processors.MandatoryValidator;
import com.venky.swf.db.annotations.column.validations.processors.MaxLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.RegExValidator;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

/**
 *
 * @author venky
 */
public class ModelImpl<M extends Model> implements InvocationHandler {

    private Record record = null;
    private Class<M> modelClass = null;
    private M proxy = null;
    private ModelReflector<M> reflector = null;
    private List<String> virtualFields = new ArrayList<String>();

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

        if (ModelReflector.getFieldGetterMatcher().matches(method)) {
            String fieldName = getReflector().getFieldName(method);
            if (!virtualFields.contains(fieldName)){
                Object value = record.get(fieldName);
                TypeConverter<?> converter = Database.getInstance().getJdbcTypeHelper().getTypeRef(retType).getTypeConverter();
                if (value == null) {
                    return converter.valueOf(null);
                } else if (retType.isInstance(value)) {
                    return value;
                } else {
                    return converter.valueOf(value);
                }
            }
        } else if (ModelReflector.getFieldSetterMatcher().matches(method) ) {
            String fieldName = StringUtil.underscorize(mName.substring(3));
            if (!virtualFields.contains(fieldName)){
                return record.put(fieldName, args[0]);
            }
        } else if (ModelReflector.getParentGetterMatcher().matches(method)) {
            return getParent(method);
        } else if (ModelReflector.getChildrenGetterMatcher().matches(method)) {
            if (Model.class.isAssignableFrom(method.getReturnType())){
                return getChild((Class<? extends Model>) method.getReturnType());
            }else {
                return getChildren(ModelReflector.getChildModelClass(method));
            }
        }

        Method inCurrentClass = this.getClass().getMethod(mName, parameters);
        if (retType.isAssignableFrom(inCurrentClass.getReturnType())) {
            return inCurrentClass.invoke(this, args);
        } else {
            throw new NoSuchMethodException("Donot know how to execute this method");
        }
        

    }

    @SuppressWarnings("unchecked")
	public <P extends Model> P getParent(Method parentGetter) {
    	Class<P> parentClass = (Class<P>) parentGetter.getReturnType();
    	
    	String parentIdFieldName =  StringUtil.underscorize(parentGetter.getName().substring(3) +"Id");
    	
    	Method parentIdGetter = this.reflector.getFieldGetter(parentIdFieldName);
    	
    	long parentId;
		try {
			parentId = Long.valueOf((Long)parentIdGetter.invoke(proxy));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
    	
    	
        P parent = Database.getInstance().getTable(parentClass).get(parentId);
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
    	Query q = new Query(childClass);
    	long parentId = proxy.getId();
    	return q.select().where(parentIdFieldName + " =  ? " , new BindVariable(parentId)).execute();
    }

    public void setProxy(M proxy) {
        this.proxy = proxy;
    }

    public M getProxy() {
		return proxy;
	}

	private static Map<Class<?>,Class<?>> modelImplMap = new HashMap<Class<?>, Class<?>>();
    
	@SuppressWarnings("unchecked")
	public static <M extends Model> M getProxy(Class<M> modelClass, Record record) {
		Class<?> modelImplClass = modelImplMap.get(modelClass)	;		
		if (modelImplClass == null){
			synchronized (modelImplMap) {
				modelImplClass = modelImplMap.get(modelClass);
				if (modelImplClass == null){
					String modelImplClassName = modelClass.getName()+"Impl";
					try { 
						modelImplClass = (Class<?>) Class.forName(modelImplClassName);
					}catch(ClassNotFoundException ex){
						modelImplClass = ModelImpl.class;
					}
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
    	

    public void save() {
        if (record.getDirtyFields().isEmpty()) {
            return;
        }
        beforeSave();
        if (record.isNewRecord()) {
            create();
        } else {
            update();
        }
        afterSave();

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
            ret = v.isValid(getter.getAnnotation(v.getAnnotationClass()), value, message) && ret;
        }
        return ret;

    }

    protected void beforeSave() {
        StringBuilder errmsg = new StringBuilder();
        if (!isModelValid(errmsg)) {
            throw new RuntimeException(errmsg.toString());
        }
    }

    protected void afterSave() {
    }

    public void destroy() {
        Query q = new Query();
        Table<M> table = Database.getInstance().getTable(modelClass);
        q.add("delete from ").add(table.getTableName()).add(" where id = ? and version = ? ", new BindVariable(proxy.getId()), new BindVariable(proxy.getVersion()));
        q.executeUpdate();
    }

    private void update() {
        long oldVersion = proxy.getVersion();
        long newVersion = oldVersion + 1;

        Query q = new Query();
        Table<M> table = Database.getInstance().getTable(modelClass);
        q.add("update ").add(table.getTableName()).add(" set  ");
        Iterator<String> fI = record.getDirtyFields().iterator();
        while (fI.hasNext()) {
            String field = fI.next();
            q.add(field).add(" = ? ", new BindVariable(record.get(field),
                    reflector.getColumnDescriptor(field).getJDBCType()));
            q.add(",");
        }
        q.add(" version = ? ", new BindVariable(newVersion));
        q.add(" ");
        q.add(" where id = ? ", new BindVariable(proxy.getId()));
        q.add(" and version = ? ", new BindVariable(oldVersion));

        q.executeUpdate();

        proxy.setVersion(newVersion);
        record.startTracking();
    }

    private void create() {
        proxy.setVersion(0);
        Query insertSQL = new Query();
        Table<M> table = Database.getInstance().getTable(modelClass);
        insertSQL.add("insert into ").add(table.getTableName()).add("(");

        Iterator<String> fieldIterator = record.getDirtyFields().iterator();
        StringBuilder values = new StringBuilder();
        while (fieldIterator.hasNext()) {
            String fieldName = fieldIterator.next();

            insertSQL.add(fieldName);
            values.append("?");
            if (fieldIterator.hasNext()) {
                insertSQL.add(",");
                values.append(",");
            }
            insertSQL.add(new BindVariable(record.get(fieldName),
                    reflector.getColumnDescriptor(fieldName).getJDBCType()));
        }

        insertSQL.add(" ) ").add("values (").add(values.toString()).add(")");
        Record generatedValues = new Record();
        String[] generatedKeys = table.getAutoIncrementColumns();
        assert (generatedKeys.length <= 1); // atmost one auto increment column
        
        insertSQL.executeUpdate(generatedValues, generatedKeys);
        if (generatedKeys.length == 1){
            assert (generatedValues.getDirtyFields().size() == 1);
            String fieldName = generatedKeys[0];
            String virtualFieldName = generatedValues.getDirtyFields().first();
            record.put(fieldName, generatedValues.get(virtualFieldName));
        }

        record.setNewRecord(false);
        record.startTracking();
    }
}
