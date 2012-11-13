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
import javax.servlet.http.HttpSession;

import org.apache.lucene.search.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.core.date.DateUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.Unrestricted;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReader;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKey;
import com.venky.swf.db.model.reflection.uniquekey.UniqueKeyFieldDescriptor;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.path.Path.ModelInfo;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.login.LoginView;
import com.venky.swf.views.model.XLSLoadView;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

/**
 *
 * @author venky
 */
public class Controller {
    protected Path path;

    public Path getPath() {
        return path;
    }
    
    public Controller(Path path){
        this.path = path ;
    }
    @Unrestricted
    public View login(){
        if (getPath().getRequest().getMethod().equals("GET") && getPath().getSession() == null ) {
            return createLoginView();
        }else if (getPath().getSession() != null){
        	if ( getSessionUser() == null ) {
        		return  createLoginView();
        	}else {
        		return new RedirectorView(getPath(), "dashboard");
        	}
		}else{ 
			return authenticate();
        }
    }

    protected View authenticate(){
        String username = getPath().getRequest().getParameter("name");
        String password = getPath().getRequest().getParameter("password");
        User user = getUser(username);
        if (user != null && user.authenticate(password)){
            HttpSession newSession = getPath().getRequest().getSession(true);
            newSession.setAttribute("user", user);
            return new RedirectorView(getPath(), "dashboard");
        }else {
        	HtmlView lView = createLoginView();
        	lView.setStatus(StatusType.ERROR, "Login incorrect");
            return lView;
        }
    }
	protected final HtmlView createLoginView(StatusType statusType, String text){
		invalidateSession();
		HtmlView lv = createLoginView(); 
		lv.setStatus(statusType, text);
		return lv;
	}

	protected void invalidateSession(){
		try {
			if (path.getSession() != null) {
				path.getSession().invalidate();
				path.setSession(null);
			}
		}catch (Exception ex){
			//ensure session is invalid.
		}
	}
    protected HtmlView createLoginView(){
    	invalidateSession();
    	return new LoginView(getPath());
    }
    
	@SuppressWarnings("unchecked")
	public <U extends User> U getSessionUser(){
    	return (U)getPath().getSessionUser();
    }
    
    //Can be cast to any user model class as the proxy implements all the user classes.
    protected User getUser(String username){
        Select q = new Select().from(User.class);
        String nameColumn = ModelReflector.instance(User.class).getColumnDescriptor("name").getName();
        q.where(new Expression(nameColumn,Operator.EQ,new BindVariable(username)));
        
		List<? extends User> users  = q.execute(User.class);
        if (users.size() == 1){
        	return users.get(0);
        }
        return null;
    }
    
    @Unrestricted
    public View logout(){
        invalidateSession();
        return new RedirectorView(getPath(), "login");
    }

    public View index(){
        return new RedirectorView(getPath(), "dashboard");
    }

    public DashboardView dashboard(){
        DashboardView dashboard = new DashboardView(getPath());
        return dashboard;
    }
    
    protected DashboardView dashboard(HtmlView aContainedView){
        DashboardView dashboard = dashboard();
        dashboard.addChildView(aContainedView);
        return dashboard;
    }

    @Unrestricted
    public View resources(String name) throws IOException{
    	Path p = getPath();
    	if (name.startsWith("/config/")){
    		return new BytesView(p, "Access Denied!".getBytes());
    	}
    	
        
        InputStream is = getClass().getResourceAsStream(name);
        
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte [] buffer = new byte[1024];
        int read = 0 ;
        try {
        	if (is != null){
	            while ((read = is.read(buffer)) >= 0){ 
	                baos.write(buffer,0,read);
	            }
        	}else {
        		return new BytesView(p, "No such resource!".getBytes());
        	}
        }catch (IOException ex){
            //
        }

        p.getResponse().setDateHeader("Expires", DateUtils.addHours(System.currentTimeMillis(), 24*365*15));
        return new BytesView(getPath(), baos.toByteArray());
    }
    
    public <M extends Model> View autocomplete(Class<M> modelClass, Expression baseWhereClause, String fieldName ,String value,boolean isNullable){
        XMLDocument doc = new XMLDocument("entries");
        XMLElement root = doc.getDocumentRoot();
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        ColumnDescriptor fd = reflector.getColumnDescriptor(fieldName);

        if (isNullable){
        	createEntry(root, "-Not Selected-", " ");
        }

        String columnName = fd.getName();

        Expression where = new Expression(Conjunction.AND);
        where.add(baseWhereClause);
        
        if (reflector.getIndexedColumns().contains(columnName) && !ObjectUtil.isVoid(value)){
        	LuceneIndexer indexer = LuceneIndexer.instance(reflector);
        	Query q = indexer.constructQuery(columnName +":" + value +"*");
        	List<Integer> top10Records = indexer.findIds(q, 20);
        	if (!top10Records.isEmpty()){
            	Expression idExpression = Expression.createExpression("ID", Operator.IN, top10Records.toArray());
            	where.add(idExpression);
        	}else {
        		where.add(new Expression(columnName,Operator.LK,new BindVariable("%"+value+"%")));
        	}
        }else{
	        where.add(new Expression(columnName,Operator.LK,new BindVariable("%"+value+"%")));
        }
        Select q = new Select().from(modelClass);
        q.where(where);
        List<M> records = q.execute(modelClass);
        Method fieldGetter = reflector.getFieldGetter(fieldName);
        TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(fieldGetter.getReturnType()).getTypeConverter();
        
        for (M record:records){
            try {
            	createEntry(root,converter.toString(fieldGetter.invoke(record)),record.getId());
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return new BytesView(path, String.valueOf(doc).getBytes());
    }
    private void createEntry(XMLElement root,Object name, Object id){
        XMLElement elem = root.createChildElement("entry");
        elem.setAttribute("name", name);
        elem.setAttribute("id", id);
    }

    protected Map<String,Object> getFormFields(){
    	return getPath().getFormFields();
    }
    
    protected List<Sheet> getSheetsToImport(Workbook book){
    	List<Sheet> sheets = new ArrayList<Sheet>();
    	for (int i =  0; i < book.getNumberOfSheets() ; i ++ ){
			Sheet sheet = book.getSheetAt(i);
    		Table<? extends Model> table = getTable(sheet);
			if (table != null){
				sheets.add(sheet);
			}
    	}
    	return sheets;
    }

    public View importxls(){
        HttpServletRequest request = getPath().getRequest();

        if (request.getMethod().equalsIgnoreCase("GET")) {
        	return dashboard(new XLSLoadView(getPath()));
        }else {
        	Map<String,Object> formFields = getFormFields();
        	if (!formFields.isEmpty()){
        		InputStream in = (InputStream)formFields.get("datafile");
        		List<ModelReflector<? extends Model>> modelReflectorsOfImportedTables = new ArrayList<ModelReflector<? extends Model>>();
        		try {
	    			Workbook book = new HSSFWorkbook(in);
	    			for (Sheet sheet : getSheetsToImport(book)){ 
	    				Table<? extends Model> table = getTable(sheet);
	    				if (table == null){
	    					continue;
	    				}
	    				try {
	    					modelReflectorsOfImportedTables.add(table.getReflector());
	    					importxls(sheet, table.getModelClass());
	    				} catch (Exception e) {
	    					for (ModelReflector<? extends Model> ref : modelReflectorsOfImportedTables){
		    					Database.getInstance().getCache(ref).clear();
	    					}
	    					if (!(e instanceof RuntimeException)){
	    						throw new RuntimeException(e);
	    					}else {
	    						throw (RuntimeException)e;
	    					}
	    				}
					}
        		}catch (IOException ex){
        			throw new RuntimeException(ex);
        		}
        	}
        	return back();
        }
        
    }

    @Unrestricted
    public RedirectorView back(){
    	RedirectorView v = new RedirectorView(getPath());
    	v.setRedirectUrl(getPath().getBackTarget());
    	return v;
    }

    protected <M extends Model> Table<M> getTable(Sheet sheet){
		String tableName = StringUtil.underscorize(sheet.getSheetName()); 
		Table<M> table = Database.getTable(tableName);
		return table;
    }
    
    protected <M extends Model> void importxls(Sheet sheet, Class<M> modelClass){
		ModelReader<M> modelReader = new ModelReader<M>(modelClass);
		ModelReflector<M> modelReflector = ModelReflector.instance(modelClass);
		for (M m : modelReader.read(sheet)){
			M preExistingRecord  = null;
			if (m.getId() > 0){
				preExistingRecord = Database.getTable(modelClass).get(m.getId());
			}else {
				for (UniqueKey<M> key : modelReflector.getUniqueKeys()){
					Expression where =  new Expression(Conjunction.AND);
					for (UniqueKeyFieldDescriptor<M> fieldDescriptor : key.getFields()){
						Object value = modelReflector.get(m,fieldDescriptor.getFieldName());
						
						if (value != null){
							where.add(new Expression(fieldDescriptor.getFieldName(),Operator.EQ, value));
						}else {
							where.add(new Expression(fieldDescriptor.getFieldName(),Operator.EQ));
						}
					}
					List<M> recordsWithMatchingUK = new Select().from(modelClass).where(where).execute(modelClass);
					if (recordsWithMatchingUK.size() > 1){
						throw new RuntimeException("Found multiple records for key attributes passed, Please use the id column");
					}else if (recordsWithMatchingUK.size() == 1){
						preExistingRecord = recordsWithMatchingUK.get(0);
					}
				}
			}
			if (preExistingRecord != null){
				for (String fieldName : m.getRawRecord().getDirtyFields()){
					preExistingRecord.getRawRecord().put(fieldName, m.getRawRecord().get(fieldName));
				}
				importRecord(preExistingRecord, modelClass);
			}else {
				importRecord(m,modelClass);
			}
		}
    	
    }

    
    protected <M extends Model> void importRecord(M record, Class<M> modelClass){
    	fillDefaultsForReferenceFields(record,modelClass);
    	save(record,modelClass);
    }
    
    protected <M extends Model> void save(M record, Class<M> modelClass){
        if (record.getRawRecord().isNewRecord()){
        	record.setCreatorUserId(getSessionUser().getId());
        	record.setCreatedAt(null);
    	}
        record.setUpdaterUserId(getSessionUser().getId());
        record.setUpdatedAt(null);

    	if (record.isAccessibleBy(getSessionUser(),modelClass)){
            record.save();
        }else {
        	throw new AccessDeniedException();
        }
    	
    }

    protected <M extends Model> void fillDefaultsForReferenceFields(M record,Class<M> modelClass){
        List<ModelInfo> modelElements = getPath().getModelElements();
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
		for (Method referredModelGetter: reflector.getReferredModelGetters()){
	    	@SuppressWarnings("unchecked")
			Class<? extends Model> referredModelClass = (Class<? extends Model>)referredModelGetter.getReturnType();
	    	String referredModelIdFieldName =  reflector.getReferenceField(referredModelGetter);
	    	if (!reflector.isFieldSettable(referredModelIdFieldName) || reflector.isHouseKeepingField(referredModelIdFieldName)){
	    		continue;
	    	}
	    	Method referredModelIdSetter =  reflector.getFieldSetter(referredModelIdFieldName);
	    	Method referredModelIdGetter =  reflector.getFieldGetter(referredModelIdFieldName);
	    	try {
				Integer oldValue = (Integer) referredModelIdGetter.invoke(record);
				if (!Database.getJdbcTypeHelper().isVoid(oldValue)){
					continue;
				}
				List<Integer> idoptions = getSessionUser().getParticipationOptions(modelClass).get(referredModelIdFieldName);
				Integer id = null; 
						
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
    }

}
