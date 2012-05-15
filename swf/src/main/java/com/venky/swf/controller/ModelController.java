/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

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

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.path.Path.ModelInfo;
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
    protected ModelReflector<M> getReflector(){
    	return reflector;
    }
    
    private static List<Method> getReferredModelGetters(Map<String,List<Method>> referredModelGettersMap, String referredTableName){
    	List<Method> rmg = referredModelGettersMap.get(referredTableName);
    	if (rmg == null){
    		rmg = new ArrayList<Method>();
    	}
    	return rmg;
    }
    public Expression getWhereClause(){
    	return getWhereClause(reflector);
    }
    public Expression getWhereClause(ModelReflector<?> reflector){
    	Expression where = new Expression(Conjunction.AND);
		Map<String, List<Method>> referredModelGetterMap = new HashMap<String, List<Method>>();

		for (Method referredModelGetter : reflector.getReferredModelGetters()){
			Class<? extends Model> referredModelClass = (Class<? extends Model>) referredModelGetter.getReturnType();
			
			ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
			String referredTableName = referredModelReflector.getTableName();
			
			List<Method> getters = referredModelGetterMap.get(referredTableName);
			if (getters == null){
				getters = new ArrayList<Method>();
				referredModelGetterMap.put(referredTableName, getters);
			}
			
			getters.add(referredModelGetter);
		}
		
		if (referredModelGetterMap.isEmpty()){
			return where;
		}

		List<ModelInfo> modelElements = getPath().getModelElements();

		for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){ // The last model is self.
    		ModelInfo mi = miIter.next();
    		if(!miIter.hasNext()){
    			//last model is self.
    			break;
    		}
    		List<Method> referredModelGetters = getReferredModelGetters(referredModelGetterMap, mi.getReflector().getTableName());
    		
    		if (referredModelGetters.isEmpty() || mi.getId() == null){
    			continue;
    		}
    		
    		Expression referredModelWhere = new Expression(Conjunction.AND);
	    	ModelReflector<?> referredModelReflector = mi.getReflector();
	    	for (Method childGetter : referredModelReflector.getChildGetters()){
	    		Class<? extends Model> childModelClass = referredModelReflector.getChildModelClass(childGetter);
	    		if (reflector.reflects(childModelClass)){
	            	CONNECTED_VIA join = referredModelReflector.getAnnotation(childGetter,CONNECTED_VIA.class);
	            	if (join == null){
	            		Expression referredModelWhereChoices = new Expression(Conjunction.OR);
	            		for (Method referredModelGetter: referredModelGetters){ 
    	        	    	String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
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
		
		Expression dsw = getSessionUser().getDataSecurityWhereClause(reflector.getModelClass());
		if (dsw.getParameterizedSQL().length()> 0){
			where.add(dsw); 
		}
		
    	return where;
		    	
    }
    @Override
    public View index() {
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getWhereClause()).execute(modelClass);
        return dashboard(new ModelListView<M>(getPath(), modelClass, getIncludedFields(), records));
    }
    
    protected String[] getIncludedFields(){
    	return null;
    }
    
    @SingleRecordAction(icon="/resources/images/show.png")
    public View show(int id) {
		M record = Database.getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser(),modelClass)){
            return dashboard(new ModelShowView<M>(getPath(), modelClass, getIncludedFields(), record));
        }else {
        	throw new AccessDeniedException();
        }
    }
    
    public View view(int id){
		M record = Database.getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser(),modelClass)){
            try {
            	for (Method getter : reflector.getFieldGetters()){
            		if (InputStream.class.isAssignableFrom(getter.getReturnType())){
            			CONTENT_TYPE ct = reflector.getAnnotation(getter,CONTENT_TYPE.class);
            			MimeType mimeType = MimeType.TEXT_PLAIN; 
            			if (ct  != null){
            				mimeType = ct.value();
            			}
        				return new BytesView(getPath(), StringUtil.readBytes((InputStream)getter.invoke(record)), mimeType);
            		}
            	}
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
    

    @SingleRecordAction(icon="/resources/images/edit.png")
    @Depends("save")
    public View edit(int id) {
        M record = Database.getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser(),modelClass)){
            return dashboard(new ModelEditView<M>(getPath(), modelClass, getIncludedFields(), record));
        }else {
        	throw new AccessDeniedException();
        }
    }

    @SingleRecordAction(icon="/resources/images/clone.png")
    @Depends("save")
    public View clone(int id){
		Table<M> table = Database.getTable(modelClass);
    	M record = table.get(id);
    	M newrecord = table.newRecord();
    	
    	Record oldRaw = record.getRawRecord();
    	Record newRaw = newrecord.getRawRecord();
    	
    	for (String f:oldRaw.getFieldNames()){ //Fields in raw records are column names.
    		if (!reflector.isHouseKeepingField(reflector.getFieldName(f))){
        		newRaw.put(f, oldRaw.get(f));
    		}
    	}
    	newRaw.setNewRecord(true);
		ModelEditView<M> mev = new ModelEditView<M>(getPath(), modelClass, getIncludedFields(), newrecord);
		for (String field : reflector.getFields()){
			if (reflector.isHouseKeepingField(field)){
		        mev.getIncludedFields().remove(field);
			}
		}
        return dashboard(mev);
    	
    }
    
    @Depends("save")
    public View blank() {
		M record = Database.getTable(modelClass).newRecord();
        List<ModelInfo> modelElements = getPath().getModelElements();
        
		for (Method referredModelGetter: reflector.getReferredModelGetters()){
	    	Class<? extends Model> referredModelClass = (Class<? extends Model>)referredModelGetter.getReturnType();
	    	String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
	    	Method referredModelIdSetter =  reflector.getFieldSetter(referredModelIdFieldName);
	    	Method referredModelIdGetter =  reflector.getFieldGetter(referredModelIdFieldName);
	    	try {
				Integer oldValue = (Integer) referredModelIdGetter.invoke(record);
				List<Integer> idoptions = getSessionUser().getParticipationOptions(modelClass).get(referredModelIdFieldName);
				Integer id = null; 
						
				if (oldValue == null){
					if (idoptions != null && !idoptions.isEmpty() && idoptions.size() == 1){
						id = idoptions.get(0);
						Model referredModel = Database.getTable(referredModelClass).get(id);
            	    	if (referredModel.isAccessibleBy(getSessionUser(),referredModelClass)){
            	    		referredModelIdSetter.invoke(record,id);
            	    		continue;
            	    	}
					}
				}
				for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){
		    		ModelInfo mi = miIter.next();
		    		if(!miIter.hasNext()){
		    			//last model is self.
		    			break;
		    		}
		    		
	        		if (mi.getReflector().reflects(referredModelClass)){
	        	    	try {
							Model referredModel = Database.getTable(referredModelClass).get(mi.getId());
	            	    	if (referredModel.isAccessibleBy(getSessionUser(),referredModelClass)){
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
		record.setCreatorUserId(getSessionUser().getId());
		record.setUpdaterUserId(getSessionUser().getId());
		ModelEditView<M> mev = new ModelEditView<M>(getPath(), modelClass, getIncludedFields(), record);
		for (String field : reflector.getFields()){
			if (reflector.isHouseKeepingField(field)){
		        mev.getIncludedFields().remove(field);
			}
		}
        return dashboard(mev);
    }

    @SingleRecordAction(icon="/resources/images/destroy.png")
    public View destroy(int id){ 
		M record = Database.getTable(modelClass).get(id);
        if (record != null){
            if (record.isAccessibleBy(getSessionUser(),modelClass)){
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
    private Map<String,Object> getFormFields(HttpServletRequest request){
    	Map<String,Object> formFields = new HashMap<String, Object>();
        boolean isMultiPart = ServletFileUpload.isMultipartContent(request);
        if (isMultiPart){
        	FileItemFactory factory = new DiskFileItemFactory(1024*1024*128, new File(System.getProperty("java.io.tmpdir")));
        	ServletFileUpload fu = new ServletFileUpload(factory);
        	try {
				List<FileItem> fis = fu.parseRequest(request);
				for (FileItem fi:fis){
					if (fi.isFormField()){
						if (!formFields.containsKey(fi.getFieldName())){
							formFields.put(fi.getFieldName(), fi.getString());
						}
					}else {
						formFields.put(fi.getFieldName(), new ByteArrayInputStream(StringUtil.readBytes(fi.getInputStream())));
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
        return formFields;
    }
      
    private View  persistInDB(){
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        
        Map<String,Object> formFields = getFormFields(request);
        String id = (String)formFields.get("ID");
        String lockId = (String)formFields.get("LOCK_ID");
        M record = null;
        if (ObjectUtil.isVoid(id)) {
			record = Database.getTable(modelClass).newRecord();
        } else {
			record = Database.getTable(modelClass).get(Integer.valueOf(id));
            if (!ObjectUtil.isVoid(lockId)) {
                if (record.getLockId() != Long.parseLong(lockId)) {
                    throw new RuntimeException("Stale record update prevented. Please reload and retry!");
                }
            }
            if (!record.isAccessibleBy(getSessionUser(),modelClass)){
            	throw new AccessDeniedException();
            }
        }

        List<String> fields = reflector.getRealFields();
        
        Iterator<String> e = formFields.keySet().iterator();
        String buttonName = null;
        String digest = null;
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = fields.contains(name) && !reflector.isHouseKeepingField(name) ? name : null;
            if (fieldName != null) {
                Object value = formFields.get(fieldName);
                reflector.set(record, fieldName, value);
            }else if ( name.startsWith("_SUBMIT")){
            	buttonName = name;
            }else if ( name.startsWith("_FORM_DIGEST")){
            	digest = (String)formFields.get("_FORM_DIGEST");
            }
        }
        boolean isNew = false;
        if (hasUserModifiedData(formFields,digest)){
	        if (record.getRawRecord().isNewRecord()){
	        	isNew = true;
	        	record.setCreatorUserId(getSessionUser().getId());
	        	record.setCreatedAt(null);
	    	}
	        record.setUpdaterUserId(getSessionUser().getId());
	        record.setUpdatedAt(null);
	        if (record.isAccessibleBy(getSessionUser(),modelClass)){
	            record.save();
	        	if (!getPath().canAccessControllerAction("save",String.valueOf(record.getId()))){
	        		try {
						Database.getInstance().getCache(reflector).clear();
					} catch (SQLException e1) {
						throw new AccessDeniedException(e1);
					}
	        		throw new AccessDeniedException();	
	        	}
	        }else {
	        	throw new AccessDeniedException();
	        }
    	}
        
        if (isNew &&  buttonName.equals("_SUBMIT_MORE") && getPath().canAccessControllerAction("blank",String.valueOf(record.getId()))){
        	return redirectTo("blank");
        }
        
        return back();
    }
    
    protected boolean hasUserModifiedData(Map<String,Object> formFields, String oldDigest){
    	StringBuilder hash = null;
        for (String field: reflector.getFields()){
        	if (!formFields.containsKey(field)){
        		continue;
        	}
    		Object currentValue = formFields.get(field);
    		if (currentValue != null && currentValue instanceof InputStream){
    			if (StringUtil.readBytes((InputStream)currentValue).length > 0){
    				return true;
    			}
    			currentValue = "";
    		}
			if (hash != null){
				hash.append(",");
			}else {
				hash = new StringBuilder();
			}
			hash.append(field).append("=").append((String)currentValue);
        }
        String newDigest = hash == null ? null : Encryptor.encrypt(hash.toString());
        return !ObjectUtil.equals(newDigest, oldDigest);
    }
    
    public View save() {
    	return persistInDB();
    }

    public View autocomplete() {
		List<String> fields = reflector.getFields();
		Map<String,Object> formData = getFormFields(getPath().getRequest());
		M model = null;
		if (formData.containsKey("ID")){
			model = Database.getTable(modelClass).get(Integer.valueOf(formData.get("ID").toString()));
		}else {
			model = Database.getTable(modelClass).newRecord();
		}
			
		String autoCompleteFieldName = null;
		String value = "";
		for (String fieldName : formData.keySet()){
			if (fields.contains(fieldName)){
				Object ov = formData.get(fieldName); 
				reflector.set(model,fieldName, ov);
			}else if (fieldName.startsWith("_AUTO_COMPLETE_")){
				autoCompleteFieldName = fieldName.split("_AUTO_COMPLETE_")[1];
				value = StringUtil.valueOf(formData.get(fieldName));
			}
		}
		model.getRawRecord().remove(autoCompleteFieldName);
		
    	Expression where = new Expression(Conjunction.AND);
		if (reflector.isAnnotationPresent(reflector.getFieldGetter(autoCompleteFieldName),PARTICIPANT.class)){
    		Map<String,List<Integer>> pOptions = getSessionUser().getParticipationOptions(reflector.getModelClass(),model);
    		if (pOptions.containsKey(autoCompleteFieldName)){
    			List<Integer> autoCompleteFieldValues = pOptions.get(autoCompleteFieldName);
    			if (!autoCompleteFieldValues.isEmpty()){
    				where.add(new Expression("ID",Operator.IN,autoCompleteFieldValues.toArray()));
    			}
    		}
		}
		
		Class<? extends Model> autoCompleteModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(reflector.getFieldGetter(autoCompleteFieldName)));
		ModelReflector<? extends Model> autoCompleteModelReflector = ModelReflector.instance(autoCompleteModelClass); 
		
		where.add(getWhereClause(autoCompleteModelReflector));//
        return super.autocomplete(autoCompleteModelClass, where, autoCompleteModelReflector.getDescriptionColumn(), value);
    }
    
    
    
}
