package com.venky.swf.db.extensions;

import com.venky.core.string.StringUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Must be extended only once if this class is extended two level deep, the the grand child must implement all
 * methods implemented by its parent and defer to the parent with super call. Thats the only way to ensure all extensions
 * are registered correctly. This class used declared methods to optimize installing call outs/extension..
 * @param <M>
 */
public abstract class  ModelOperationExtension<M extends Model> implements Extension {
    enum ModelOperationExtensionPoint {
        before_validate,
        after_validate,
        before_create,
        after_create,
        before_save,
        after_save,
        before_destroy,
        after_destroy;

        public String extensionPointName(){
            return this.name().replace('_','.');
        }
        public static ModelOperationExtensionPoint extensionPoint(String exptName){
            return ModelOperationExtensionPoint.valueOf(exptName.replace('.','_'));
        }
        public String getMethodName(){
            return StringUtil.camelize(name(),false);
        }
    }
    public static final Set<String> overrideableMethods = Collections.unmodifiableSet(new HashSet<>(){{
        for (ModelOperationExtensionPoint value : ModelOperationExtensionPoint.values()) {
            add(value.getMethodName());
        }
    }});


    protected static <M extends Model> void registerExtension(ModelOperationExtension<M> instance){
        Method[] methods = instance.getClass().getDeclaredMethods();
        Set<String> overriddenMethods = new HashSet<>();
        for (Method method : methods){
            if (overrideableMethods.contains(method.getName()) && method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(instance.getModelClass())) {
                overriddenMethods.add(method.getName());
            }
        }
        for (ModelOperationExtensionPoint value : ModelOperationExtensionPoint.values()) {
            if (overriddenMethods.contains(value.getMethodName())) {
                Registry.instance().registerExtension(instance.getModelClass().getSimpleName() + "." + value.extensionPointName(), instance);
            }
        }
    }
    protected static <M extends Model> void deregisterExtension(ModelOperationExtension<M> instance){
        for (ModelOperationExtensionPoint value : ModelOperationExtensionPoint.values()) {
            Registry.instance().deregisterExtension(instance.getModelClass().getSimpleName() +"." + value.extensionPointName(), instance);
        }
    }
    @SuppressWarnings("unchecked")
    protected static <M extends Model> Class<M> getModelClass(ModelOperationExtension<M> instance){
        ParameterizedType pt = (ParameterizedType)instance.getClass().getGenericSuperclass();
        return (Class<M>) pt.getActualTypeArguments()[0];
    }
    protected Class<M> getModelClass(){
        return getModelClass(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(Object... context){
        M instance = (M) context[0];
        String pointName = ((String) context[1]).substring(getModelClass().getSimpleName().length()  + 1);
        ModelOperationExtensionPoint modelOperationExtensionPoint = ModelOperationExtensionPoint.extensionPoint(pointName);
        switch (modelOperationExtensionPoint){
            case before_validate:
                beforeValidate(instance);
                break;
            case after_validate:
                afterValidate(instance);
                break;
            case before_create:
                beforeCreate(instance);
                break;
            case after_create:
                afterCreate(instance);
                break;
            case before_save:
                beforeSave(instance);
                break;
            case after_save:
                afterSave(instance);
                break;
            case before_destroy:
                beforeDestroy(instance);
                break;
            case after_destroy:
                afterDestroy(instance);
                break;
        }

    }
    public String getPool(){
        return ModelReflector.instance(getModelClass(this)).getPool();
    }

    protected void afterValidate(M instance){

    }

    protected void beforeValidate(M instance) {

    }

    protected void beforeCreate(M instance) {

    }
    protected void afterCreate(M instance){

    }
    protected void beforeSave(M instance){

    }
    protected void afterSave(M instance){

    }
    protected void beforeDestroy(M instance){

    }
    protected void afterDestroy(M instance){

    }
}
