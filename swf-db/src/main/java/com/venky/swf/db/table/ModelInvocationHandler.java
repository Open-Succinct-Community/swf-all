/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
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
import com.venky.swf.db.annotations.column.validations.processors.IntegerRangeValidator;
import com.venky.swf.db.annotations.column.validations.processors.MaxLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.MinLengthValidator;
import com.venky.swf.db.annotations.column.validations.processors.NotNullValidator;
import com.venky.swf.db.annotations.column.validations.processors.NumericRangeValidator;
import com.venky.swf.db.annotations.column.validations.processors.RegExValidator;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.validations.ModelValidator;
import com.venky.swf.db.annotations.model.validations.UniqueKeyValidator;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Delete;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Insert;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Update;
import com.venky.swf.sql.parser.SQLExpressionParser;

/**
 *
 * @author venky
 */
public class ModelInvocationHandler implements InvocationHandler {

    private Record record = null;
    private Class<? extends Model> modelClass = null;
    private List<String> virtualFields = new IgnoreCaseList(false);
    private String modelName = null;

    private transient Model proxy = null;
    private transient ModelReflector<? extends Model> reflector = null;

	@SuppressWarnings("unchecked")
	public <M extends Model> ModelReflector<M> getReflector() {
		if (reflector == null) { 
			reflector = ModelReflector.instance(modelClass);
		}
		return (ModelReflector<M>) reflector;
	}
	
	public String getModelName(){
		return modelName;
	}
	
	public String getPool(){
		return getReflector().getPool();
	}
	
	public Class<? extends Model> getModelClass(){ 
		return modelClass;
	}
	
	
	/**
	 * Used for serialization.:
	 */
	protected ModelInvocationHandler() {
		
	}
	public ModelInvocationHandler(Class<? extends Model> modelClass, Record record) {
        this.record = record;
        this.modelClass = modelClass;
        
        this.reflector = ModelReflector.instance(modelClass);
        this.modelName = Table.getSimpleModelClassName(reflector.getTableName());
        this.virtualFields = reflector.getVirtualFields();
        record.startTracking();
    }
	
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    	bootStrapProxy(getModelClass().cast(proxy));
        String mName = method.getName();
        Class<?> retType = method.getReturnType();
        Class<?>[] parameters = method.getParameterTypes();

        if (getReflector().getFieldGetterSignatures().contains(getReflector().getSignature(method))) {
            String fieldName = getReflector().getFieldName(method);
            if (!virtualFields.contains(fieldName)){
                ColumnDescriptor cd = getReflector().getColumnDescriptor(fieldName);
                String columnName = cd.getName();

                Object value = record.get(columnName);

				TypeRef<?> ref =Database.getJdbcTypeHelper(getPool()).getTypeRef(retType);
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
	        	CONNECTED_VIA join = getReflector().getAnnotation(method,CONNECTED_VIA.class);
	        	if (join != null){
	        		return getChildren(getReflector().getChildModelClass(method),join.value(),join.additional_join());
	        	}else {
	        		return getChildren(getReflector().getChildModelClass(method));
	        	}
        	}
        }
        
        /* Optimization 
        for (Object impl: modelImplObjects){
        	try {
	        	Method inModelImplClass = impl.getClass().getMethod(mName, parameters); 
	        	if (retType.isAssignableFrom(inModelImplClass.getReturnType())){
					Timer timer = startTimer(inModelImplClass.toString());
	        		try {
	        			return inModelImplClass.invoke(impl, args);
	        		}catch(InvocationTargetException ex){
	            		throw ex.getCause();
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
        	try {
        		return inCurrentClass.invoke(this, args);
        	}catch (InvocationTargetException ex){
        		throw ex.getCause();
        	}
        } else {
            throw new NoSuchMethodException("Donot know how to execute this method");
        }
        
        */
        Class<?> implClass = getMethodImplClass(method);
        Object implObject = null;
        if (implClass != null){
        	implObject = modelImplObjects.get(implClass);
        }
        if (implClass == null || implObject == null){ 
        	//implObject is null while constructing impls.
        	implClass = this.getClass();
        	implObject = this;
        }
    	Method inImplClass = implClass.getMethod(mName, parameters);
    	if (retType.isAssignableFrom(inImplClass.getReturnType())) {
	        Timer timer = cat.startTimer(inImplClass.toString());
	        try {
	        	return inImplClass.invoke(implObject, args);
	        }catch (InvocationTargetException ex){
	        	throw ex.getCause();
	        }finally{
	        	timer.stop();
	        }
    	}else {
    		throw new NoSuchMethodException("Donot know how to execute " + getReflector().getSignature(method));
    	}
    }
    private transient final SWFLogger cat = Config.instance().getLogger(getClass().getName()+"."+getModelName());

    @SuppressWarnings("unchecked")
	public <P extends Model> P getParent(Method parentGetter) {
    	Class<P> parentClass = (Class<P>) parentGetter.getReturnType();
    	
    	String parentIdFieldName =  StringUtil.underscorize(parentGetter.getName().substring(3) +"Id");
    	
    	Method parentIdGetter = this.getReflector().getFieldGetter(parentIdFieldName);
    	
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
    	Class<? extends Model> modelClass = getReflector().getModelClass();
    	ModelReflector<?> childReflector = ModelReflector.instance(childClass);
    	Expression expression = new Expression(childReflector.getPool(),Conjunction.OR);
    	for (String fieldName: childReflector.getFields()){
        	if (fieldName.endsWith("_ID")){
            	Method fieldGetter = childReflector.getFieldGetter(fieldName);
            	Method referredModelGetter = childReflector.getReferredModelGetterFor(fieldGetter);
            	if (referredModelGetter != null && ObjectUtil.equals(referredModelGetter.getReturnType().getSimpleName(),modelClass.getSimpleName())){
            		String columnName = childReflector.getColumnDescriptor(fieldName).getName();
            		expression.add(new Expression(childReflector.getPool(),columnName,Operator.EQ,proxy.getId()));
            	}
        	}
        }
        if (expression.isEmpty()){
        	throw new RuntimeException("Don;t know how to getChildren of kind " + childClass.getSimpleName() + " for " + modelClass.getSimpleName());
        }

    	return getChildren(childClass,expression);
    }
    public <C extends Model> List<C> getChildren(Class<C> childClass, String parentIdFieldName){
    	return getChildren(childClass,parentIdFieldName,null);
    }
    public <C extends Model> List<C> getChildren(Class<C> childClass, String parentIdFieldName, String addnl_condition){
    	int parentId = proxy.getId();
    	ModelReflector<C> childReflector = ModelReflector.instance(childClass);
    	String parentIdColumnName = childReflector.getColumnDescriptor(parentIdFieldName).getName();
    	Expression where = new Expression(getPool(),Conjunction.AND);
    	where.add(new Expression(getPool(),parentIdColumnName,Operator.EQ,new BindVariable(getPool(),parentId)));
    	if (!ObjectUtil.isVoid(addnl_condition)){
    		Expression addnl = new SQLExpressionParser(childClass).parse(addnl_condition);
        	where.add(addnl);
    	}
    	return getChildren(childClass, where);
    }
    
    public <C extends Model> List<C> getChildren(Class<C> childClass, Expression expression){
    	Select  q = new Select();
    	q.from(childClass);
    	q.where(expression);
    	q.orderBy(ModelReflector.instance(childClass).getOrderBy());
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
    public Set<String> getParticipatingRoles(User user){
    	return getParticipatingRoles(user,getReflector().getModelClass());
    }
    public Set<String> getParticipatingRoles(User user,Class<? extends Model> asModel){
    	if (!getReflector().reflects(asModel)){
    		throw new AccessDeniedException(); 
    	}
    	return getParticipatingRoles(user, user.getParticipationOptions(asModel));
    }
    private Set<String> getParticipatingRoles(User user,Cache<String,Map<String,List<Integer>>> pGroupOptions){
    	Timer timer = cat.startTimer();
    	try {
        	ModelReflector<? extends Model> reflector = getReflector();
        	Set<String> participantingRoles = new HashSet<String>();
    		for (String participantRoleGroup : pGroupOptions.keySet()){
    			Map<String,List<Integer>> pOptions = pGroupOptions.get(participantRoleGroup);
    			for (String referencedModelIdFieldName :pOptions.keySet()){
    				Integer referenceValue = reflector.get(getRawRecord(),referencedModelIdFieldName);	
    				
    				if (pOptions.get(referencedModelIdFieldName).contains(referenceValue)){
    					participantingRoles.add(reflector.getParticipatingRole(referencedModelIdFieldName));
    				}
    			}
    			if (!pOptions.isEmpty() && participantingRoles.isEmpty()){
    				throw new AccessDeniedException(); // User is not a participant on the model.
    			}
    		}
    		return participantingRoles;
    	}finally{
    		timer.stop();
    	}
    }
    public boolean isAccessibleBy(User user,Class<? extends Model> asModel){
    	Timer timer = cat.startTimer(null,Config.instance().isTimerAdditive());
    	try {
	    	if (!getReflector().reflects(asModel)){
	    		return false;
	    	}
	    	Set<String> pRoles = getParticipatingRoles(user,asModel);
	    	return (pRoles != null);// It is always true. returning false depends on AccessDeniedException being thrown.
    	}catch(AccessDeniedException ex){
    		return false;
    	}finally{
    		timer.stop();
    	}
    }
    
    public Record getRawRecord(){
    	return record;
    }

	public static void dispose(){
		modelImplClassesCache.clear();
		methodImplClassCache.clear();
	}
    
	public static <M extends Model> M getProxy(Class<M> modelClass, Record record) {
		ModelReflector<M> ref = ModelReflector.instance(modelClass);
		
		
		try {
	    	ModelInvocationHandler mImpl = new ModelInvocationHandler(modelClass, record);
	    	M m = modelClass.cast(Proxy.newProxyInstance(modelClass.getClassLoader(), ref.getClassHierarchies().toArray(new Class<?>[]{}), mImpl));
	    	mImpl.bootStrapProxy(m);
	    	return m;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private <M extends Model> void bootStrapProxy(M m) {
		if (proxy == null) {
	    	setProxy(m);
	        List<Class<?>> modelImplClasses = getModelImplClasses(modelClass);		
	    	for (Class<?> implClass: modelImplClasses){
	    		addModelImplObject(constructImpl(implClass, m));
	    	}
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
	private transient SequenceMap<Class<?>,Object> modelImplObjects = new SequenceMap<Class<?>,Object>();
	private void addModelImplObject(Object o){
		modelImplObjects.put(o.getClass(),o);
	}
	
	private Class<?> getMethodImplClass(Method m){
		return methodImplClassCache.get(getReflector().getModelClass()).get(m);
	}
	
	private static Cache<Class<? extends Model>,Cache<Method,Class<?>>> methodImplClassCache = new Cache<Class<? extends Model>, Cache<Method,Class<?>>>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8303755398345923039L;

		@Override
		protected Cache<Method, Class<?>> getValue(final Class<? extends Model> modelClass) {
			
			return new Cache<Method, Class<?>>() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1322249489351360016L;

				@Override
				protected Class<?> getValue(Method method) {
			        String mName = method.getName();
			        Class<?> retType = method.getReturnType();
			        Class<?>[] parameters = method.getParameterTypes();

					for (Class<?> implClass: getModelImplClasses(modelClass)){
			        	try {
				        	Method inModelImplClass = implClass.getMethod(mName, parameters); 
				        	if (retType.isAssignableFrom(inModelImplClass.getReturnType())){
				        		return implClass;
				        	}
			        	}catch (NoSuchMethodException ex){
			        		//
			        	}   	
					}
					return null;
				}
			};
		}
	}; 
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
		save(true);
	}
    public void save(boolean validate) {
        if (!isDirty()) {
            return;
        }
        if (validate){
        	validate();
        }
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
    private static final Cache<String,List<FieldValidator<? extends Annotation>>> _fieldValidators = new Cache<String, List<FieldValidator<? extends Annotation>>>() {
        /**
		 * 
		 */
		private static final long serialVersionUID = -8174150221673158116L;

		@Override
        protected List<FieldValidator<? extends Annotation>> getValue(String pool) {
            List<FieldValidator<? extends Annotation>> fieldValidators = new ArrayList<FieldValidator<? extends Annotation>>();
            fieldValidators.add(new ExactLengthValidator(pool));
            fieldValidators.add(new MaxLengthValidator(pool));
            fieldValidators.add(new MinLengthValidator(pool));
            fieldValidators.add(new NotNullValidator(pool));
            fieldValidators.add(new RegExValidator(pool));
            fieldValidators.add(new EnumerationValidator(pool));
            fieldValidators.add(new DateFormatValidator(pool));
            fieldValidators.add(new NumericRangeValidator(pool));
            fieldValidators.add(new IntegerRangeValidator(pool));
            return fieldValidators;
        }
    };

    private static final List<ModelValidator> modelValidators = new ArrayList<ModelValidator>();
    static{
        modelValidators.add(new UniqueKeyValidator());
    }
    
    protected boolean isModelValid(MultiException ex) {
        List<String> fields = getReflector().getEditableFields();
        boolean ret = true;
        for (String field : fields) {
        	MultiException fieldException = new MultiException();
            if (!getReflector().isHouseKeepingField(field) && !isFieldValid(field,fieldException)) {
                ex.add(fieldException);
                ret = false;
            }
        }
        if (ret){
        	for (ModelValidator v : modelValidators){
        		ret = v.isValid(getProxy(),ex) && ret;
        	}
        }
        return ret;
    }

    protected boolean isFieldValid(String field, MultiException fieldException) {
        boolean ret = true;
        Iterator<FieldValidator<? extends Annotation>> i = _fieldValidators.get(getPool()).iterator();
        while (i.hasNext()) {
            FieldValidator<? extends Annotation> v = i.next();
            ret = v.isValid(getProxy(), field, fieldException) && ret;
        }
        return ret;

    }

    protected void validate(){
    	beforeValidate();
    	MultiException me = new MultiException();
        if (!isModelValid(me)) {
            throw me;
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
    	for (String extnPoint: getExtensionPoints(getReflector().getModelClass(), extnPointNameSuffix)){
    		Registry.instance().callExtensions(extnPoint, getProxy());
    	}
    }
    
    
    protected void beforeValidate(){
    	defaultFields();
    	callExtensions("before.validate");
    }
    public void defaultFields(){
        if (!record.isNewRecord()){
        	ColumnDescriptor updatedAt = getReflector().getColumnDescriptor("updated_at");
        	ColumnDescriptor updatorUser = getReflector().getColumnDescriptor("updater_user_id");
        	if (!updatedAt.isVirtual()){
        		proxy.setUpdatedAt(null);
        	}
        		
        	if (!updatorUser.isVirtual()){
        		proxy.setUpdaterUserId(null);
        	}
        }
        ModelReflector<? extends Model> reflector = getReflector();
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
					if (childReflector.getRealModelClass() == null){
						continue;
					}
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
	        Expression condition = new Expression(getPool(),Conjunction.AND);
	        condition.add(new Expression(getPool(),getReflector().getColumnDescriptor("id").getName(),Operator.EQ,new BindVariable(getPool(),proxy.getId())));
	        condition.add(new Expression(getPool(),getReflector().getColumnDescriptor("lock_id").getName(),Operator.EQ,new BindVariable(getPool(),proxy.getLockId())));
	        q.where(condition);
	        if (q.executeUpdate() <= 0){
	        	throw new RecordNotFoundException();
	        }
	        
			Database.getInstance().getCache(getReflector()).registerDestroy((Model)getProxy());
			Database.getInstance().getCurrentTransaction().registerTableDataChanged(getReflector().getTableName());
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
            TypeRef<?> ref = Database.getJdbcTypeHelper(getPool()).getTypeRef(getReflector().getFieldGetter(fieldName).getReturnType());
            q.set(columnName,new BindVariable(getPool(),record.get(columnName), ref));
        }
        
        String idColumn = getReflector().getColumnDescriptor("id").getName();
        Expression condition = new Expression(getPool(),Conjunction.AND);
        condition.add(new Expression(getPool(),idColumn,Operator.EQ,new BindVariable(getPool(),proxy.getId())));

        ColumnDescriptor lockIdColumDescriptor = getReflector().getColumnDescriptor("lock_id");
        if (!lockIdColumDescriptor.isVirtual()){
            String lockidColumn = lockIdColumDescriptor.getName();
            q.set(lockidColumn,new BindVariable(getPool(),newLockId));
            condition.add(new Expression(getPool(),lockidColumn,Operator.EQ,new BindVariable(getPool(),oldLockId)));
        }
        

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

		Database.getInstance().getCache(getReflector()).registerUpdate((Model)getProxy());
		Database.getInstance().getCurrentTransaction().registerTableDataChanged(getReflector().getTableName());
    }

    private void create() {
    	proxy.setLockId(0);
		//Table<? extends Model> table = Database.getTable(getReflector().getTableName());
        Insert insertSQL = new Insert(getReflector());
        Map<String,BindVariable> values = new HashMap<String, BindVariable>();
        
        Iterator<String> columnIterator = record.getDirtyFields().iterator();
        while (columnIterator.hasNext()) {
            String columnName = columnIterator.next();
            String fieldName =  getReflector().getFieldName(columnName);
            if (fieldName == null){
            	continue;
            }
            TypeRef<?> ref = Database.getJdbcTypeHelper(getPool()).getTypeRef(getReflector().getFieldGetter(fieldName).getReturnType());
            values.put(columnName,new BindVariable(getPool(),record.get(columnName), ref));
        }
        insertSQL.values(values);
        
        
        Record generatedValues = new Record(getPool());
        Set<String> autoIncrementColumns = getReflector().getAutoIncrementColumns();
        assert (autoIncrementColumns.size() <= 1); // atmost one auto increment id column
        List<String> generatedKeys = new ArrayList<String>();
        
        for (String anAutoIncrementColumn:autoIncrementColumns){
			if ( Database.getJdbcTypeHelper(getPool()).isColumnNameAutoLowerCasedInDB() ){
        		generatedKeys.add(LowerCaseStringCache.instance().get(anAutoIncrementColumn));
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
        
    	Database.getInstance().getCache(getReflector()).registerInsert((Model)getProxy());
    	Database.getInstance().getCurrentTransaction().registerTableDataChanged(getReflector().getTableName());
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
 
    private transient Map<String,Object> txnProperties = new HashMap<String, Object>();
    public Object getTxnProperty(String name) { 
    	return txnProperties.get(name);
    }
    public void setTxnPropery(String name,Object value) {
    	txnProperties.put(name, value);
    }
    public Object removeTxnProperty(String name) { 
    	return txnProperties.remove(name);
    }

    public boolean isDirty(){
    	return !getProxy().getRawRecord().getDirtyFields().isEmpty();
    }
    
}
