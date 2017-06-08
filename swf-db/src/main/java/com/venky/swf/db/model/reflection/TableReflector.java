/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.model.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.reflection.MethodSignatureCache;
import com.venky.reflection.Reflector;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.model.DBPOOL;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;

/**
 *
 * @author venky
 */
public class TableReflector {
	
    private static final Map<String , TableReflector>  tableReflectorByTableName = new HashMap<String, TableReflector>();
    public static void dispose(){
    	tableReflectorByTableName.clear();
    	MReflector.dispose();
    }
    
    public static <M extends Model> TableReflector instance(Class<M> modelClass){
    	Class<? extends Model> realModelClass = getRealModelClass(modelClass);
        if (realModelClass == null){
    		//The whole tree is Virtual. 
        	realModelClass = modelClass;
    	}
    	
    	String tableName = Table.tableName(realModelClass);
    	DBPOOL dbpool = realModelClass.getAnnotation(DBPOOL.class);
    	String pool = dbpool == null ? "" : dbpool.value();
    	if (ObjectUtil.isVoid(pool)){
    		pool = ConnectionManager.instance().getDefaultPool();
    	}
    	String tableKey = pool + "." + tableName;
    	
    	TableReflector reflector = tableReflectorByTableName.get(tableKey);
        if (reflector == null){
	        synchronized(tableReflectorByTableName){
	            reflector = tableReflectorByTableName.get(tableKey);
	            if (reflector == null){
	                reflector = new TableReflector(tableName,pool);
	                tableReflectorByTableName.put(tableKey, reflector);
	            }
	        }
        }
        reflector.registerModelClass(modelClass);
        return reflector;
    }

    private SequenceSet<Class<? extends Model>> modelClassesInClasspathSequence  = new SequenceSet<Class<? extends Model>>();
    private Map<String,SequenceSet<Class<? extends Model>>> modelClassesInClasspathSequenceByName = new HashMap<String, SequenceSet<Class<? extends Model>>>();
    
    public <M extends Model> void  registerModelClass(Class<M> modelClass){
    	if (!modelClassesInClasspathSequence.contains(modelClass)){
    		modelClassesInClasspathSequence.add(modelClass);
    		SequenceSet<Class<? extends Model>> modelClassesForName = modelClassesInClasspathSequenceByName.get(modelClass.getSimpleName());
    		if (modelClassesForName == null){
    			modelClassesForName = new SequenceSet<Class<? extends Model>>();
    			modelClassesInClasspathSequenceByName.put(modelClass.getSimpleName(), modelClassesForName);
    		}
    		modelClassesForName.add(modelClass);
    	}
    }
    
    public SequenceSet<Class<? extends Model>> getModelClasses(){
    	return modelClassesInClasspathSequence;
    }
    
    /**
     * @param modelClass
     * @return modelClasses that have the same SimpleName as the the passed modelClass in the order in which they occur in classpath. 
     * If multiple classes with same simple name are present in a classpath entry (jar or a directory) the sequence between them is not guaranteed.
     */
    public SequenceSet<Class<? extends Model>> getSiblingModelClasses(Class<? extends Model> modelClass){
    	if (modelClass == null){
    		return modelClassesInClasspathSequence;
    	}
    	return modelClassesInClasspathSequenceByName.get(modelClass.getSimpleName());
    }
    
    @SuppressWarnings("unchecked")
	public static <U extends Model> Class<U> getRealModelClass(Class<? extends Model> modelClass){
    	MReflector<? extends Model> ref = MReflector.instance(modelClass);
    	Class<? extends Model> lastRealClass = null;
    	List<Class<? extends Model>> modelHierarchyClasses = ref.getClassHierarchy(); 
    	for (Class<? extends Model> claz:modelHierarchyClasses){
    		IS_VIRTUAL isVirtual = claz.getAnnotation(IS_VIRTUAL.class);
    		if (isVirtual != null){
    			if (isVirtual.value()){
    				lastRealClass = null;
    			}else if (lastRealClass == null){
    				lastRealClass = claz;
    				break;
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
    
    private final String tableName;
    private final String pool ; 
    
    private TableReflector(String tableName, String pool) {
    	this.tableName = tableName;
    	this.pool = pool;
    }
    public String getPool(){
    	return pool;
    }
	public String getTableName(){
    	return tableName;
    }
    
    public boolean reflects(Class<? extends Model> other){
    	return modelClassesInClasspathSequence.contains(other);
    }
    
    public boolean canReflect(Object o){
    	for (Class<? extends Model> model: modelClassesInClasspathSequence){
    		if (model.isInstance(o)){
    			return true;
    		}
    	}
    	return false;
    }

    
    public void loadMethods(Class<? extends Model> inModel , List<Method> into , MethodMatcher matcher ){
    	if (!into.isEmpty()){
    		return;
    	}
    	synchronized (into) {
    		if (into.isEmpty()){
    			HashSet<String> signatures = new HashSet<String>(); 
    			for (Class<? extends Model> modelClass:getSiblingModelClasses(inModel)){
    				List<Method> matchingMethods = MReflector.instance(modelClass).getMethods(matcher);
    				for (Method m: matchingMethods){
    					String signature = getSignature(m);
    					if (!signatures.contains(signature)){
    						into.add(m);
    						signatures.add(signature);
    					}
    				}
				}
    		}		
		}
    }
    private MethodSignatureCache signatureCache = new MethodSignatureCache();
    public String getSignature(Method method){
    	return signatureCache.get(method);
    }

    public boolean isAnnotationPresent(Class<? extends Model> inModel, Class<? extends Annotation> annotationClass){
    	return getAnnotation(inModel,annotationClass) != null;
    }
    
    public <A extends Annotation> A getAnnotation(Class<? extends Model> inModel, Class<A> annotationClass){
    	A a = null; 
    	for (Class<? extends Model> sibling: getSiblingModelClasses(inModel)){
        	MReflector<? extends Model> ref = MReflector.instance(sibling);
        	a =  ref.getAnnotation(annotationClass);
        	if (a != null){
        		break;
        	}        	
    	}
    	return a;
    }

    public boolean isAnnotationPresent(Class<? extends Model> inModel, Method method,  Class<? extends Annotation> annotationClass ){
    	return getAnnotation(inModel, method, annotationClass) != null ; 
    }
    
    public <A extends Annotation> A getAnnotation(Class<? extends Model> inModel, Method method, Class<A> annotationClass){
    	A a = null;
    	for (Class<? extends Model> sibling: getSiblingModelClasses(inModel)) {
        	MReflector<? extends Model> ref = MReflector.instance(sibling);
        	a = ref.getAnnotation(method,annotationClass);
        	if (a != null){
        		break;
        	}
    	}
    	return a; 
    }
    

    public SequenceSet<Class<? extends Model>> getClassHierarchies(Class<? extends Model> modelClass){
    	if (modelClass == null || modelClassesInClasspathSequence.contains(modelClass)){
    		SequenceSet<Class<? extends Model>> hierarchy = new SequenceSet<Class<? extends Model>>();
    		for (Class<? extends Model> sibling : getSiblingModelClasses(modelClass)){
            	MReflector<?> ref = MReflector.instance(sibling);
            	hierarchy.addAll(ref.getClassHierarchy());
    		}
    		return hierarchy;
    	}else {
    		return null;
    	}
    }

    public SequenceSet<Class<?>> getClassForests(Class<? extends Model> modelClass){
    	if (modelClass == null || modelClassesInClasspathSequence.contains(modelClass)){
    		SequenceSet<Class<?>> forest = new SequenceSet<Class<?>>();
    		for (Class<? extends Model> sibling : getSiblingModelClasses(modelClass) ){
    			MReflector<?> ref = MReflector.instance(sibling);
    			forest.addAll(ref.getClassForest());
    		}
        	return forest;
		}else {
			return null;
		}
    }
    
    public static class MReflector<M extends Model> extends Reflector<Model, M>{
    	
        private static final Map<Class<? extends Model>, MReflector<? extends Model>> mreflectors = new HashMap<Class<? extends Model>, MReflector<? extends Model>>();
        
    	@SuppressWarnings("unchecked")
        public static <M extends Model> MReflector<M> instance(Class<M> modelClass){
        	MReflector<M> ref = (MReflector<M>)mreflectors.get(modelClass);
        	if (ref == null){
        		synchronized (mreflectors) {
        			ref = (MReflector<M>)mreflectors.get(modelClass);
        			if (ref == null){
        				Config.instance().getLogger(TableReflector.class.getName()).fine("Trying to reflect "+ modelClass.getName());
        				ref = new MReflector<M>(modelClass);
        				mreflectors.put(modelClass, ref);
        			}
    			}
        	}
        	return ref;
        }
    	
    	public static void dispose(){
    		mreflectors.clear();
    	}
		private MReflector(Class<M> reflectedClass) {
			super(reflectedClass, Model.class);
		}
		
		public List<Method> getMethodsForSignature(Method method){
			return super.getMethodsForSignature(getMethodSignature(method));
		}
    }
}
