/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.routing.Path;
import com.venky.swf.routing.Path.ModelInfo;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
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
    private ModelReflector<M> reflector ;
    public ModelController(Path path) {
        super(path);
        modelClass = getPath().getModelClass();
    	reflector = ModelReflector.instance(modelClass);
        
    }
    public Expression getWhereClause(){
    	Expression where = new Expression(Conjunction.AND);
		Map<Class<? extends Model>, List<Method>> classModelGetterMap = new HashMap<Class<? extends Model>, List<Method>>();

		for (Method referredModelGetter : reflector.getReferredModelGetters()){
			Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
			List<Method> getters = classModelGetterMap.get(referredModelClass);
			if (getters == null){
				getters = new ArrayList<Method>();
				classModelGetterMap.put(referredModelClass, getters);
			}
			getters.add(referredModelGetter);
		}
		
		if (classModelGetterMap.isEmpty()){
			return where;
		}

		List<ModelInfo> modelElements = getPath().getModelElements();

		for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){ // The last model is self.
    		ModelInfo mi = miIter.next();
    		if(!miIter.hasNext()){
    			//last model is self.
    			break;
    		}
    		if (!classModelGetterMap.containsKey(mi.getModelClass())){
    			continue;
    		}
    		
    		Expression referredModelWhere = new Expression(Conjunction.AND);
	    	ModelReflector<?> referredModelReflector = ModelReflector.instance(mi.getModelClass());
	    	for (Method childGetter : referredModelReflector.getChildGetters()){
	    		if (referredModelReflector.getChildModelClass(childGetter) == modelClass){
	            	CONNECTED_VIA join = reflector.getAnnotation(childGetter,CONNECTED_VIA.class);
	            	if (join == null){
	            		Expression referredModelWhereChoices = new Expression(Conjunction.OR);
	            		for (Method referredModelGetter: classModelGetterMap.get(mi.getModelClass())){ 
    	        	    	String referredModelIdFieldName =  StringUtil.underscorize(referredModelGetter.getName().substring(3) +"Id");
    	        	    	String referredModelIdColumnName = reflector.getColumnDescriptor(referredModelIdFieldName).getName();

    	        	    	referredModelWhereChoices.add(new Expression(referredModelIdColumnName,Operator.EQ,new BindVariable(mi.getId())));
	            		}
	            		referredModelWhere.add(referredModelWhereChoices);
	            	}else {
	            		String referredModelIdColumnName = join.value();
	            		referredModelWhere.add(new Expression(referredModelIdColumnName,Operator.EQ,new BindVariable(mi.getId())));
	            	}
	    		}
	    	}
    		if (referredModelWhere.getParameterizedSQL().length() > 0){
    			where.add(referredModelWhere);
    		}
    	}
		
		Expression dsw = getDataSecurityWhere();
		if (dsw.getParameterizedSQL().length()> 0){
			where.add(dsw); 
		}
    	return where;
		    	
    }
    public Expression getDataSecurityWhere(){
    	return getSessionUser().getDataSecurityWhereClause(modelClass);
    }
    @Override
    public View index() {
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getWhereClause()).execute(modelClass);
        return dashboard(new ModelListView<M>(getPath(), modelClass, null, records));
    }
    

    public View show(int id) {
        M record = Database.getInstance().getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser())){
            return dashboard(new ModelShowView<M>(getPath(), modelClass, null, record));
        }else {
        	throw new AccessDeniedException();
        }
    }
    
    public View view(int id){
    	M record = Database.getInstance().getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser())){
            try {
            	for (Method getter : getReflector().getFieldGetters()){
            		if (InputStream.class.isAssignableFrom(getter.getReturnType())){
            			CONTENT_TYPE ct = reflector.getAnnotation(getter,CONTENT_TYPE.class);
            			MimeType mimeType = MimeType.TEXT_PLAIN; 
            			if (ct  != null){
            				mimeType = ct.value();
            			}
        				return new BytesView(getPath(), toBytes((InputStream)getter.invoke(record)), mimeType);
            		}
            	}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			}
        }else {
        	throw new AccessDeniedException();
        }
        return back();
    }
    
    protected byte[] toBytes(InputStream in) throws IOException{
    	ByteArrayOutputStream os = new ByteArrayOutputStream();
    	byte [] b = new byte[1000];
    	int i = 0;
    	while ((i = in.read(b)) > 0){
    		os.write(b,0,i);
    	}
		return os.toByteArray();
    }

    public View edit(int id) {
        M record = Database.getInstance().getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser())){
            return dashboard(new ModelEditView<M>(getPath(), modelClass, null, record));
        }else {
        	throw new AccessDeniedException();
        }
    }

    public View clone(int id){
    	Table<M> table = Database.getInstance().getTable(modelClass);
    	M record = table.get(id);
    	M newrecord = table.newRecord();
    	
    	Record oldRaw = record.getRawRecord();
    	Record newRaw = newrecord.getRawRecord();
    	
    	for (String f:oldRaw.getFieldNames()){
    		newRaw.put(f, oldRaw.get(f));
    	}
    	newRaw.setNewRecord(true);
    	newRaw.remove("ID");
    	newRaw.remove("LOCK_ID");
    	newRaw.remove("UPDATED_AT");
    	newRaw.remove("CREATED_AT");
    	newRaw.remove("CREATOR_ID");
    	newRaw.remove("UPDATER_ID");
		ModelEditView<M> mev = new ModelEditView<M>(getPath(), modelClass, null, newrecord);
        mev.getIncludedFields().remove("ID");
        return dashboard(mev);
    	
    }
    public View blank() {
        M record = Database.getInstance().getTable(modelClass).newRecord();
        List<ModelInfo> modelElements =getPath().getModelElements();
        
		for (Method referredModelGetter: reflector.getReferredModelGetters()){
	    	Class<? extends Model> referredModelClass = (Class<? extends Model>)referredModelGetter.getReturnType();
	    	String referredModelIdFieldName =  reflector.getReferredModelIdFieldName(referredModelGetter);
	    	Method referredModelIdSetter =  reflector.getFieldSetter(referredModelIdFieldName);
	    	Method referredModelIdGetter =  reflector.getFieldGetter(referredModelIdFieldName);
	    	try {
				Integer oldValue = (Integer) referredModelIdGetter.invoke(record);
				List<Integer> idoptions = getSessionUser().getParticipationOptions(modelClass).get(referredModelIdFieldName);
				Integer id = null; 
						
				if (oldValue == null){
					if (idoptions != null && !idoptions.isEmpty() && idoptions.size() == 1){
						id = idoptions.get(0);
            	    	Model referredModel = Database.getInstance().getTable(referredModelClass).get(id);
            	    	if (referredModel.isAccessibleBy(getSessionUser())){
            	    		referredModelIdSetter.invoke(record,id);
            	    	}
					}
				}
				for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){
		    		ModelInfo mi = miIter.next();
		    		if(!miIter.hasNext()){
		    			//last model is self.
		    			break;
		    		}
	        		if (referredModelClass.isAssignableFrom(mi.getModelClass())){
	        	    	try {
	            	    	Model referredModel = Database.getInstance().getTable(referredModelClass).get(mi.getId());
	            	    	if (referredModel.isAccessibleBy(getSessionUser())){
	            	    		referredModelIdSetter.invoke(record, mi.getId());
	            	    		break;
	            	    	}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
	        		}
	        		
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}

		ModelEditView<M> mev = new ModelEditView<M>(getPath(), modelClass, null, record);
        mev.getIncludedFields().remove("ID");
        return dashboard(mev);
    }

    public View destroy(int id){ 
        M record = Database.getInstance().getTable(modelClass).get(id);
        if (record != null){
            if (record.isAccessibleBy(getSessionUser())){
                record.destroy();
            }else {
            	throw new AccessDeniedException();
            }
        }
        return back();
    }
    
    public RedirectorView back(){
    	RedirectorView v = new RedirectorView(getPath());
    	v.setRedirectUrl(getPath().getBackTarget());
    	return v;
    }
    
    private RedirectorView redirectTo(String action){
    	RedirectorView v = new RedirectorView(getPath(),action);
    	return v;
    }
    
    private View  persistInDB(){
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        
        Map<String,Object> formFields = new HashMap<String, Object>();
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        if (isMultiPart){
        	FileItemFactory factory = new DiskFileItemFactory(1024*1024*128, new File(System.getProperty("java.io.tmpdir")));
        	ServletFileUpload fu = new ServletFileUpload(factory);
        	try {
				List<FileItem> fis = fu.parseRequest(request);
				for (FileItem fi:fis){
					if (fi.isFormField()){
						formFields.put(fi.getFieldName(), fi.getString());
					}else {
						formFields.put(fi.getFieldName(), fi.getInputStream());
					}
				}
			} catch (FileUploadException e1) {
				throw new RuntimeException(e1);
			} catch (IOException e1){
				throw new RuntimeException(e1);
			}
        }else {
        	Enumeration<String> parameterNames = request.getParameterNames();
        	while (parameterNames.hasMoreElements()){
        		String name =parameterNames.nextElement();
            	formFields.put(name,request.getParameter(name));
        	}
        }
        String id = (String)formFields.get("ID");
        String lockId = (String)formFields.get("LOCK_ID");
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
            if (!record.isAccessibleBy(getSessionUser())){
            	throw new AccessDeniedException();
            }
        }

        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        List<String> fields = reflector.getRealFields();
        fields.remove("ID");
        fields.remove("LOCK_ID");
        fields.remove("UPDATED_AT");
        fields.remove("UPDATER_USER_ID");
        
        Iterator<String> e = formFields.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = fields.contains(name) ? name : null;

            if (fieldName != null) {
                Object value = formFields.get(fieldName);
                Method getter = reflector.getFieldGetter(fieldName);
                Method setter = reflector.getFieldSetter(fieldName);

                TypeRef<?> typeRef = Database.getInstance().getJdbcTypeHelper().getTypeRef(getter.getReturnType());

                try {
                	if (ObjectUtil.isVoid(value) && reflector.getColumnDescriptor(getter).isNullable()){
                        setter.invoke(record, getter.getReturnType().cast(null));
            		}else {
                        setter.invoke(record, typeRef.getTypeConverter().valueOf(value));
                	}
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }

        }
        boolean isNew = false;
        if (record.getRawRecord().isNewRecord()){
        	isNew = true;
        	record.setCreatorUserId(getSessionUser().getId());
        	record.setCreatedAt(null);
    	}
        record.setUpdaterUserId(getSessionUser().getId());
        record.setUpdatedAt(null);
        if (record.isAccessibleBy(getSessionUser())){
            record.save();
        	if (!getPath().canAccessControllerAction("save",String.valueOf(record.getId()))){
        		try {
					Database.getInstance().getCache(modelClass).clear();
				} catch (SQLException e1) {
					throw new AccessDeniedException(e1);
				}
        		throw new AccessDeniedException();	
        	}
        }else {
        	throw new AccessDeniedException();
        }
        
        if (isNew && !getPath().canAccessControllerAction("edit", String.valueOf(record.getId()))){
        	return redirectTo("edit/"+record.getId());
        }else {
        	return back();
        }
   }
    
    public View save() {
    	return persistInDB();
    }

    public View autocomplete(String value) {
    	ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        return super.autocomplete(modelClass,getWhereClause(), reflector.getDescriptionColumn(), value);
    }
	public ModelReflector<M> getReflector() {
		return reflector;
	}
    
    
    
}
