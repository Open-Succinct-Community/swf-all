package com.venky.swf.db.model.io;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.integration.FormatHelper;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractModelReader<M extends Model, T> extends ModelIO<M> implements ModelReader<M, T> {
    public void setInvalidReferencesAllowed(boolean invalidReferencesAllowed) {
        this.invalidReferencesAllowed = invalidReferencesAllowed;
    }
    private boolean invalidReferencesAllowed = false;
    public boolean isInvalidReferencesAllowed(){
        return invalidReferencesAllowed;
    }
    protected AbstractModelReader(Class<M> beanClass) {
        super(beanClass);
    }

    @SuppressWarnings("unchecked")
    public Class<T> getFormatClass() {
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<T>) pt.getActualTypeArguments()[1];
    }

    public MimeType getMimeType() {
        return FormatHelper.getMimeType(getFormatClass());
    }

    public M read(T source ,boolean saveRecursive) {
        return read(source, true,saveRecursive);
    }
    public M read(T source, boolean ensureAccessibleByLoggedInUser,boolean saveRecursive) {
        return read(source,ensureAccessibleByLoggedInUser,true,saveRecursive);
    }
    public M read(T source, boolean ensureAccessibleByLoggedInUser, boolean updateAttributesFromElement,boolean saveRecursive) {

        M m = createInstance();
        FormatHelper<T> helper = FormatHelper.instance(source);
        load(m,helper);

        M m1 = null ;
        if (updateAttributesFromElement){
            m1 = Database.getTable(getBeanClass()).getRefreshed(m,ensureAccessibleByLoggedInUser);
            load(m1, helper);
            if (saveRecursive) {
                m1.save(); //Needed to propagate the id.
            }

            //Since we were loading on a new instance,
            // it is possible that some fields are not getting marked as dirty which need to be getting marked as dirty and null.
            // Hence this second load is required.
            if (saveRecursive && !helper.getArrayElementNames().isEmpty()){
                Set<String> processedChildElementNames = new HashSet<>();
                for (Method childGetter : m1.getReflector().getChildGetters()){
                    ModelReflector<? extends Model> childReflector = ModelReflector.instance(m1.getReflector().getChildModelClass(childGetter));
                    String elementName = childReflector.getModelClass().getSimpleName();
                    if (processedChildElementNames.contains(elementName)){
                        continue;
                    }
                    processedChildElementNames.add(elementName);
                    List<T> childElements = helper.getArrayElements(elementName);
                    if (childElements.isEmpty()){
                        continue;
                    }
                    CONNECTED_VIA connectedVia  = getReflector().getAnnotation(childGetter, CONNECTED_VIA.class);
                    String parentAttributeName = null;
                    if (connectedVia != null ){
                        String connectedViaColumn = connectedVia.value();
                        parentAttributeName = StringUtil.camelize(childReflector.getFieldName(connectedViaColumn));
                    }else {
                        List<Method> methods = childReflector.getReferredModelGetters(getReflector().getModelClass());
                        if (methods.size() == 1){
                            parentAttributeName = StringUtil.camelize(childReflector.getReferenceField(methods.get(0)));
                        }
                    }

                    for (T childElement : childElements) {
                        FormatHelper.instance(childElement).setAttribute(parentAttributeName,StringUtil.valueOf(m1.getId())); // Link parent with child
                        ModelIOFactory.getReader(childReflector.getModelClass(),
                                getFormatClass()).read(childElement,ensureAccessibleByLoggedInUser,true,true);
                        //Saving done recursively
                    }
                }
            }
        }else {
            m1 =  Database.getTable(getBeanClass()).find(m,ensureAccessibleByLoggedInUser);
        }

        return m1;
    }
    private void load(M m, FormatHelper<T>helper){
        set(m, helper);

        for (Method referredModelGetter : getReflector().getReferredModelGetters()) {
            Class<? extends Model> referredModelClass = getReflector().getReferredModelClass(referredModelGetter);
            String refElementName = referredModelGetter.getName().substring("get".length());

            T refElement = helper.getElementAttribute(refElementName);
            if (refElement != null) {
                FormatHelper<T> refHelper = FormatHelper.instance(refElement);
                if (!refHelper.getAttributes().isEmpty()) {
                    Class<T> formatClass = getFormatClass();
                    if (refHelper.getAttributes().size() > 1){
                        if (isInvalidReferencesAllowed()) {
                            refHelper.removeAttribute("Id");
                        }
                    }
                    ModelReader<? extends Model, T> reader = (ModelReader<? extends Model, T>) ModelIOFactory.getReader(referredModelClass, formatClass);
                    reader.setInvalidReferencesAllowed(isInvalidReferencesAllowed());
                    Model referredModel = reader.read(refElement, false,false);
                    if (referredModel != null) {
                        if (referredModel.getRawRecord().isNewRecord()) {
                            if (!isInvalidReferencesAllowed()) {
                                throw new RuntimeException("Oops! Please select the correct " + referredModelClass.getSimpleName());
                            }else {
                                getReflector().set(m, getReflector().getReferenceField(referredModelGetter), null);
                            }
                        }else {
                            getReflector().set(m, getReflector().getReferenceField(referredModelGetter), referredModel.getId());
                        }
                    } else {
                        if (!isInvalidReferencesAllowed()){
                            throw new RuntimeException(referredModelClass.getSimpleName() + " not found for passed information " + refElement.toString());
                        }else {
                            getReflector().set(m, getReflector().getReferenceField(referredModelGetter), null);
                        }
                    }
                } else {
                    getReflector().set(m, getReflector().getReferenceField(referredModelGetter), null);
                }
            }
        }

    }

    private void set(M m, FormatHelper<T> helper) {

        for (String attributeName : helper.getAttributes()) {
            String fieldName = getFieldName(attributeName);
            if (fieldName == null){
                continue;
            }
            if (!getReflector().isFieldSettable(fieldName)){
                continue;
            }

            Object attrValue = helper.getAttribute(attributeName) ;
            if (attrValue == null && !helper.hasAttribute(attributeName)){
                continue;
            }
            Class<?> valueClass = getReflector().getFieldGetter(fieldName).getReturnType();

            ColumnDescriptor columnDescriptor = getReflector().getColumnDescriptor(fieldName);
            Object value = null;
            if (!getReflector().isVoid(attrValue) || !columnDescriptor.isNullable()){
                value = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(valueClass).getTypeConverter().valueOf(attrValue);
            }
            getReflector().set(m, fieldName, value);
        }

        Map<String, List<String>> groupedFields = getReflector().getGroupedFields();
        groupedFields.forEach((g,fl)->{
            T groupElement = helper.getElementAttribute(g);
            if (groupElement == null){
                groupElement = helper.createElementAttribute(g);
            }

            FormatHelper<T> groupHelper = FormatHelper.instance(groupElement);
            fl.forEach(fieldName ->{
                if (!getReflector().isFieldSettable(fieldName)){
                    return;
                }
                String attrName = getAttributeName(fieldName);
                Object attrValue = groupHelper.getAttribute(attrName);
                if (attrValue == null && !groupHelper.hasAttribute(attrName)){
                    return;
                }
                Class<?> valueClass = getReflector().getFieldGetter(fieldName).getReturnType();

                ColumnDescriptor columnDescriptor = getReflector().getColumnDescriptor(fieldName);
                Object value = null;
                if (!getReflector().isVoid(attrValue) || !columnDescriptor.isNullable()){
                    value = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(valueClass).getTypeConverter().valueOf(attrValue);
                }
                getReflector().set(m, fieldName, value);

            });
        });

    }
}
