/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Query;
import com.venky.swf.routing.Path;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.ModelEditView;
import com.venky.swf.views.model.ModelListView;
import com.venky.swf.views.model.ModelShowView;

/**
 *
 * @author venky
 */
public class ModelController<M extends Model> extends Controller {

    private Class<M> modelClass;

    public ModelController(Path path) {
        super(path);
        modelClass = getPath().getModelClass();
    }

    @Override
    public View index() {
        Query q = new Query(modelClass);
		//TODO VENKY Add to filter for parent model ids based on path information.
        List<M> records = q.select().execute();
        return dashboard(new ModelListView<M>(getPath(), modelClass, null, records));
    }
    

    public View show(int id) {
        M record = Database.getInstance().getTable(modelClass).get(id);
        return dashboard(new ModelShowView<M>(getPath(), modelClass, null, record));
    }

    public View edit(int id) {
        M record = Database.getInstance().getTable(modelClass).get(id);
        return dashboard(new ModelEditView<M>(getPath(), modelClass, null, record));
    }

    public View blank() {
        M record = Database.getInstance().getTable(modelClass).newRecord();
        ModelEditView<M> mev = new ModelEditView<M>(getPath(), modelClass, null, record);
        mev.getIncludedFields().remove("ID");
        return dashboard(mev);
    }

    public View destroy(int id){ 
        M record = Database.getInstance().getTable(modelClass).get(id);
        if (record != null){
            record.destroy();
        }
        return back();
    }
    
    public RedirectorView back(){
    	RedirectorView v = new RedirectorView(getPath());
    	v.setRedirectUrl(getPath().getBackTarget());
    	return v;
    }
    
    private boolean isNew(){ 
        return ObjectUtil.isVoid(getPath().getRequest().getParameter("ID"));
    }
    private void persistInDB(){
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }

        String id = getPath().getRequest().getParameter("ID");
        String lockId = getPath().getRequest().getParameter("LOCK_ID");
        M record = null;
        if (ObjectUtil.isVoid(id)) {
            record = Database.getInstance().getTable(modelClass).newRecord();
        } else {
            record = Database.getInstance().getTable(modelClass).get(Integer.valueOf(id));
            if (!ObjectUtil.isVoid(lockId)) {
                if (record.getLockId() != Long.parseLong(lockId)) {
                    throw new RuntimeException("Stale record update prevented. Please reload and retry!");
                }
            }
        }

        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        List<String> fields = reflector.getFields();
        fields.remove("ID");
        fields.remove("LOCK_ID");
        
        Enumeration<String> e = getPath().getRequest().getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String fieldName = fields.contains(name) ? name : null;

            if (fieldName != null) {
                Object value = request.getParameter(fieldName);
                Method getter = reflector.getFieldGetter(fieldName);
                Method setter = reflector.getFieldSetter(fieldName);

                TypeRef<?> typeRef = Database.getInstance().getJdbcTypeHelper().getTypeRef(getter.getReturnType());

                try {
                    setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }

        }
        record.save();
    }
    
    public View save() {
    	persistInDB();
        return back();
    }

    public View autocomplete(String value) {
    	ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        return super.autocomplete(modelClass, reflector.getDescriptionColumn(), value);
    }
    
    
}
