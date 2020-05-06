package com.venky.swf.db.model.io;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.integration.FormatHelper;

public abstract class AbstractModelReader<M extends Model, T> extends ModelIO<M> implements ModelReader<M, T> {

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

    public M read(T source) {
        return read(source, true);
    }
    public M read(T source, boolean ensureAccessibleByLoggedInUser) {
        return read(source,ensureAccessibleByLoggedInUser,true);
    }
    public M read(T source, boolean ensureAccessibleByLoggedInUser, boolean updateObject) {

        M m = createInstance();
        FormatHelper<T> helper = FormatHelper.instance(source);
        load(m,helper);

        M m1 = Database.getTable(getBeanClass()).getRefreshed(m, ensureAccessibleByLoggedInUser);
        load(m1, helper);

        return m1;
    }
    private void load(M m, FormatHelper<T>helper){
        set(m, helper);

        for (Method referredModelGetter : getReflector().getReferredModelGetters()) {
            Class<? extends Model> referredModelClass = getReflector().getReferredModelClass(referredModelGetter);
            String refElementName = referredModelGetter.getName().substring("get".length());

            T refElement = helper.getElementAttribute(refElementName);
            if (refElement != null) {
                if (!FormatHelper.instance(refElement).getAttributes().isEmpty()) {
                    Class<T> formatClass = getFormatClass();
                    ModelReader<? extends Model, T> reader = (ModelReader<? extends Model, T>) ModelIOFactory.getReader(referredModelClass, formatClass);
                    Model referredModel = reader.read(refElement, false,false);
                    if (referredModel != null) {
                        if (referredModel.getRawRecord().isNewRecord()) {
                            throw new RuntimeException("Oops! Please select the correct " + referredModelClass.getSimpleName());
                        }
                        getReflector().set(m, getReflector().getReferenceField(referredModelGetter), referredModel.getId());
                    } else {
                        throw new RuntimeException(referredModelClass.getSimpleName() + " not found for passed information " + refElement.toString());
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

            Object attrValue = helper.getAttribute(attributeName);
            Class<?> valueClass = getReflector().getFieldGetter(fieldName).getReturnType();

            ColumnDescriptor columnDescriptor = getReflector().getColumnDescriptor(fieldName);
            Object value = null;
            if (!getReflector().isVoid(attrValue) || !columnDescriptor.isNullable()){
                value = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(valueClass).getTypeConverter().valueOf(attrValue);
            }
            getReflector().set(m, fieldName, value);
        }
    }
}
