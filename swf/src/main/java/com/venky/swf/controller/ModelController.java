/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.search.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReader;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.ModelWriter;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.path.Path.ModelInfo;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.ModelEditView;
import com.venky.swf.views.model.ModelListView;
import com.venky.swf.views.model.ModelLoadView;
import com.venky.swf.views.model.ModelShowView;

/**
 *
 * @author venky
 */
public class ModelController<M extends Model> extends Controller {

    private Class<M> modelClass;
    private ModelReflector<M> reflector ;
    private boolean indexedModel = false; 
    public ModelController(Path path) {
        super(path);
        modelClass = getPath().getModelClass();
    	reflector = ModelReflector.instance(modelClass);
        indexedModel = !reflector.getIndexedFieldGetters().isEmpty();
    }
    protected ModelReflector<M> getReflector(){
    	return reflector;
    }
    public View exportxls(){
		List<String> fieldsIncluded = getReflector().getFields();
		Iterator<String> fieldIterator = fieldsIncluded.iterator();
		while (fieldIterator.hasNext()){
			if (getReflector().isHouseKeepingField(fieldIterator.next())){
				fieldIterator.remove();
			}
		}
		List<M> list = new Select().from(getModelClass()).where(getPath().getWhereClause()).execute(getModelClass(), new Select.AccessibilityFilter<M>());
		Workbook wb = new HSSFWorkbook();
		new ModelWriter<M>(getModelClass()).write(list, wb,fieldsIncluded);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			wb.write(os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new BytesView(getPath(), os.toByteArray(),MimeType.APPLICATION_XLS,"content-disposition", "attachment; filename=" + getModelClass().getSimpleName() + ".xls");
    }
    
    @Depends("save")
    public View importxls(){
        HttpServletRequest request = getPath().getRequest();

        if (request.getMethod().equalsIgnoreCase("GET")) {
        	return dashboard(new ModelLoadView<M>(getPath(), getModelClass(), getIncludedFields()));
        }else {
        	Map<String,Object> formFields = getFormFields();
        	if (!formFields.isEmpty()){
        		InputStream in = (InputStream)formFields.get("datafile");
        		try {
	    			Workbook book = new HSSFWorkbook(in);
	    			ModelReader<M> modelReader = new ModelReader<M>(getModelClass());
	    			for (M m : modelReader.read(book.getSheet(StringUtil.pluralize(getModelClass().getSimpleName())))){
	    				String descriptionColumn = getReflector().getDescriptionColumn();
	    				Object descriptionValue = m.getRawRecord().get(descriptionColumn);
	    				boolean createNewRecord = true;
	    				if ( !ObjectUtil.isVoid(descriptionColumn) && !ObjectUtil.isVoid(descriptionValue) ){
	    					for (M preExistingRecord:new Select().from(getModelClass()).where(new Expression(descriptionColumn,Operator.EQ, descriptionValue))
	    							.execute(getModelClass())){
	    						for (String fieldName : m.getRawRecord().getDirtyFields()){
		    						preExistingRecord.getRawRecord().put(fieldName, m.getRawRecord().get(fieldName));
	    						}
	    						preExistingRecord.save();
	    						createNewRecord = false;
	    					}
	    				}
	    				if(createNewRecord){
	    					save(m);
	    				}
	    			}
				} catch (Exception e) {
					Database.getInstance().getCache(getReflector()).clear();
					if (!(e instanceof RuntimeException)){
						throw new RuntimeException(e);
					}else {
						throw (RuntimeException)e;
					}
				}
    			
        	}
        	return redirectTo("index");
        }
        
    }
    
    
    @Override
    public View index() {
    	if (indexedModel){
    		return search();
    	}else {
    		return list();
    	}
    }


    public View search(){
    	Map<String,Object> formData = getFormFields();
    	String q = "";
    	int maxRecords = MAX_LIST_RECORDS;
		if (!formData.isEmpty()){
			rewriteQuery(formData);
			q = StringUtil.valueOf(formData.get("q"));
			Object mr = formData.get("maxRecords");
			if (!ObjectUtil.isVoid(mr)){
				maxRecords = Integer.parseInt(StringUtil.valueOf(mr));
			}
		}	
		return search(q,maxRecords);
    }
    
    public View search(String strQuery) {
    	return search(strQuery,MAX_LIST_RECORDS);
    }
    
    private View search(String strQuery,int maxRecords) {
		if (!ObjectUtil.isVoid(strQuery)){
			if (!getFormFields().containsKey("q")){
				getFormFields().put("q", strQuery);
			}
			LuceneIndexer indexer = LuceneIndexer.instance(getModelClass());
			Query q = indexer.constructQuery(strQuery);
			
			List<Integer> ids = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
			if (!ids.isEmpty()) {
				Select sel = new Select().from(getModelClass()).where(new Expression(Conjunction.AND)
					.add(new Expression("ID",Operator.IN,ids.toArray()))
					.add(getPath().getWhereClause()));
				List<M> records = sel.execute(getModelClass(),maxRecords,new Select.AccessibilityFilter<M>());
				return list(records);
			}else {
				return list(new ArrayList<M>());
			}
		}
		return list(maxRecords);
    }
    
    public static final int MAX_LIST_RECORDS = 10 ;
	protected void rewriteQuery(Map<String,Object> formData){
		
	}
	
	public View list(){
		return list(Select.MAX_RECORDS_ALL_RECORDS);
	}
	
    private View list(int maxRecords) {
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getPath().getWhereClause()).orderBy("ID DESC").execute(modelClass, maxRecords ,new Select.AccessibilityFilter<M>());
        return list(records);
    }
    
    protected View list(List<M> records){
    	return dashboard(createListView(records));
    }
    
    protected HtmlView createListView(List<M> records){
    	return new ModelListView<M>(getPath(), modelClass, getIncludedFields(), records);
    }
    
    protected String[] getIncludedFields(){
    	return null;
    }
	protected Class<M> getModelClass() {
		return modelClass;
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
    public View blank(){
		M record = Database.getTable(modelClass).newRecord();
		return blank(record);
    }
    
    protected View blank(M record) {
        List<ModelInfo> modelElements = getPath().getModelElements();
        
		for (Method referredModelGetter: reflector.getReferredModelGetters()){
	    	@SuppressWarnings("unchecked")
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
						if (id != null){
							Model referredModel = Database.getTable(referredModelClass).get(id);
	            	    	if (referredModel.isAccessibleBy(getSessionUser(),referredModelClass)){
	            	    		referredModelIdSetter.invoke(record,id);
	            	    		continue;
	            	    	}
						}
					}
				}
				for (Iterator<ModelInfo> miIter = modelElements.iterator() ; miIter.hasNext() ;){
		    		ModelInfo mi = miIter.next();
		    		if(!miIter.hasNext()){
		    			//last model is self.
		    			break;
		    		}
		    		if (mi.getId() == null){
    	    			continue;
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
        return dashboard(createEditView(record));
    }
    
    protected ModelEditView<M> createEditView(M record){
		ModelEditView<M> mev = new ModelEditView<M>(getPath(), getModelClass(), getIncludedFields(), record);
		for (String field : reflector.getFields()){
			if (reflector.isHouseKeepingField(field)){
		        mev.getIncludedFields().remove(field);
			}
		}
    	return mev;
    }
    
    public View truncate(){
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getPath().getWhereClause()).execute(modelClass,new Select.AccessibilityFilter<M>());
        for (M record: records){
        	record.destroy();
        }
        return redirectTo("index");
    }

    @SingleRecordAction(icon="/resources/images/destroy.png")
    public View destroy(int id){ 
		M record = Database.getTable(modelClass).get(id);
        destroy(record);
        return afterDestroyView();
    }
    private void destroy(M record){
        if (record != null){
            if (record.isAccessibleBy(getSessionUser(),modelClass)){
                record.destroy();
            }else {
            	throw new AccessDeniedException();
            }
        }
    }
    protected View afterDestroyView(){
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
    
    protected Map<String,Object> getFormFields(){
    	return getPath().getFormFields();
    }
      
    private View  persistInDB(){
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        
        Map<String,Object> formFields = getFormFields();
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

        List<String> setableFields = reflector.getRealFields();
        for (String virtualField: reflector.getVirtualFields()){
    		if (reflector.isFieldSettable(virtualField)){
    			setableFields.add(virtualField);
    		}
        }
        
        Iterator<String> e = formFields.keySet().iterator();
        String buttonName = null;
        String digest = null;
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = setableFields.contains(name) && !reflector.isHouseKeepingField(name) ? name : null;
            if (fieldName != null) {
                Object value = formFields.get(fieldName);
                reflector.set(record, fieldName, value);
            }else if ( name.startsWith("_SUBMIT")){
            	buttonName = name;
            }else if ( name.startsWith("_FORM_DIGEST")){
            	digest = (String)formFields.get("_FORM_DIGEST");
            }
        }
        boolean isNew = record.getRawRecord().isNewRecord();
        boolean hasUserModifiedData = hasUserModifiedData(formFields,digest);
        if (hasUserModifiedData || isNew){
        	try {
        		save(record);
        	}catch (RuntimeException ex){
        		Throwable th = ExceptionUtil.getRootCause(ex);
        		String message = th.getMessage();
        		if (message == null){
        			message = th.toString();
            		th.printStackTrace();
        		}
        		record.setTxnPropery("ui.error.msg", message);
        		return dashboard(createEditView(record));
        	}
    	}
        
        if (isNew &&  hasUserModifiedData && buttonName.equals("_SUBMIT_MORE") && getPath().canAccessControllerAction("blank",String.valueOf(record.getId()))){
        	//Usability Logic: If user is not modifying data shown, then why be in data entry mode. 
        	return redirectTo("blank");
        }
        
        return afterPersistDBView(record);
    }
    
    private void save(M record){
        if (record.getRawRecord().isNewRecord()){
        	record.setCreatorUserId(getSessionUser().getId());
        	record.setCreatedAt(null);
    	}
        record.setUpdaterUserId(getSessionUser().getId());
        record.setUpdatedAt(null);

    	if (record.isAccessibleBy(getSessionUser(),modelClass)){
            record.save();
        	if (!getPath().canAccessControllerAction("save",String.valueOf(record.getId()))){
				Database.getInstance().getCache(reflector).clear();
        		throw new AccessDeniedException();	
        	}
        }else {
        	throw new AccessDeniedException();
        }
    	
    }

    protected View afterPersistDBView(M record){
        return back();
    }
    
    protected boolean hasUserModifiedData(Map<String,Object> formFields, String oldDigest){
    	StringBuilder hash = null;
        for (String field: reflector.getFields()){
        	if (!formFields.containsKey(field) || !reflector.isFieldSettable(field)){
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
		Map<String,Object> formData = getFormFields();
		M model = null;
		if (formData.containsKey("ID")){
			model = Database.getTable(modelClass).get(Integer.valueOf(formData.get("ID").toString()));
		}else {
			model = Database.getTable(modelClass).newRecord();
		}
		model = model.cloneProxy();
			
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
    				autoCompleteFieldValues.remove(null); // We need not try to use null for lookups.
    				where.add(new Expression("ID",Operator.IN,autoCompleteFieldValues.toArray()));
    			}
    		}
		}
		
		Class<? extends Model> autoCompleteModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(reflector.getFieldGetter(autoCompleteFieldName)));
		ModelReflector<? extends Model> autoCompleteModelReflector = ModelReflector.instance(autoCompleteModelClass); 
		Path autoCompletePath = getPath().createRelativePath(autoCompleteModelReflector.getTableName().toLowerCase());
		where.add(autoCompletePath.getWhereClause());
        return super.autocomplete(autoCompleteModelClass, where, autoCompleteModelReflector.getDescriptionColumn(), value,reflector.getColumnDescriptor(autoCompleteFieldName).isNullable());
    }
    
    
    
}
