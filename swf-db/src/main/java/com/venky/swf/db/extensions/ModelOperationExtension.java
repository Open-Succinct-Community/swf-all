package com.venky.swf.db.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.model.Model;

import java.lang.reflect.ParameterizedType;

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
    }

    protected static <M extends Model> void registerExtension(ModelOperationExtension<M> instance){
        for (ModelOperationExtensionPoint value : ModelOperationExtensionPoint.values()) {
            Registry.instance().registerExtension(instance.getModelClass().getSimpleName() +"." +value.extensionPointName(), instance);
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
