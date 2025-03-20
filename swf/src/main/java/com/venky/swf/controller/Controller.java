/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.entity.Actions;
import com.venky.swf.db.model.entity.ChildEntity;
import com.venky.swf.db.model.entity.ChildEntity.ChildEntities;
import com.venky.swf.db.model.entity.Entity;
import com.venky.swf.db.model.entity.Field;
import com.venky.swf.db.model.entity.Fields;
import com.venky.swf.db.model.entity.Strings;
import com.venky.swf.db.model.io.xls.XLSModelReader;
import com.venky.swf.db.model.io.xls.XLSModelReader.RecordVisitor;
import com.venky.swf.db.model.io.xls.XLSModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.status.ServerStatus;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.IOTask;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.Router;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.util.TemplateProcessor;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.layout.Pre;
import com.venky.swf.views.login.LoginView;
import com.venky.swf.views.login.LoginView.LoginContext;
import com.venky.swf.views.model.FileUploadView;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.owasp.encoder.Encode;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author venky
 */
public class Controller implements TemplateLoader{

    public static final int MAX_LIST_RECORDS = 30;
    protected Path path;
    
    public Controller(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public View tron(String loggerName) {
        Config.instance().getLogger(StringUtil.valueOf(loggerName)).setLevel(Level.ALL);
        return back();
    }

    public View troff(String loggerName) {
        Config.instance().getLogger(StringUtil.valueOf(loggerName)).setLevel(Level.OFF);
        return back();
    }

    public View reset_router() {
        if (Config.instance().isDevelopmentEnvironment()) {
            TaskManager.instance().executeAsync((IOTask)()->Router.instance().reset(),false);
        }
        return back();
    }
    private String getLoginUrlParams(){
        StringBuilder msg = new StringBuilder();
        Map<String, Object> fields = getPath().getFormFields();
        getPath().getErrorMessages().forEach(m -> msg.append(m));
        if (msg.length() == 0){
            msg.append(fields.getOrDefault("error",""));
        }
        if (getPath().getProtocol() == MimeType.TEXT_HTML){
            if (msg.length() >0){
                msg.insert(0,"?error=");
            }

            fields.forEach((k,v)->{
                if ("name".equals(k) || k.startsWith("password") ||  k.equals("_LOGIN") || k.equals("_REGISTER") || k.equals("_RESET")){
                    return;
                }
                if (msg.length() >0){
                    msg.append("&");
                }else {
                    msg.append("?");
                }
                msg.append(String.format("%s=%s",k, Encode.forUriComponent(v.toString())));
            });

        }
        return msg.toString();
    }

    @RequireLogin(false)
    public View register(){
        return login();
    }
    @RequireLogin(false)
    public View login() {
        Map<String, Object> fields = getPath().getFormFields();
        if (getPath().getRequest().getMethod().equals("GET") && getSessionUser() == null) {
            boolean isLoginOverridden = !getClass().getSimpleName().equals("Controller") && getPath().action().equals("login");

            if (!isLoginOverridden) {
                return createLoginView();
            } else {
                return authenticate();
            }
        } else if (getPath().getSession() != null) {
            if (getSessionUser() == null) {
                String msg = getLoginUrlParams();
                if (getPath().getProtocol() == MimeType.TEXT_HTML){
                    //return createLoginView();
                    return new RedirectorView(getPath(),getPath().action() + msg);
                }else if (msg.isEmpty()){
                    return authenticate();
                }else {
                    throw new AccessDeniedException(msg);
                }
            } else {
                if (getPath().getProtocol() == MimeType.TEXT_HTML){
                    return new RedirectorView(getPath(), loginSuccessful(), "");
                }else {
                    return authenticate();
                }
            }
        } else {
            return authenticate();
        }
    }

    protected String loginSuccessful() {
        String redirectedTo = getPath().getRequest().getParameter("_redirect_to");
        return loginSuccessful(redirectedTo == null ? "" : redirectedTo);
    }

    protected String loginSuccessful(String redirectedTo) {
        if (ObjectUtil.isVoid(redirectedTo)) {
            redirectedTo = "dashboard";
        }
        return redirectedTo;
    }

    protected View authenticate() {
        boolean authenticated = getPath().isRequestAuthenticated();
        if (getPath().getProtocol() == MimeType.TEXT_HTML) {
            if (authenticated) {
                return new RedirectorView(getPath(), "", loginSuccessful());
            } else {
                String msg = getLoginUrlParams();

                return new RedirectorView(getPath(),"/",getPath().action()+msg);
                //return createLoginView(StatusType.ERROR, msg.toString());
            }
        } else {
            IntegrationAdaptor<User, ?> adaptor = IntegrationAdaptor.instance(User.class, FormatHelper.getFormatClass(getPath().getProtocol()));
            if (adaptor == null) {
                throw new RuntimeException(" content-type in request header should be " + MimeType.APPLICATION_JSON + " or " + MimeType.APPLICATION_XML);
            }
            if (authenticated) {
                return adaptor.createResponse(getPath(), (User) getSessionUser(), Arrays.asList("ID", "NAME", "API_KEY"));
            } else {
                throw new AccessDeniedException("Login incorrect!");
            }
        }
    }

    protected final HtmlView createLoginView(StatusType statusType, String text) {
        invalidateSession();
        HtmlView lv = createLoginView();
        lv.setStatus(statusType, text);
        return lv;
    }

    protected void invalidateSession() {
        path.invalidateSession();
    }

    protected HtmlView createLoginView() {
        if (getPath().action().equals("register")) {
            return createLoginView(LoginContext.REGISTER);
        } else if (getPath().action().equals("reset_password")){
            return createLoginView(LoginContext.PASSWORD_RESET);
        }else {
            return createLoginView(LoginContext.LOGIN);
        }
    }

    protected HtmlView createLoginView(LoginContext context) {
        invalidateSession();
        return new LoginView(getPath(), context);
    }

    @SuppressWarnings("unchecked")
    public <U extends User> U getSessionUser() {
        return (U) getPath().getSessionUser();
    }

    @RequireLogin(false)
    public View logout() {
        invalidateSession();
        String redirect_to = (String)getPath().getFormFields().get("_redirect_to");
        if (ObjectUtil.isVoid(redirect_to)){
            redirect_to = "/";
        }
        
        return new RedirectorView(getPath(), redirect_to, "");
    }

    public View dashboard() {
        if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/dashboard.html")){
            return  html("dashboard");
        }else {
            return Controller.dashboard(getPath());
        }
    }

    public DashboardView dashboard(HtmlView aContainedView) {
        return Controller.dashboard(getPath(), aContainedView);
    }

    protected static DashboardView dashboard(Path currentPath) {
        return new DashboardView(currentPath);
    }

    protected static DashboardView dashboard(Path currentPath, HtmlView aContainedView) {
        DashboardView dashboard = dashboard(currentPath);
        dashboard.setChildView(aContainedView);
        return dashboard;
    }

    @RequireLogin(false)
    public View resources(String name) throws IOException {
        return resources(name,null);
    }
    @RequireLogin(false)
    public View resources(String name, View defaultView) throws IOException {
        Path p = getPath();
        if (name.startsWith("/config/")) {
            return new BytesView(p, "Access Denied!".getBytes());
        }

        ByteArrayInputStream is = p.getResourceAsStream(name);
        if (is == null){
            String resource = String.format("%s/%s",Config.instance().getHostName(),name);
            View cached = getCachedResult(getPath().constructNewPath(String.format("/resources/%s",resource)));
            if (cached != null) {
                return cached;
            }
            is = p.getResourceAsStream(resource);
        }
        if (is == null){
            if (defaultView != null){
                return defaultView;
            }else {
                throw new AccessDeniedException("No such resource!");
            }
        }

        return new BytesView(getPath(), StringUtil.readBytes(is), MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(name));
    }

    public <M extends Model> JSONObject autocompleteJSON(Class<M> modelClass, Expression baseWhereClause, String fieldName, String value) {
            FormatHelper<JSONObject> fh = FormatHelper.instance(MimeType.APPLICATION_JSON, "entries", true);

        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        ColumnDescriptor fd = reflector.getColumnDescriptor(fieldName);

        String columnName = fd.getName();

        Expression where = new Expression(reflector.getPool(), Conjunction.AND);

        //where.add(baseWhereClause); baseWhereClause may have virtual columns,
        int maxRecordsToGet = MAX_LIST_RECORDS;
        if (!ObjectUtil.isVoid(value)) {
            if (reflector.getIndexedColumns().contains(columnName)) {
                LuceneIndexer indexer = LuceneIndexer.instance(reflector);
                StringBuilder qry = new StringBuilder();
                qry.append("( ");
                StringTokenizer tok = new StringTokenizer(QueryParser.escape(value));
                while (tok.hasMoreElements()){
                    qry.append(columnName).append(":");
                    qry.append(tok.nextElement());
                    qry.append("*");
                    if (tok.hasMoreElements()){
                        qry.append(" AND ");
                    }
                }

                qry.append(")");
                Query q = indexer.constructQuery(qry.toString());
                List<Long> topRecords = indexer.findIds(q, maxRecordsToGet);
                int numRecordRetrieved = topRecords.size();
                if (numRecordRetrieved > 0) {
                    Expression idExpression = Expression.createExpression(reflector.getPool(), "ID", Operator.IN, topRecords.toArray());
                    where.add(idExpression);
                } else {
                    return fh.getRoot();
                }
            } else if (!reflector.isFieldVirtual(fieldName)) {
                where.add(new Expression(reflector.getPool(), reflector.getJdbcTypeHelper().getLowerCaseFunction() + "(" + columnName + ")", Operator.LK, new BindVariable(reflector.getPool(), "%" + value.toLowerCase() + "%")));
            }else {
                maxRecordsToGet = Select.MAX_RECORDS_ALL_RECORDS;
            }
        }
        Select q = new Select().from(modelClass);
        q.where(where).orderBy(reflector.getOrderBy());
        List<M> records = q.execute(modelClass, maxRecordsToGet, new DefaultModelFilter<M>(modelClass));
        Iterator<M> i = records.iterator();
        while (i.hasNext()) {
            M m = i.next();
            if (!baseWhereClause.eval(m)) {
                i.remove();
            } else if (reflector.isFieldVirtual(fieldName)) {
                String fieldValue = reflector.get(m, fieldName);
                if (!ObjectUtil.isVoid(value) ) {
                    StringTokenizer tokenizer = new StringTokenizer(value);
                    boolean pass = true;
                    while (pass && tokenizer.hasMoreElements()){
                        pass = fieldValue.toLowerCase().contains(tokenizer.nextToken().toLowerCase());
                    }
                    if (!pass) {
                        i.remove();
                    }
                }
            }
        }
        Method fieldGetter = reflector.getFieldGetter(fieldName);
        TypeConverter<?> converter = Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(fieldGetter.getReturnType()).getTypeConverter();

        for (M record : records) {
            try {
                createEntry(reflector, fh, converter.toString(fieldGetter.invoke(record)), record.getId());
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        return fh.getRoot();
    }

    public <M extends Model> View autocomplete(Class<M> modelClass, Expression baseWhereClause, String fieldName, String value) {
        JSONObject doc = autocompleteJSON(modelClass, baseWhereClause, fieldName, value);
        Config.instance().getLogger(getClass().getName()).info(doc.toString());
        return new BytesView(path, String.valueOf(doc).getBytes(), MimeType.APPLICATION_JSON);
    }

    private void createEntry(ModelReflector<? extends Model> reflector, FormatHelper<JSONObject> doc, Object name, Object id) {
        JSONObject elem = doc.createArrayElement("entry");
        FormatHelper<JSONObject> elemHelper = FormatHelper.instance(elem);
        elemHelper.setAttribute("name", Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(name.getClass()).getTypeConverter().toString(name));
        elemHelper.setAttribute("id", Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(id.getClass()).getTypeConverter().toString(id));
    }

    protected Map<String, Object> getFormFields() {
        return getPath().getFormFields();
    }

    private List<Sheet> getSheetsToImport(Workbook book, ImportSheetFilter filter) {
        List<Sheet> sheets = new ArrayList<Sheet>();
        for (int i = 0; i < book.getNumberOfSheets(); i++) {
            Sheet sheet = book.getSheetAt(i);
            if (filter.filter(sheet)) {
                sheets.add(sheet);
            }
        }
        return sheets;
    }

    protected void importxls(InputStream in, ImportSheetFilter filter) {
        List<ModelReflector<? extends Model>> modelReflectorsOfImportedTables = new ArrayList<ModelReflector<? extends Model>>();
        Workbook book = null;
        try {
            book = new XSSFWorkbook(in);
            for (Sheet sheet : getSheetsToImport(book, filter)) {
                Table<? extends Model> table = getTable(sheet);
                if (table == null) {
                    continue;
                }
                Config.instance().getLogger(getClass().getName()).info("Importing:" + table.getTableName());
                try {
                    modelReflectorsOfImportedTables.add(table.getReflector());
                    importxls(sheet, table.getModelClass());
                } catch (Exception e) {
                    for (ModelReflector<? extends Model> ref : modelReflectorsOfImportedTables) {
                        Database.getInstance().getCache(ref).clear();
                    }
                    if (!(e instanceof RuntimeException)) {
                        throw new RuntimeException(e);
                    } else {
                        throw (RuntimeException) e;
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (book != null) {
                    book.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public View importxls() {
        return importxls(getDefaultImportSheetFilter());
    }

    public final View importxls(ImportSheetFilter filter) {
        HttpServletRequest request = getPath().getRequest();

        if (request.getMethod().equalsIgnoreCase("GET")) {
            return dashboard(new FileUploadView(getPath()));
        } else {
            Map<String, Object> formFields = getFormFields();
            if (!formFields.isEmpty()) {
                InputStream in = (InputStream) formFields.get("datafile");
                if (in == null) {
                    throw new RuntimeException("Nothing uploaded!");
                }
                importxls(in, filter);
            }
            return back();
        }

    }

    @RequireLogin(false)
    public RedirectorView back() {
        RedirectorView v = new RedirectorView(getPath());
        v.setRedirectUrl(getPath().getBackTarget());
        return v;
    }



    protected static <M extends Model> Table<M> getTable(Sheet sheet) {
        String tableName = StringUtil.underscorize(sheet.getSheetName());
        Table<M> table = Database.getTable(tableName);
        return table;
    }

    protected <M extends Model> XLSModelReader<M> getXLSModelReader(Class<M> modelClass) {
        return new XLSModelReader<M>(modelClass);
    }

    protected <M extends Model> void importxls(Sheet sheet, Class<M> modelClass) {
        XLSModelReader<M> modelReader = getXLSModelReader(modelClass);
        boolean raw = ModelReflector.instance(modelClass).getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(getFormFields().get("raw"));
        if (!raw){
            importRecords(modelReader.read(sheet), modelClass);
        }else {
            RecordVisitor<M> visitor =  new RecordVisitor<M>() {
                List<Task> tasks = new ArrayList<>();
                @Override
                public void visit(M m) {
                    if (m != null) {
                        tasks.add(new ImportTask<>(m));
                    }
                    if (m == null || tasks.size() >= 200){
                        AsyncTaskManagerFactory.getInstance().addAll(tasks);
                        tasks.clear();
                    }
                }
            };
            modelReader.read(sheet, visitor);
            visitor.visit(null);
            TaskManager.instance().executeAsync(new ReindexTask<>(modelClass), false);
        }
    }

    protected <M extends Model> void importRecords(List<M> records, Class<M> modelClass) {
        long recordCount = Database.getTable(modelClass).recordCount();

        if (records.size() > recordCount) {
            LuceneIndexer.instance(modelClass).setIndexingEnabled(false);
            TaskManager.instance().executeAsync(new ReindexTask<>(modelClass), false);//This will be executed post commit.
        }
        for (M m : records) {
            importRecord(m, modelClass);
        }
    }

    protected <M extends Model> void importRecord(M record, Class<M> modelClass) {
        getPath().fillDefaultsForReferenceFields(record, modelClass);
        save(record, modelClass);
    }

    protected <M extends Model> void save(M record){
        save(record,record.getReflector().getModelClass());
    }
    protected <M extends Model> void save(M record, Class<M> modelClass) {
        if (record.getRawRecord().isNewRecord()) {
            record.setCreatorUserId(getPath().getSessionUserId());
            record.setCreatedAt(null);
        }
        if (record.isDirty()) {
            record.setUpdaterUserId(getPath().getSessionUserId());
            record.setUpdatedAt(null);
        }
        record.save(); //Allow extensions to fill defaults etc.

        if (getSessionUser() != null && !getSessionUser().isAdmin() && (!record.isAccessibleBy(getSessionUser())
                || !getPath().getModelAccessPath(modelClass).canAccessControllerAction("save", String.valueOf(record.getId())))) {
            Database.getInstance().getCache(ModelReflector.instance(modelClass)).clear();
            throw new AccessDeniedException();
        }
    }

    public View exportxls() {
        Workbook wb = new XSSFWorkbook();
        for (String pool : ConnectionManager.instance().getPools()) {
            Map<String, Table<? extends Model>> tables = Database.getTables(pool);
            for (String tableName : tables.keySet()) {
                Table<? extends Model> table = tables.get(tableName);
                EXPORTABLE exportable = table.getReflector().getAnnotation(EXPORTABLE.class);
                if (table.isReal() && (exportable == null || exportable.value())) {
                    Config.instance().getLogger(getClass().getName()).info("Exporting:" + table.getTableName());
                    exportxls(table.getModelClass(), wb);
                }
            }
        }
        try {
            String baseFileName = "db" + new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(System.currentTimeMillis()));

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            ZipOutputStream zos = new ZipOutputStream(os);
            zos.putNextEntry(new ZipEntry(baseFileName + ".xlsx"));
            wb.write(zos);
            zos.closeEntry();
            zos.close();

            return new BytesView(getPath(), os.toByteArray(), MimeType.APPLICATION_ZIP, "content-disposition", "attachment; filename=" + baseFileName + ".zip");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    protected final SWFLogger cat = Config.instance().getLogger(getClass().getName());

    protected class DefaultModelFilter<M extends Model> implements Select.ResultFilter<M> {

        Select.AccessibilityFilter<M> defaultFilter = new Select.AccessibilityFilter<M>();
        Class<M> modelClass = null;

        public DefaultModelFilter(Class<M> modelClass) {
            super();
            this.modelClass = modelClass;
        }

        @Override
        public boolean pass(M record) {
            Timer timer = cat.startTimer("DefaultModelFilter.pass", Config.instance().isTimerAdditive());
            try {
                return defaultFilter.pass(record) && getPath().getModelAccessPath(modelClass).canAccessControllerAction("index",
                        StringUtil.valueOf(record.getId()));
            } finally {
                timer.stop();
            }
        }
    }
    protected <M extends Model> void exportxls(Class<M> modelClass, Workbook wb) {
        exportxls(modelClass,wb,null);
    }
    protected <M extends Model> void exportxls(Class<M> modelClass, Workbook wb, List<M> records) {
        ModelReflector<M> reflector = ModelReflector.instance(modelClass);
        List<String> fieldsIncluded = reflector.getFields();
        Iterator<String> fieldIterator = fieldsIncluded.iterator();
        int numUniqueKeys = reflector.getUniqueKeys().size();
        while (fieldIterator.hasNext()) {
            String field = fieldIterator.next();
            EXPORTABLE exportable = reflector.getAnnotation(reflector.getFieldGetter(field), EXPORTABLE.class);
            if (exportable != null) {
                if (!exportable.value()) {
                    fieldIterator.remove();
                }
            } else if (reflector.isHouseKeepingField(field)) {
                //if (!field.equals("ID") || numUniqueKeys > 0){
                if (!field.equals("ID")) {
                    if ((!Config.instance().getBooleanProperty("swf.listview.housekeeping.show", false) && reflector.isHouseKeepingField(field))
                            || !reflector.isFieldVisible(field)) {
                        fieldIterator.remove();
                    }
                }
            }
        }
        exportxls(modelClass, wb, fieldsIncluded,records);
    }

    protected <M extends Model> void exportxls(Class<M> modelClass, Workbook wb, List<String> fieldsIncluded , List<M> records) {
        List<M> list = records ;
        if (list == null){
            list = new Select().from(modelClass).where(getPath().getWhereClause(modelClass)).execute(modelClass, new DefaultModelFilter<M>(modelClass));
        }
        getXLSModelWriter(modelClass).write(list, wb, fieldsIncluded, new HashSet<>(), new HashMap<>());
    }

    protected <M extends Model> XLSModelWriter<M> getXLSModelWriter(Class<M> modelClass) {
        return new XLSModelWriter<M>(modelClass);
    }

    public static interface ImportSheetFilter {

        public boolean filter(Sheet sheet);
    }

    protected ImportSheetFilter getDefaultImportSheetFilter() {
        return new ImportSheetFilter() {

            @Override
            public boolean filter(Sheet sheet) {
                Table<? extends Model> table = getTable(sheet);
                if (table != null) {
                    return true;
                }
                return false;
            }

        };
    }

    public enum CacheOperation{
        SET,
        GET,
        CLEAR,
    }
    public static final String GET_CACHED_RESULT_EXTENSION = "get.cached.result";
    public View getCachedResult(){
        return getCachedResult(getPath());
    }
    public View getCachedResult(Path path){
        ObjectHolder<View> holder = new ObjectHolder<>(null);
        Registry.instance().callExtensions(GET_CACHED_RESULT_EXTENSION,CacheOperation.GET,path,holder);
        View v = holder.get();
        if (v != null) {
            v.setPath(null);
        }
        return v;
    }

    public static final String SET_CACHED_RESULT_EXTENSION = "set.cached.result";
    public void setCachedResult(View view){
        ObjectHolder<View> holder = new ObjectHolder<>(view);
        Registry.instance().callExtensions(SET_CACHED_RESULT_EXTENSION,CacheOperation.SET,getPath(),holder);
    }

    public static final String CLEAR_CACHED_RESULT_EXTENSION = "clear.cached.result";
    public View clearCachedResult(){
        Registry.instance().callExtensions(CLEAR_CACHED_RESULT_EXTENSION,CacheOperation.CLEAR,getPath());
        return new BytesView(getPath(),"OK".getBytes());
    }


    public static class ImportTask<M extends Model> implements Task{
        M model;
        public ImportTask(M model){
            this.model = model;
        }
        @Override
        public void execute() {
            if (model != null){
                try {
                    LuceneIndexer.instance(model.getReflector().getModelClass()).setIndexingEnabled(false);
                    model.save();
                }finally {
                    LuceneIndexer.instance(model.getReflector().getModelClass()).setIndexingEnabled(true);
                }
            }
        }
    }
    public static class ReindexTask<M extends Model> implements Task{

        Class<M> modelClass;
        public  ReindexTask(Class<M> modelClass) {
            this.modelClass = modelClass;
        }

        @Override
        public void execute() {
            if (!LuceneIndexer.instance(modelClass).hasIndexedFields()){
                return;
            }
            List<M> models = new Select().from(modelClass).execute();
            for (M m : models){
                try {
                    LuceneIndexer.instance(modelClass).updateDocument(m.getRawRecord());
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }

        }
    }

    public <M extends Model> Entity meta(Long id,ModelReflector<M> ref){
        Entity entity = new Entity();
        entity.setName(ref.getModelClass().getSimpleName());
        entity.setVirtual(ref.isVirtual());
        entity.setFields(new Fields());
        entity.setActions(new Actions());
        entity.setChildren(new ChildEntities());

        List<String> referenceFields = ref.getReferenceFields();

        for (String f : ref.getFields()){
            Method fieldGetter = ref.getFieldGetter(f);
            ColumnDescriptor descriptor = ref.getColumnDescriptor(f);
            Field field = new Field();
            field.setName(StringUtil.camelize(f));
            field.setJavaClass(fieldGetter.getReturnType().getName());
            field.setNullable(descriptor.isNullable());
            field.setVirtual(descriptor.isVirtual());
            if (referenceFields.contains(f)){
                Method referredModelGetter = ref.getReferredModelGetterFor(fieldGetter);
                field.setReferenceEntityName(referredModelGetter.getReturnType().getSimpleName());
            }
            if (ref.isFieldEnumeration(f)){
                Strings values = new Strings();
                for (String allowedValue : ref.getAllowedValues(f)) {
                    values.add(allowedValue);
                }
                field.setValues(values);
            }
            entity.getFields().add(field);
        }
        for (Method childGetter : ref.getChildGetters()) {
            IS_VIRTUAL isVirtual= childGetter.getAnnotation(IS_VIRTUAL.class);

            Class<? extends Model> childClass = ref.getChildModelClass(childGetter);
            ModelReflector<? extends Model> childRef = ModelReflector.instance(childClass);
            if (childRef.isVirtual()){
                continue;
            }

            CONNECTED_VIA via = ref.getAnnotation(childGetter, CONNECTED_VIA.class);
            String referenceFieldName = null;
            if (via != null){
                referenceFieldName = ref.getFieldName(via.value());
            }else {
                List<Method> referredModelGetters = childRef.getReferredModelGetters(ref.getModelClass());
                if (!referredModelGetters.isEmpty()){
                    referenceFieldName = childRef.getReferenceField(referredModelGetters.get(0));
                }
            }
            if (referenceFieldName != null && !childRef.isFieldVirtual(referenceFieldName)) {
                ChildEntity childEntity = new ChildEntity();
                childEntity.setVirtual(isVirtual != null && isVirtual.value());
                childEntity.setName(childClass.getSimpleName());
                childEntity.setReferenceFields(new Fields());
                Field referenceField = new Field();
                referenceField.setName(StringUtil.camelize(referenceFieldName));
                childEntity.getReferenceFields().add(referenceField);
                entity.getChildren().add(childEntity);
            }
        }

        List<Method> actionMethods = ControllerReflector.instance(getClass()).getMethods(new MethodMatcher() {
            @Override
            public boolean matches(Method method) {
                boolean matches = false;
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length <= 1){
                    matches =  View.class.isAssignableFrom(method.getReturnType());
                    if (matches && parameterTypes.length == 1){
                        matches = (parameterTypes[0] == String.class || Path.isNumberClass(parameterTypes[0]));
                    }
                }
                matches = matches && Modifier.isPublic(method.getModifiers());

                return matches;
                
            }
        });

        for (Method actionMethod : actionMethods) {
            com.venky.swf.db.model.entity.Action action = new com.venky.swf.db.model.entity.Action();
            action.setName(actionMethod.getName());
            action.setRequireLogin(ControllerReflector.instance(getClass()).isSecuredActionMethod(actionMethod));

            SingleRecordAction singleRecordAction = ControllerReflector.instance(getClass()).getAnnotation(actionMethod, SingleRecordAction.class);
            if (singleRecordAction != null){
                if (id == null)
                    action.setAllowed(getPath().canAccessControllerAction(actionMethod.getName()));
                else {
                    action.setAllowed(getPath().canAccessControllerAction(actionMethod.getName(),id.toString()));
                }
                action.setIcon(singleRecordAction.icon());
                action.setToolTip(singleRecordAction.tooltip());
            }else {
                action.setAllowed(getPath().canAccessControllerAction(actionMethod.getName()));
            }
            entity.getActions().add(action);
        }
        return entity;
    }


    public <M extends Model> View erd(ModelReflector<M> ref){
        StringBuilder erd = new StringBuilder("erDiagram\n");
        Set<String> childTables = new SequenceSet<>();
        Set<String> parentTables = new SequenceSet<>();


        ref.getChildModels().forEach(m->childTables.add(ModelReflector.instance(m).getTableName()));


        for (String referenceField : ref.getReferenceFields()) {
            if (ref.isHouseKeepingField(referenceField)){
                continue;
            }
            Class<? extends Model> referredModelClass = ref.getReferredModelClass(ref.getReferredModelGetterFor(ref.getFieldGetter(referenceField)));
            parentTables.add(ModelReflector.instance(referredModelClass).getTableName());
        }
        for (String parentTableName : parentTables){
            Table<? extends Model> parentTable = Database.getTable(parentTableName);
            if (parentTable != null) {
                erd.append(erd(parentTable, new SequenceSet<>()));  // Print fields of parents.
            }
        }

        erd.append(erd(Database.getTable(ref.getModelClass()),parentTables));

        for (String childTableName : childTables){
            Table<? extends Model> childTable = Database.getTable(childTableName);
            if (childTable == null){
                continue;
            }
            erd.append(erd(childTable,new SequenceSet<>(){{
                add(ref.getTableName());
            }}));
        }

        Pre pre = new Pre();
        pre.addClass("mermaid");
        pre.setText(erd.toString());

        return new HtmlView(getPath()) {
            @Override
            protected void createBody(_IControl b) {
                b.addControl(pre);
            }
        };
    }
    public String erd(Table<? extends Model> table,  Set<String> referenceTablesToBeIncluded){
        ModelReflector<? extends Model> ref = table.getReflector();


        StringBuilder fields = new StringBuilder();
        StringBuilder references = new StringBuilder();

        fields.append("\n").append(table.getTableName()).append("{");
        List<String> referenceFields = ref.getReferenceFields();

        ref.getRealFields().forEach(rf->{
            Method fieldGetter = ref.getFieldGetter(rf);
            fields.append("\n\t").append(fieldGetter.getReturnType().getSimpleName()) .append(" ").append(rf);

            UNIQUE_KEY uk= ref.getAnnotation(fieldGetter, UNIQUE_KEY.class);
            StringBuilder comment = new StringBuilder();
            if (uk != null) {
                comment.append(" ").append(uk.value());
            }

            if (referenceFields.contains(rf)) {
                comment.append(" ").append("FK");
                Class<? extends Model> referredModelClass = ref.getReferredModelClass(ref.getReferredModelGetterFor(fieldGetter));
                ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
                if (referenceTablesToBeIncluded.contains(referredModelReflector.getTableName())){
                    references.append("\n").append(referredModelReflector.getTableName()).append("|");

                    if (ref.getColumnDescriptor(rf).isNullable()) {
                        references.append("o--o{ ").append(table.getTableName());
                    } else {
                        references.append("|--o{ ").append(table.getTableName());
                    }
                    references.append(" : has ");
                }
            }

            if (!comment.isEmpty() ) {
                fields.append("\"").append(comment).append("\"");
            }
        });

        fields.append("\n}");
        fields.append(references);
        return fields.toString();
    }

    @RequireLogin(false)
    public View ping(){
        return no_content();
    }
    @RequireLogin(false)
    public View status(){
        ServerStatus status = new ServerStatus();
        AsyncTaskManagerFactory.getInstance().status(status);
        Router.instance().status(status);
        return new BytesView(getPath(),status.toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }



    public View info() throws IOException{
        return info(0L);
    }
    public View info(long id) throws IOException{
        if (!Config.instance().isDevelopmentEnvironment() &&  getPath().getSessionUserId() != 1){
            throw new AccessDeniedException("You cannot view logs!");
        }
        return new BytesView(getPath(),StringUtil.readBytes(new FileInputStream(String.format("tmp/java_info0.log.%d",id)),true),MimeType.TEXT_PLAIN);
    }
    public View warn() throws IOException{
        return warn(0L);
    }
    public View warn(long id) throws IOException{
        if (!Config.instance().isDevelopmentEnvironment()){
            throw new AccessDeniedException("You cannot view logs!");
        }
        return new BytesView(getPath(),StringUtil.readBytes(new FileInputStream(String.format("tmp/java_warn0.log.%d",id)),true),MimeType.TEXT_PLAIN);
    }
    
    protected View no_content(){
        return new BytesView(getPath(),new byte[]{},getPath().getAccept()){
            @Override
            public void write() throws IOException {
                super.write(HttpServletResponse.SC_NO_CONTENT);
            }
        };
    }
}
