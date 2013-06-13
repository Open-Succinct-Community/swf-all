/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller; 

import static com.venky.core.log.TimerStatistics.Timer.startTimer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.search.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.cache.Cache;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.ForwardedView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.HtmlView.StatusType;
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
    private boolean indexedModel = false; 
    private IntegrationAdaptor<M, ?> integrationAdaptor = null;
    public ModelController(Path path) {
        super(path);
        modelClass = getPath().getModelClass();
    	reflector = ModelReflector.instance(modelClass);
        indexedModel = !reflector.getIndexedFieldGetters().isEmpty();
        if (path.getProtocol() != MimeType.TEXT_HTML){
        	integrationAdaptor = IntegrationAdaptor.instance(modelClass, FormatHelper.getFormatClass(path.getProtocol()));
        }
    }
    
    public IntegrationAdaptor<M, ?> getIntegrationAdaptor() {
		return this.integrationAdaptor;
	}

	protected ModelReflector<M> getReflector(){
    	return reflector;
    }
    
    public View exportxls(){
    	ensureUI();
		Workbook wb = new HSSFWorkbook();
    	super.exportxls(getModelClass(), wb);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			wb.write(os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	return new BytesView(getPath(), os.toByteArray(),MimeType.APPLICATION_XLS,"content-disposition", "attachment; filename=" + getModelClass().getSimpleName() + ".xls");
    }
    
    @Depends("save")
    @Override
    public View importxls(){
    	ensureUI();
    	return super.importxls();
    }
    
    protected void ensureUI(){
    	if (integrationAdaptor != null) {
    		throw new RuntimeException("Action is only available from UI"); 
    	}
    }
    
    @Override
    @RequireLogin(true)
    public View index() {
    	Timer index = startTimer(getReflector().getTableName() + ".index", Config.instance().isTimerAdditive());
    	try {
	    	if (indexedModel){
	    		return search();
	    	}else {
	    		return list();
	    	}
    	}finally {
    		index.stop();
    	}
    }


    public View search(){
    	Map<String,Object> formData = new HashMap<String, Object>();
    	formData.putAll(getFormFields());
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
    	getFormFields().put("q",strQuery);
    	Map<String,Object> formData = new HashMap<String, Object>(getFormFields());
    	rewriteQuery(formData);

    	String q = StringUtil.valueOf(formData.get("q"));
    	return search(q,MAX_LIST_RECORDS);
    }
    
    protected View search(String strQuery,int maxRecords) {
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
					.add(getPath().getWhereClause())).orderBy(getReflector().getOrderBy());
				List<M> records = sel.execute(getModelClass(),maxRecords,new DefaultModelFilter<M>(getModelClass()));
				return list(records);
			}else {
				return list(new ArrayList<M>());
			}
		}
		return list(maxRecords);
    }
    
    public static final int MAX_LIST_RECORDS = 30 ;
	protected void rewriteQuery(Map<String,Object> formData){
		String strQuery = StringUtil.valueOf(formData.get("q"));
		StringBuilder q = new StringBuilder();
		
		if (!ObjectUtil.isVoid(strQuery) && !strQuery.contains(":")){
			for (String f:getReflector().getIndexedFields()){
				if (q.length() > 0 ){
					q.append(" OR ");
				}
				Method referredModelIdGetter = getReflector().getFieldGetter(f);
				if (getReflector().getReferredModelGetterFor(referredModelIdGetter) != null){
					q.append(f.substring(0,f.length()-"_ID".length())).append(":").append(strQuery);
				}else {
					q.append(f).append(":").append(strQuery);
				}
			}
			try { 
				Integer id = Integer.valueOf(strQuery);
				if (q.length() > 0){
					q.append(" OR ");
				}
				q.append("ID:").append(strQuery);
			}catch (NumberFormatException ex){
				// Nothing to do.
			}
			formData.put("q", q.toString());
		}
		Config.instance().getLogger(getClass().getName()).fine(formData.toString());
	}
	
	public View list(){
		return list(Select.MAX_RECORDS_ALL_RECORDS);
	}
	
    private View list(int maxRecords) {
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getPath().getWhereClause()).orderBy(getReflector().getOrderBy()).execute(modelClass, maxRecords ,new DefaultModelFilter<M>(getModelClass()));
        if (maxRecords > 0 && records.size() ==  maxRecords){
        	getPath().addInfoMessage("Refine your search if the record you are looking for is not listed.");
        }
        return list(records);
    }
    
    protected View list(List<M> records){
    	View v = null;
    	if (integrationAdaptor != null){
    		v = integrationAdaptor.createResponse(getPath(),records);
    	}else {
    		View lv = constructModelListView(records);
    		if (lv instanceof HtmlView){
        		v = dashboard((HtmlView)lv); 
    		}else {
    			// To support View Redirection.!!
    			v = lv;
    		}
    	}
    	return v;
    }
    
    protected View constructModelListView(List<M> records){
    	return new ModelListView<M>(getPath(), modelClass, getIncludedFields(), records);
    }
    
    protected String[] getIncludedFields(){
    	return null;
    }
	protected Class<M> getModelClass() {
		return modelClass;
	}

    @SingleRecordAction(icon="/resources/images/show.png")
    @Depends("index")
    public View show(int id) {
    	M record = Database.getTable(modelClass).get(id);
		if (!record.isAccessibleBy(getSessionUser(),modelClass)){
			throw new AccessDeniedException();
		}
		View view = null ;
		if (integrationAdaptor != null){
			view = integrationAdaptor.createResponse(getPath(),record);
		}else {
			view = dashboard(createModelShowView(record));
		}
    	return view;
    }
    protected ModelShowView<M> createModelShowView(M record){
    	return constructModelShowView(getPath(),record);
    }
    protected ModelShowView<M> constructModelShowView(Path path, M record){
    	return new ModelShowView<M>(path, modelClass, getIncludedFields(), record);
    }

    @Depends("index")
    public View view(int id){
		M record = Database.getTable(modelClass).get(id);
        if (record.isAccessibleBy(getSessionUser(),modelClass)){
            try {
            	for (Method getter : reflector.getFieldGetters()){
            		if (InputStream.class.isAssignableFrom(getter.getReturnType())){
            			String fieldName = reflector.getFieldName(getter);
            			String mimeType = reflector.getContentType(record, fieldName);
            			String fileName = reflector.getContentName(record,fieldName);
            			if (fileName != null){
            				return new BytesView(getPath(), StringUtil.readBytes((InputStream)getter.invoke(record)), mimeType,
									"content-disposition","attachment; filename=\"" + fileName +"\"");
            			}else {
            				return new BytesView(getPath(), StringUtil.readBytes((InputStream)getter.invoke(record)), mimeType);
            			}
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
    	return getSuccessView();
    }
    
    @SingleRecordAction(icon="/resources/images/edit.png")
    @Depends("save,index")
    public View edit(int id) {
    	ensureUI();
        return dashboard(createModelEditView(id, "save"));
    }
    protected ModelEditView<M> createModelEditView(int id, String formAction){
    	M record = Database.getTable(modelClass).get(id);
    	return createModelEditView(record, formAction);
    }
    protected ModelEditView<M> createModelEditView(M record, String formAction){
    	return createModelEditView(getPath(), record, formAction);
    }
    protected ModelEditView<M> createModelEditView(Path path, M record, String formAction){
        if (record.isAccessibleBy(getSessionUser(),getModelClass())){
        	return constructModelEditView(path, record, formAction);
        }else {
        	throw new AccessDeniedException();
        }
    }
    protected ModelEditView<M> constructModelEditView(Path path, M record, String formAction){
    	return new ModelEditView<M>(path, getModelClass(), getIncludedFields(), record,formAction);
    }

    @SingleRecordAction(icon="/resources/images/clone.png")
    @Depends("save,index")
    public View clone(int id){
    	M record = Database.getTable(modelClass).get(id);
    	M newrecord = clone(record);
		return blank(newrecord);
    }
    
	public M clone(M record){
		Table<M> table = Database.getTable(modelClass);
    	M newrecord = table.newRecord();
    	
    	Record oldRaw = record.getRawRecord();
    	Record newRaw = newrecord.getRawRecord();
    	
    	for (String f:oldRaw.getFieldNames()){ //Fields in raw records are column names.
    		if (getReflector().isFieldCopiedWhileCloning(getReflector().getFieldName(f))){
        		newRaw.put(f, oldRaw.get(f));
    		}
    	}
    	newRaw.setNewRecord(true);
    	return newrecord;
    }
    
    @Depends("save")
    public View blank(){
		M record = Database.getTable(modelClass).newRecord();
		return blank(record);
    }
    
    protected View blank(M record) {
    	getPath().fillDefaultsForReferenceFields(record,getModelClass());
    	if (integrationAdaptor != null){
    		return integrationAdaptor.createResponse(getPath(),record);
    	}else {
    		return dashboard(createBlankView(record,"save"));
    	}
    }

    protected ModelEditView<M> createBlankView(M record,String formAction){
    	return createBlankView(getPath(), record, formAction);
    }
    
    protected ModelEditView<M> createBlankView(Path path , M record, String formAction){
		ModelEditView<M> mev = constructModelEditView(path, record,formAction);
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
        	if (getPath().canAccessControllerAction("destroy", String.valueOf(record.getId()))){
            	record.destroy();
        	}else {
        		throw new AccessDeniedException("Don't have permission to destroy record " + record.getId());
        	}
        }
        return back();
    }

    @SingleRecordAction(icon="/resources/images/destroy.png")
    @Depends("index")
    public View destroy(int id){ 
		M record = Database.getTable(modelClass).get(id);
        destroy(record);
        return getSuccessView();
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
    
	protected View getSuccessView(){
    	View ret = null ;
    	if (integrationAdaptor != null){
    		ret = integrationAdaptor.createStatusResponse(getPath(),null);
    	}else {
			ret = back();
		}
    	return ret;
    }
	
    protected RedirectorView redirectTo(String action){
    	RedirectorView v = new RedirectorView(getPath(),action);
    	return v;
    }

    protected View forwardTo(String action){
		return new ForwardedView(getPath(), action);
    }
    
    public static interface Action<M> {
    	public View noAction(M m);
    	public void act(M m);
    	public View error(M m);
    }
    
    public class SaveAction implements Action<M>{
    	public View noAction(M m){
    		return afterPersistDBView(m);
    	}
		@Override
		public void act(M m) {
			save(m, getModelClass());
		}

		@Override
		public View error(M m) {
			ModelEditView<M> errorView = null;
			if (m.getRawRecord().isNewRecord()){
    			errorView = createBlankView(getPath().createRelativePath("blank"),m,"save");
    		}else {
    			errorView = createModelEditView(getPath().createRelativePath("edit/" + m.getId()), m,"save");
    		}
	    	return errorView;
    	} 
    	
    }
    private View  saveModelFromForm(){
    	return performPostAction(new SaveAction());
    }

	protected View performPostAction(Action<M> action){
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
        MultiException dataValidationExceptions = new MultiException("Invalid input: ");
        boolean hasUserModifiedData = false; 
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = setableFields.contains(name) && !reflector.isHouseKeepingField(name) ? name : null;
            if (fieldName != null){
            	try {
            		validateEnteredData(fieldName, formFields);
            	}catch (Exception ex){
            		dataValidationExceptions.add(ex);
            		hasUserModifiedData = true;
            	}
            	Object value = formFields.get(fieldName);
	            Class<?> fieldClass = reflector.getFieldGetter(fieldName).getReturnType();
	            if (value == null && (Reader.class.isAssignableFrom(fieldClass) || InputStream.class.isAssignableFrom(fieldClass))){
	            	continue;
	            }
                reflector.set(record, fieldName, value);
            }else if ( name.startsWith("_SUBMIT")){
            	buttonName = name;
            }else if ( name.startsWith("_FORM_DIGEST")){
            	digest = (String)formFields.get("_FORM_DIGEST");
            }
        }
        boolean isNew = record.getRawRecord().isNewRecord();
        hasUserModifiedData = hasUserModifiedData || hasUserModifiedData(formFields,digest);
        if (hasUserModifiedData || isNew){
        	try {
        		if (!dataValidationExceptions.isEmpty()){
        			throw dataValidationExceptions;
        		}
        		action.act(record);
                if (isNew &&  hasUserModifiedData && buttonName.equals("_SUBMIT_MORE") && getPath().canAccessControllerAction("blank",String.valueOf(record.getId()))){
                	//Usability Logic: If user is not modifying data shown, then why be in data entry mode.
                	getPath().addInfoMessage(getModelClass().getSimpleName() + " created sucessfully, press Done when finished.");
            		return clone(record.getId());
                }
        	}catch (RuntimeException ex){
        		if (hasUserModifiedData){
	        		Throwable th = ExceptionUtil.getRootCause(ex);
	        		String message = th.getMessage();
	        		if (message == null){
	        			message = th.toString();
	        		}
            		Database.getInstance().getCurrentTransaction().rollback(th);
	            	getPath().addMessage(StatusType.ERROR, message);
	    	    	View eView = action.error(record);
	    	    	if (eView instanceof HtmlView){
		    	    	return dashboard((HtmlView)eView);
	    	    	}else {
	    	    		return eView;
	    	    	}
        		}
        	}
        	return afterPersistDBView(record);
    	}else {
    		View view = action.noAction(record);
        	if (view instanceof HtmlView){
    	    	return dashboard((HtmlView)view);
	    	}else {
	    		return view;
	    	}
    	}
    }
    protected View afterPersistDBView(M record){
    	View v = null; 
    	if (integrationAdaptor != null){
    		v = integrationAdaptor.createResponse(getPath(),record);
    	}else{
    		v = back();
    	}
        return v;
    }
    
    protected boolean hasUserModifiedData(Map<String,Object> formFields, String oldDigest){
    	StringBuilder hash = null;
        for (String field: reflector.getFields()){
        	if (!formFields.containsKey(field) || !reflector.isFieldSettable(field) || reflector.getFieldProtection(field) == Kind.DISABLED){
        		continue;
        	}
    		Object currentValue = formFields.get(field);

			if (hash != null){
				hash.append(",");
			}else {
				hash = new StringBuilder();
			}
			hash.append(field).append("=").append(StringUtil.valueOf(currentValue));
        }
        String newDigest = hash == null ? null : Encryptor.encrypt(hash.toString());
        return !ObjectUtil.equals(newDigest, oldDigest);
    }

    private void validateEnteredData(String field,  Map<String,Object> formFields){
    	String autoCompleteHelperField = "_AUTO_COMPLETE_"+field;
    	if (!formFields.containsKey(autoCompleteHelperField)){
    		return ;
    	}
    	Object autoCompleteHelperFieldValue = formFields.get(autoCompleteHelperField);
    	Object currentValue = formFields.get(field);

		Method referredModelIdGetter = reflector.getFieldGetter(field);
		Method referredModelGetter = referredModelIdGetter == null ? null : reflector.getReferredModelGetterFor(referredModelIdGetter);
		Class<? extends Model> referredModelClass = referredModelGetter == null ? null : reflector.getReferredModelClass(referredModelGetter);

		if (referredModelClass == null){
			return ;//Defensive. not really needed due to first condition. of check existance of autoCompleteHelperField in formFields.
		}
		ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
		
		String descriptionField = referredModelReflector.getDescriptionField();
		Method descriptionFieldGetter = referredModelReflector.getFieldGetter(descriptionField);

    	
    	Model referredModel = null;

    	String fieldLiteral = referredModelGetter.getName().substring("get".length());
    	
    	if (Database.getJdbcTypeHelper().isVoid(autoCompleteHelperFieldValue)) {
    		if (!Database.getJdbcTypeHelper().isVoid(currentValue)){
    			formFields.put(field, "");
    		}
    		return;
		}
    	
    	//autoCompleteHelperFieldValue is not void.
    	
		if (Database.getJdbcTypeHelper().isVoid(currentValue)){
			@SuppressWarnings({ "rawtypes", "unchecked" })
			List<? extends Model> models = new Select().from(referredModelReflector.getRealModelClass()).where(new Expression(
					referredModelReflector.getColumnDescriptor(descriptionField).getName(),Operator.EQ,autoCompleteHelperFieldValue)).execute(referredModelClass,new Select.AccessibilityFilter());
			
			if (models.size() == 1){
				referredModel = models.get(0);
				currentValue = StringUtil.valueOf(referredModel.getId());
				formFields.put(field, currentValue);
			}
		}else {
			Integer id = (Integer) Database.getJdbcTypeHelper().getTypeRef(Integer.class).getTypeConverter().valueOf(currentValue);
			referredModel = Database.getTable(referredModelClass).get(id);
		}
		
		
		if (referredModel == null){
			throw new RuntimeException("Please choose " + fieldLiteral + " from lookup." );
		}else {
			Object descriptionValue = referredModelReflector.get(referredModel, descriptionField);
			
			String sDescriptionValue = Database.getJdbcTypeHelper().getTypeRef(descriptionFieldGetter.getReturnType()).getTypeConverter().toString(descriptionValue);
			String sAutoCompleteFieldDesc = Database.getJdbcTypeHelper().getTypeRef(descriptionFieldGetter.getReturnType()).getTypeConverter().toString(autoCompleteHelperFieldValue);
			if (!ObjectUtil.equals(sDescriptionValue, sAutoCompleteFieldDesc)){
				throw new RuntimeException("Please choose " + fieldLiteral + " from lookup." );
			}
		}
    }
    
    public View save() {
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        if (integrationAdaptor != null){
        	return saveModelsFromRequest();
        }else {
        	return saveModelFromForm();
        }
    }
    private <T> View saveModelsFromRequest(){
    	List<M> models = integrationAdaptor.readRequest(getPath());
    	for (M m: models){
    		try {
    			save(m, getModelClass());
    		}catch(Exception ex){
    			Database.getInstance().getCache(getReflector()).clear();
    			if (ex instanceof RuntimeException){
    				throw (RuntimeException)ex;
    			}else {
    				throw new RuntimeException(ex);
    			}
    		}
    	}
    	return integrationAdaptor.createResponse(getPath(),models);
    }
    
    public View autocomplete() {
    	ensureUI();
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
				if (reflector.isFieldSettable(fieldName)){
					reflector.set(model,fieldName, ov);
				}
			}else if (fieldName.startsWith("_AUTO_COMPLETE_")){
				autoCompleteFieldName = fieldName.split("_AUTO_COMPLETE_")[1];
				value = StringUtil.valueOf(formData.get(fieldName));
			}
		}
		model.getRawRecord().remove(autoCompleteFieldName);
		
    	Expression where = new Expression(Conjunction.AND);
    	
    	Method autoCompleteFieldGetter = reflector.getFieldGetter(autoCompleteFieldName);
		if (reflector.isAnnotationPresent(autoCompleteFieldGetter,PARTICIPANT.class)){
    		Cache<String,Map<String,List<Integer>>> pOptions = getSessionUser().getParticipationOptions(reflector.getModelClass(),model);
    		PARTICIPANT participant = reflector.getAnnotation(autoCompleteFieldGetter, PARTICIPANT.class);
    		if (pOptions.get(participant.value()).containsKey(autoCompleteFieldName)){
    			List<Integer> autoCompleteFieldValues = pOptions.get(participant.value()).get(autoCompleteFieldName);
    			if (!autoCompleteFieldValues.isEmpty()){
    				autoCompleteFieldValues.remove(null); // We need not try to use null for lookups.
    				where.add(new Expression("ID",Operator.IN,autoCompleteFieldValues.toArray()));
    			}else {
    				where.add(new Expression("ID",Operator.EQ));
    			}
    		}
		}
		
		Class<? extends Model> autoCompleteModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(autoCompleteFieldGetter));
		ModelReflector<? extends Model> autoCompleteModelReflector = ModelReflector.instance(autoCompleteModelClass); 
		Path autoCompletePath = getPath().createRelativePath( LowerCaseStringCache.instance().get(autoCompleteModelReflector.getTableName()) );
		where.add(autoCompletePath.getWhereClause());
		
        return super.autocomplete(autoCompleteModelClass, where, autoCompleteModelReflector.getDescriptionField(), value);
    }
    
    @Override
    protected ImportSheetFilter getDefaultImportSheetFilter(){
    	return new ImportSheetFilter() {
    		
    		@Override
    		public boolean filter(Sheet sheet) {
    			return sheet.getSheetName().equals(StringUtil.pluralize(getModelClass().getSimpleName()));
    		}
    	};
    }

}
