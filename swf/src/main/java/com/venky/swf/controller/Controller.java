/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import static com.venky.core.log.TimerStatistics.Timer.startTimer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.search.Query;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.core.date.DateUtils;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.io.xls.XLSModelReader;
import com.venky.swf.db.model.io.xls.XLSModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
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
import com.venky.swf.views.model.FileUploadView;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

/**
 *
 * @author venky
 */ 
public class Controller {
    public static final int MAX_LIST_RECORDS = 30 ;
    protected Path path;


    public Path getPath() {
        return path; 
    }
    
    public Controller(Path path){
        this.path = path ;
    }
    
    public View tron(String loggerName){
    	Config.instance().getLogger(StringUtil.valueOf(loggerName)).setLevel(Level.ALL);
    	return back();
    }
    
    public View troff(String loggerName){
		Config.instance().getLogger(StringUtil.valueOf(loggerName)).setLevel(Level.OFF);
		return back();
    }
    @RequireLogin(false)
    public View login(){
        if (getPath().getRequest().getMethod().equals("GET") && getPath().getSession() == null ) {
            return createLoginView();
        }else if (getPath().getSession() != null){
        	if ( getSessionUser() == null ) {
        		return  createLoginView();
        	}else {
        		return new RedirectorView(getPath(), loginSuccessful());
        	}
		}else{ 
			return authenticate();
        }
    }
    protected String loginSuccessful(){
		String redirectedTo = getPath().getRequest().getParameter("_redirect_to");
		if (ObjectUtil.isVoid(redirectedTo)){
			redirectedTo="dashboard";
		}
		return redirectedTo;
    }

    
    protected View authenticate(){
    	if (getPath().isRequestAuthenticated()){
    		return new RedirectorView(getPath(), loginSuccessful());
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
		path.invalidateSession();
	}
    protected HtmlView createLoginView(){
    	invalidateSession();
    	return new LoginView(getPath());
    }
    
	@SuppressWarnings("unchecked")
	public <U extends User> U getSessionUser(){
    	return (U)getPath().getSessionUser();
    }
    
    @RequireLogin(false)
    public View logout(){
        invalidateSession();
        return new RedirectorView(getPath(), "login");
    }

    @RequireLogin(false)
    public View index(){
        return new RedirectorView(getPath(), "dashboard");
    }

    public DashboardView dashboard(){
        return Controller.dashboard(getPath());
    }
    
    protected DashboardView dashboard(HtmlView aContainedView){
    	return Controller.dashboard(getPath(),aContainedView);
    }

    protected static DashboardView dashboard(Path currentPath){
    	return new DashboardView(currentPath);
    }
    
    
    protected static DashboardView dashboard(Path currentPath, HtmlView aContainedView){
        DashboardView dashboard = dashboard(currentPath);
        dashboard.setChildView(aContainedView);
        return dashboard;
    }

    @RequireLogin(false)
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
        return new BytesView(getPath(), baos.toByteArray(),MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(name));
    }
    public <M extends Model> XMLDocument autocompleteXML(Class<M> modelClass, Expression baseWhereClause, String fieldName ,String value){
        XMLDocument doc = new XMLDocument("entries");
        XMLElement root = doc.getDocumentRoot();
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        ColumnDescriptor fd = reflector.getColumnDescriptor(fieldName);

        String columnName = fd.getName();

        Expression where = new Expression(Conjunction.AND);
        where.add(baseWhereClause);
        
    	int maxRecordsToGet = MAX_LIST_RECORDS;
    	if (!ObjectUtil.isVoid(value)){
	        if (reflector.getIndexedColumns().contains(columnName)){
	        	LuceneIndexer indexer = LuceneIndexer.instance(reflector);
	        	StringBuilder qry = new StringBuilder();
	        	qry.append("( ").append(columnName).append(":").append(value).append("* )");
	        	Query q = indexer.constructQuery(qry.toString());
	        	List<Integer> topRecords = indexer.findIds(q, maxRecordsToGet);
	        	int numRecordRetrieved = topRecords.size();
	        	if (numRecordRetrieved > 0 && numRecordRetrieved < maxRecordsToGet){
	            	Expression idExpression = Expression.createExpression("ID", Operator.IN, topRecords.toArray());
	            	where.add(idExpression);
	        	}else {
	        		where.add(new Expression(columnName,Operator.LK,new BindVariable("%"+value+"%")));
	        	}
	        }else {
	        	where.add(new Expression(columnName,Operator.LK,new BindVariable("%"+value+"%")));
	        }
    	}
        Select q = new Select().from(modelClass);
        q.where(where).orderBy(reflector.getOrderBy());
        List<M> records = q.execute(modelClass,maxRecordsToGet,new DefaultModelFilter<M>(modelClass));
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
        return doc;
    }
    public <M extends Model> View autocomplete(Class<M> modelClass, Expression baseWhereClause, String fieldName ,String value){
    	XMLDocument doc = autocompleteXML(modelClass, baseWhereClause, fieldName, value);
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
    
    private List<Sheet> getSheetsToImport(Workbook book, ImportSheetFilter filter){
    	List<Sheet> sheets = new ArrayList<Sheet>();
    	for (int i =  0; i < book.getNumberOfSheets() ; i ++ ){
			Sheet sheet = book.getSheetAt(i);
			if (filter.filter(sheet)){
				sheets.add(sheet);
			}
    	}
    	return sheets;
    }
    
    protected void importxls(InputStream in,ImportSheetFilter filter){
		List<ModelReflector<? extends Model>> modelReflectorsOfImportedTables = new ArrayList<ModelReflector<? extends Model>>();
		try {
			Workbook book = new HSSFWorkbook(in);
			for (Sheet sheet : getSheetsToImport(book,filter)){ 
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
    public View importxls(){
    	return importxls(getDefaultImportSheetFilter());
    }
    public final View importxls(ImportSheetFilter filter){
        HttpServletRequest request = getPath().getRequest();

        if (request.getMethod().equalsIgnoreCase("GET")) {
        	return dashboard(new FileUploadView(getPath()));
        }else {
        	Map<String,Object> formFields = getFormFields();
        	if (!formFields.isEmpty()){
        		InputStream in = (InputStream)formFields.get("datafile");
        		if (in == null){
        			throw new RuntimeException("Nothing uploaded!");
        		}
        		importxls(in,filter);
        	}
        	return back();
        }
        
    }

    @RequireLogin(false)
    public RedirectorView back(){
    	RedirectorView v = new RedirectorView(getPath());
    	v.setRedirectUrl(getPath().getBackTarget());
    	return v;
    }

    protected static <M extends Model> Table<M> getTable(Sheet sheet){
		String tableName = StringUtil.underscorize(sheet.getSheetName()); 
		Table<M> table = Database.getTable(tableName);
		return table;
    }
    protected <M extends Model> XLSModelReader<M> getXLSModelReader(Class<M> modelClass){
    	return new XLSModelReader<M>(modelClass);
    }

    protected <M extends Model> void importxls(Sheet sheet, Class<M> modelClass){
		XLSModelReader<M> modelReader = getXLSModelReader(modelClass);
		importRecords(modelReader.read(sheet), modelClass);
    }
    
    protected <M extends Model> void importRecords(List<M> records,Class<M> modelClass){
    	for (M m :records){
    		importRecord(m, modelClass);
    	}
    }
    
    protected <M extends Model> void importRecord(M record, Class<M> modelClass){
    	getPath().fillDefaultsForReferenceFields(record,modelClass);
    	save(record,modelClass);
    }
    

    protected <M extends Model> void save(M record, Class<M> modelClass) {
        if (record.getRawRecord().isNewRecord()){
        	record.setCreatorUserId(getSessionUser().getId());
        	record.setCreatedAt(null);
    	}
        if (record.isDirty()){
            record.setUpdaterUserId(getSessionUser().getId());
            record.setUpdatedAt(null);
        }
        record.save(); //Allow extensions to fill defaults etc.
    	
    	if (!record.isAccessibleBy(getSessionUser()) || 
    			!getPath().getModelAccessPath(modelClass).canAccessControllerAction("save",String.valueOf(record.getId()))){
    		Database.getInstance().getCache(ModelReflector.instance(modelClass)).clear();
    		throw new AccessDeniedException();	
		}
	}




    public View exportxls(){
    	Map<String,Table<? extends Model>> tables = Database.getTables();
    	Workbook wb = new HSSFWorkbook();
    	for (String tableName: tables.keySet()){
    		Table<? extends Model> table = tables.get(tableName); 
    		EXPORTABLE exportable = table.getReflector().getAnnotation(EXPORTABLE.class);
    		if (table.isReal() && (exportable == null || exportable.value())) {
    			Config.instance().getLogger(getClass().getName()).info("Exporting:" + table.getTableName());
    			exportxls(table.getModelClass(), wb);
    		}
    	}
    	try {
    		String baseFileName =  "db"+ new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(System.currentTimeMillis())) ;

    		ByteArrayOutputStream os = new ByteArrayOutputStream();
    		
    		ZipOutputStream zos = new ZipOutputStream(os);
    		zos.putNextEntry(new ZipEntry(baseFileName + ".xls"));
    		wb.write(zos);
    		zos.closeEntry();
    		zos.close();

    		return new BytesView(getPath(), os.toByteArray(),MimeType.APPLICATION_ZIP,"content-disposition", "attachment; filename=" + baseFileName + ".zip");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    protected class DefaultModelFilter<M extends Model> implements Select.ResultFilter<M> {
    	Select.AccessibilityFilter<M> defaultFilter = new Select.AccessibilityFilter<M>();
    	Class<M> modelClass = null;
		public DefaultModelFilter(Class<M> modelClass) {
			super();
			this.modelClass = modelClass;
		}
		@Override
		public boolean pass(M record) {
			Timer timer = startTimer("DefaultModelFilter.pass",Config.instance().isTimerAdditive());
			try {
				return defaultFilter.pass(record) && getPath().getModelAccessPath(modelClass).canAccessControllerAction("index",
						StringUtil.valueOf(record.getId()));
			}finally{
				timer.stop();
			}
		}
    }
    

    protected <M extends Model> void exportxls(Class<M> modelClass,Workbook wb){
    	ModelReflector<M> reflector = ModelReflector.instance(modelClass);
    	List<String> fieldsIncluded = reflector.getFields();
		Iterator<String> fieldIterator = fieldsIncluded.iterator();
		int numUniqueKeys = reflector.getUniqueKeys().size(); 
		while (fieldIterator.hasNext()){
			String field = fieldIterator.next();
			EXPORTABLE exportable = reflector.getAnnotation(reflector.getFieldGetter(field), EXPORTABLE.class);
			if (exportable != null){
				if (!exportable.value()){
					fieldIterator.remove();
				}
			}else if (reflector.isHouseKeepingField(field)){
				if (!field.equals("ID") || numUniqueKeys > 0){
					fieldIterator.remove();
				}
			}
		}
		exportxls(modelClass, wb, fieldsIncluded);
    }
    protected <M extends Model> void exportxls(Class<M> modelClass,Workbook wb , List<String> fieldsIncluded){
    	List<M> list = new Select().from(modelClass).where(getPath().getWhereClause(modelClass)).execute(modelClass,new DefaultModelFilter<M>(modelClass));
		getXLSModelWriter(modelClass).write(list, wb,fieldsIncluded);
    }
    protected <M extends Model> XLSModelWriter<M> getXLSModelWriter(Class<M> modelClass){
    	return new XLSModelWriter<M>(modelClass); 
    }

    public static interface ImportSheetFilter {
    	public boolean filter(Sheet sheet);
    }
    
    protected ImportSheetFilter getDefaultImportSheetFilter(){
    	return new ImportSheetFilter(){

    		@Override
    		public boolean filter(Sheet sheet) {
        		Table<? extends Model> table = getTable(sheet);
    			if (table != null){
    				return true;
    			}
    			return false;
    		} 
        	
        };
    }
    
}
