/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefaulter;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.validations.processors.DateFormatValidator;
import com.venky.swf.db.annotations.column.validations.processors.EnumerationValidator;
import com.venky.swf.db.annotations.column.validations.processors.ExactLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.FieldValidator;
import com.venky.swf.db.annotations.column.validations.processors.MaxLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.NotNullValidator;
import com.venky.swf.db.annotations.column.validations.processors.RegExValidator;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;
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
public class ModelInvocationHandler implements InvocationHandler {

    private Record record = null;
    private Model proxy = null;
    private ModelReflector<? extends Model> reflector = null;
    private List<String> virtualFields = new IgnoreCaseList();
    private String modelName = null;

	public ModelReflector<? extends Model> getReflector() {
		return reflector;
	}
	
	public String getModelName(){
		return modelName;
	}
	

	public ModelInvocationHandler(Class<? extends Model> modelClass, Record record) {
        this.record = record;
        this.reflector = ModelReflector.instance(modelClass);
        this.modelName = Table.getSimpleModelClassName(reflector.getTableName());
        this.virtualFields = reflector.getVirtualFields();
        record.startTracking();
    }
	
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Not Required. setProxy(getModelClass().cast(proxy));
        String mName = method.getName();
        Class<?> retType = method.getReturnType();
        Class<?>[] parameters = method.getParameterTypes();

        if (getReflector().getFieldGetterSignatures().contains(getReflector().getSignature(method))) {
            String fieldName = getReflector().getFieldName(method);
            if (!virtualFields.contains(fieldName)){
                ColumnDescriptor cd = getReflector().getColumnDescriptor(method);
                String columnName = cd.getName();

                Object value = record.get(columnName);

				TypeRef<?> ref =Database.getJdbcTypeHelper().getTypeRef(retType);
                TypeConverter<?> converter = ref.getTypeConverter();
                if (value == null) {
                	Object defaultValue = null;
                	COLUMN_DEF colDef = getReflector().getAnnotation(method,COLUMN_DEF.class);
                	if (colDef != null){
                		defaultValue = StandardDefaulter.getDefaultValue(colDef.value(),colDef.args());
                	}
                	if (retType.isPrimitive()){
                		return converter.valueOf(defaultValue);
                	}else {
                		return defaultValue;
                	}
                } else if (retType.isInstance(value) && !ref.isLOB()) {
                    return value;
                } else {
                    return converter.valueOf(value);
                }
            }
        } else if (getReflector().getFieldSetters().contains(method) ) {
            String fieldName = StringUtil.underscorize(mName.substring(3));
            if (!virtualFields.contains(fieldName)){
                String columnName = getReflector().getColumnDescriptor(fieldName).getName(); 
                return record.put(columnName, args[0]);
            }
        } else if (getReflector().getReferredModelGetters().contains(method)) {
        	if (!getReflector().isAnnotationPresent(method,IS_VIRTUAL.class)){
                return getParent(method);
        	}
        } else if (getReflector().getChildGetters().contains(method)) {
        	if (!getReflector().isAnnotationPresent(method,IS_VIRTUAL.class)){
	        	CONNECTED_VIA join = reflector.getAnnotation(method,CONNECTED_VIA.class);
	        	if (join != null){
	        		return getChildren(getReflector().getChildModelClass(method),join.value());
	        	}else {
	        		return getChildren(getReflector().getChildModelClass(method));
	        	}
        	}
        }
        for (Object impl: modelImplObjects){
        	try {
	        	Method inModelImplClass = impl.getClass().getMethod(mName, parameters); 
	        	if (retType.isAssignableFrom(inModelImplClass.getReturnType())){
					Timer timer = Timer.startTimer(inModelImplClass.toString());
	        		try {
	        			return inModelImplClass.invoke(impl, args);
	        		}finally{
	        			timer.stop();
	        		}
	        	}
        	}catch(NoSuchMethodException ex){
        		//	
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
    	
    	Integer parentId;
		try {
			parentId = (Integer)parentIdGetter.invoke(proxy);
		} catch (Exception e) {
			throw new RuntimeException(parentIdFieldName,e);
		} 
		P parent = null;
		if (parentId != null) {
			parent = Database.getTable(parentClass).get(parentId);
		}
        return parent;
    }
    
    public <C extends Model> List<C> getChildren(Class<C> childClass){
    	List<C> children = new ArrayList<C>();
    	
    	ModelReflector<?> childReflector = ModelReflector.instance(childClass);
        for (String fieldName: childReflector.getFields()){
            if (fieldName.endsWith(StringUtil.underscorize(getModelName() +"Id"))){
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
    	return q.execute(childClass);
    }

    public <M extends Model> void setProxy(M proxy) {
        this.proxy = proxy;
    }

    @SuppressWarnings("unchecked")
	public <M extends Model> M getProxy() {
		return (M)proxy;
	}
    
    public boolean isAccessibleBy(User user){
		return isAccessibleBy(user, getReflector().getModelClass());
    }
    public boolean isAccessibleBy(User user,Class<? extends Model> asModel){
    	Timer timer = Timer.startTimer();
    	try {
	    	if (!getReflector().reflects(asModel)){
	    		return false;
	    	}
	    	Map<String,List<Integer>> fieldNameValues = user.getParticipationOptions(asModel);
	    	if (fieldNameValues.isEmpty()){
	    		return true;
	    	}	
	    	for (String fieldName:fieldNameValues.keySet()){
	    		List<Integer> values = fieldNameValues.get(fieldName);
	    		Object value = reflector.get(getProxy(), fieldName);
	    		if (values.contains(value)) {
	    			return true;
	    		}
	    	}
	    	return false;
    	}finally{
    		timer.stop();
    	}
    }
    
    public Record getRawRecord(){
    	return record;
    }

	private static Map<Class<? extends Model>,List<Class<?>>> modelImplsMap = new HashMap<Class<? extends Model>, List<Class<?>>>();
    
	public static <M extends Model> M getProxy(Class<M> modelClass, Record record) {
		ModelReflector<M> ref = ModelReflector.instance(modelClass);
		
		List<Class<?>> modelImplClasses = modelImplsMap.get(modelClass)	;		
		if (modelImplClasses == null){
			synchronized (modelImplsMap) {
				modelImplClasses = modelImplsMap.get(modelClass);
				if (modelImplClasses == null){
					modelImplClasses = getModelImplClasses(modelClass);
					modelImplsMap.put(modelClass, modelImplClasses);
				}
			}
		}
		
		try {
	    	ModelInvocationHandler mImpl = new ModelInvocationHandler(modelClass, record);
	    	M m = modelClass.cast(Proxy.newProxyInstance(modelClass.getClassLoader(), ref.getClassHierarchies().toArray(new Class<?>[]{}), mImpl));
	    	mImpl.setProxy(m);
	    	for (Class<?> implClass: modelImplClasses){
	    		mImpl.addModelImplObject(constructImpl(implClass, m));
	    	}
	    	return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@SuppressWarnings("unchecked")
	private static <M extends Model> Object constructImpl(Class<?> implClass, M m){
		if (ModelImpl.class.isAssignableFrom(implClass)){
			if (ModelImpl.class.equals(implClass)) {
				return new ModelImpl<M>(m);
			}else {
				ParameterizedType pt = (ParameterizedType)implClass.getGenericSuperclass();
				Class<? extends Model> modelClass = (Class<? extends Model>) pt.getActualTypeArguments()[0];
				try {
					return implClass.getConstructor(modelClass).newInstance(m);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		throw new RuntimeException("Don't know how to instantiate " + implClass.getName());
	}
	private List<Object> modelImplObjects = new ArrayList<Object>();
	private void addModelImplObject(Object o){
		modelImplObjects.add(o);
	}
	
	private static Cache<Class<? extends Model>,List<Class<?>>> modelImplClassesCache = new Cache<Class<? extends Model>, List<Class<?>>>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7544606584634901930L;

		@Override
		protected List<Class<?>> getValue(Class<? extends Model> modelClass) {
			SequenceSet<Class<? extends Model>> modelClasses = ModelReflector.instance(modelClass).getClassHierarchies();
			List<Class<?>> modelImplClasses = new ArrayList<Class<?>>();
			
			for (Class<?> c : modelClasses){
				String modelImplClassName = c.getName()+"Impl";
				try { 
					Class<?> modelImplClass = Class.forName(modelImplClassName);
					if (ModelImpl.class.isAssignableFrom(modelImplClass)){
						modelImplClasses.add(modelImplClass);
					}else {
						throw new ClassCastException(modelImplClassName + " does not extend " + ModelImpl.class.getName());
					}
				}catch(ClassNotFoundException ex){
					// Nothing
				}
			}
			return modelImplClasses;
		}
	};
	private static <M extends Model> List<Class<?>> getModelImplClasses(Class<M> modelClass){
		return modelImplClassesCache.get(modelClass);
	}

    public void save() {
        if (record.getDirtyFields().isEmpty()) {
            return;
        }
        validate();
        beforeSave();
        if (record.isNewRecord()) {
        	callExtensions("before.create");
            create();
        	callExtensions("after.create");
        } else {
        	callExtensions("before.update");
            update();
            callExtensions("after.update");
        }
        afterSave();

    }
    public void init(){
    	
    }
    private final static List<FieldValidator<? extends Annotation>> validators = new ArrayList<FieldValidator<? extends Annotation>>();

    static {
        validators.add(new ExactLengthValidator());
        validators.add(new MaxLengthValidator());
        validators.add(new NotNullValidator());
        validators.add(new RegExValidator());
        validators.add(new EnumerationValidator());
        validators.add(new DateFormatValidator());
    }

    protected boolean isModelValid(StringBuilder totalMessage) {
        List<String> fields = reflector.getFields();
        totalMessage.append(getModelName()).append(":<br/>");
        boolean ret = true;
        for (String field : fields) {
            StringBuilder message = new StringBuilder();
            Object value = record.get(field);
            Method getter = reflector.getFieldGetter(field);
            
            if (!reflector.isHouseKeepingField(field) && !isFieldValid(getter, value, message)) {
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
    
    private <R extends Model> SequenceSet<String> getExtensionPoints(Class<R> modelClass, String extnPointNameSuffix){
    	SequenceSet<String> extnPoints = new SequenceSet<String>();
		ModelReflector<R> ref = ModelReflector.instance(modelClass);
		for (Class<? extends Model> inHierarchy : ref.getClassHierarchies()){
			String extnPoint = inHierarchy.getSimpleName() + "." + extnPointNameSuffix;
			extnPoints.add(extnPoint);
		}
		return extnPoints;
	}
    
    private <R extends Model> void callExtensions(String extnPointNameSuffix){
    	for (String extnPoint: getExtensionPoints(reflector.getModelClass(), extnPointNameSuffix)){
    		Registry.instance().callExtensions(extnPoint, getProxy());
    	}
    }
    
    
    protected void beforeValidate(){
    	defaultFields();
    	callExtensions("before.validate");
    }
    protected void defaultFields(){
        if (!record.isNewRecord()){
        	proxy.setUpdatedAt(null);
        	proxy.setUpdaterUserId(null);
        }
        for (String field:reflector.getRealFields()){
        	String columnName = reflector.getColumnDescriptor(field).getName();
        	if (record.get(columnName) == null){
        		Method fieldGetter = reflector.getFieldGetter(field);
        		COLUMN_DEF cdef = reflector.getAnnotation(fieldGetter,COLUMN_DEF.class);
        		if (cdef != null){
        			Object defaultValue = StandardDefaulter.getDefaultValue(cdef.value(),cdef.args());
        			record.put(columnName,defaultValue);
        		}
        	}
        }
    }

    protected void afterValidate(){
    	callExtensions("after.validate");
    }
    
    protected void beforeSave() {
    	callExtensions("before.save");
    }
    protected void afterSave() {
    	callExtensions("after.save");
    }
    protected void beforeDestory(){
    	callExtensions("before.destroy");
    }
    protected void afterDestroy(){
    	callExtensions("after.destroy");
    }
    public boolean isBeingDestroyed(){
    	return beingDestroyed;
    }
    private boolean beingDestroyed = false;
    private void destroyCascade(){
        ModelReflector<? extends Model> ref = getReflector();
        for (Method childrenGetter : ref.getChildGetters()){
        	Class<? extends Model> childModelClass = ref.getChildModelClass(childrenGetter);
        	ModelReflector<? extends Model> childReflector = ModelReflector.instance(childModelClass);
        	List<String> referenceFields = childReflector.getReferenceFields(ref.getModelClass());
        	
        	for (String referenceField: referenceFields){
				try {
					@SuppressWarnings("unchecked")
					List<Model> children = (List<Model>)childrenGetter.invoke(getProxy());
					for (Model child : children){
		        		if (childReflector.isFieldMandatory(referenceField)){
		        			child.destroy();
		        		}else {
		        			childReflector.set(child,referenceField,null);
		        			child.save();
		        		}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
        	}
        }
    }
    public void destroy() {
    	if (isBeingDestroyed()){
    		return;
    	}
    	try {
    		beingDestroyed = true;
    		beforeDestory();
	    	destroyCascade();
	
	    	Delete q = new Delete(getReflector());
	        Expression condition = new Expression(Conjunction.AND);
	        condition.add(new Expression(getReflector().getColumnDescriptor("id").getName(),Operator.EQ,new BindVariable(proxy.getId())));
	        condition.add(new Expression(getReflector().getColumnDescriptor("lock_id").getName(),Operator.EQ,new BindVariable(proxy.getLockId())));
	        q.where(condition);
	        if (q.executeUpdate() <= 0){
	        	throw new RecordNotFoundException();
	        }
	        
			Database.getInstance().getCache(getReflector()).registerDestroy(getProxy());
	        afterDestroy();
		}finally{
			beingDestroyed = false;
		}
    }

    private void update() {
        int oldLockId = proxy.getLockId();
        int newLockId = oldLockId + 1;

        Update q = new Update(getReflector());
        Iterator<String> fI = record.getDirtyFields().iterator();
        while (fI.hasNext()) {
            String columnName = fI.next();
            String fieldName =  getReflector().getFieldName(columnName);
            TypeRef<?> ref = Database.getJdbcTypeHelper().getTypeRef(getReflector().getFieldGetter(fieldName).getReturnType());
            q.set(columnName,new BindVariable(record.get(columnName), ref));
        }
        
        String idColumn = getReflector().getColumnDescriptor("id").getName();
        String lockidColumn = getReflector().getColumnDescriptor("lock_id").getName();
        q.set(lockidColumn,new BindVariable(newLockId));
        
        Expression condition = new Expression(Conjunction.AND);
        condition.add(new Expression(idColumn,Operator.EQ,new BindVariable(proxy.getId())));
        condition.add(new Expression(lockidColumn,Operator.EQ,new BindVariable(oldLockId)));

        q.where(condition);
        
        if (q.executeUpdate() <= 0){
        	throw new RecordNotFoundException();
        }
        proxy.setLockId(newLockId);
        record.startTracking();
        if (!getReflector().isAnnotationPresent(CONFIGURATION.class)){
        	record.setLocked(true);
        	//Do only for transaction tables as config cache would need to be reset to false after commit. This is just to avoid that unwanted loop over config records cached.
        }

		Database.getInstance().getCache(getReflector()).registerUpdate(getProxy());
    }

    private void create() {
    	proxy.setLockId(0);
		Table<? extends Model> table = Database.getTable(getReflector().getTableName());
        Insert insertSQL = new Insert(getReflector());
        Map<String,BindVariable> values = new HashMap<String, BindVariable>();
        
        Iterator<String> columnIterator = record.getDirtyFields().iterator();
        while (columnIterator.hasNext()) {
            String columnName = columnIterator.next();
            String fieldName =  getReflector().getFieldName(columnName);
            TypeRef<?> ref = Database.getJdbcTypeHelper().getTypeRef(getReflector().getFieldGetter(fieldName).getReturnType());
            values.put(columnName,new BindVariable(record.get(columnName), ref));
        }
        insertSQL.values(values);
        
        
        Record generatedValues = new Record();
        Set<String> autoIncrementColumns = table.getAutoIncrementColumns();
        assert (autoIncrementColumns.size() <= 1); // atmost one auto increment id column
        List<String> generatedKeys = new ArrayList<String>();
        
        for (String anAutoIncrementColumn:autoIncrementColumns){
			if ( Database.getJdbcTypeHelper().isColumnNameAutoLowerCasedInDB() ){
        		generatedKeys.add(anAutoIncrementColumn.toLowerCase());
        	}else {
        		generatedKeys.add(anAutoIncrementColumn);
        	}
        }
        
        insertSQL.executeUpdate(generatedValues, generatedKeys.toArray(new String[]{}));
        
        if (generatedKeys.size() == 1){
            assert (generatedValues.getDirtyFields().size() == 1);
            String fieldName = generatedKeys.get(0);
            String virtualFieldName = generatedValues.getDirtyFields().iterator().next();
            int id = ((Number)generatedValues.get(virtualFieldName)).intValue();
            record.put(fieldName, id);
        }

        record.setNewRecord(false);
        record.startTracking();
        if (!getReflector().isAnnotationPresent(CONFIGURATION.class)){
        	record.setLocked(true);
        }
        
    	Database.getInstance().getCache(getReflector()).registerInsert(getProxy());
    	
	}
    @Override
    public boolean equals(Object o){
    	if (o == null){
    		return false;
    	}
    	if (!(o instanceof ModelInvocationHandler) && !getReflector().canReflect(o)){
    		return false;
    	}
    	if (o instanceof ModelInvocationHandler){
    		return equalImpl((ModelInvocationHandler)o);
    	}else {
    		return equalsProxy((Model)o);
    	}
    }
    
    public int hashCode(){
    	return (getModelName() + ":" + getProxy().getId()).hashCode() ;
    }
    protected boolean equalImpl(ModelInvocationHandler anotherImpl){
    	return (getProxy().getId() == anotherImpl.getProxy().getId()) && getReflector().getTableName().equals(anotherImpl.getReflector().getTableName()); 
    }
    
    protected boolean equalsProxy(Model anotherProxy){
    	boolean ret = false;
    	if (anotherProxy != null){
    		ret = getProxy().getId() == anotherProxy.getId();
    	}
    	return ret;
    }
    
    @SuppressWarnings("unchecked")
	public <M extends Model> M cloneProxy(){
		return (M)getRawRecord().clone().getAsProxy(getReflector().getModelClass());
    }
 
    private Map<String,Object> txnProperties = new HashMap<String, Object>();
    public Object getTxnProperty(String name) { 
    	return txnProperties.get(name);
    }
    public void setTxnPropery(String name,Object value) {
    	txnProperties.put(name, value);
    }
    public Object removeTxnProperty(String name) { 
    	return txnProperties.remove(name);
    }
 
    public String uniqueDescription(){
    	Collection<SequenceSet<String>> uniqueKeys = getReflector().getUniqueKeys().values();
    	
    	Expression where = new Expression(Conjunction.AND);
    	if (uniqueKeys.isEmpty()){
    		where.add(new Expression("ID",Operator.EQ,getProxy().getId()));
    	}else {
	    	Set<String> firstKey = uniqueKeys.iterator().next();
	    	if (getReflector().getSingleColumnUniqueKeys().size() == 1){
	    		firstKey = getReflector().getSingleColumnUniqueKeys().iterator().next();
	    	}
	    	if (firstKey.size() == 1){
	    		return StringUtil.valueOf(getReflector().get(getProxy(), firstKey.iterator().next()));
	    	}
	    	
	    	for (Iterator<String> i = firstKey.iterator() ; i.hasNext(); ){
	    		String field = i.next();
	    		String column = getReflector().getColumnDescriptor(field).getName();
	    		
	    		Object value = getReflector().get(getProxy(), field);
	    		if ( value != null) {
	        		where.add(new Expression(column,Operator.EQ,value));
	    		}else {
	    			where.add(new Expression(column,Operator.EQ));
	    		}
	    	}
    	}
    	return where.getRealSQL();
    }
    
    
    
    
}
