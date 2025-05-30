/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.controller;

import com.venky.cache.Cache;
import com.venky.cache.UnboundedCache;
import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.log.TimerUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.entity.Entity;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.RecordNotFoundException;
import com.venky.swf.db.table.Table;
import com.venky.swf.db.table.Table.ColumnDescriptor;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.path.Path;
import com.venky.swf.path.Path.ControllerInfo;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.plugins.lucene.index.common.ModelCollector;
import com.venky.swf.routing.Config;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.util.TemplateProcessor;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.ForwardedView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.HtmlView.StatusType;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.AbstractModelView;
import com.venky.swf.views.model.ModelEditView;
import com.venky.swf.views.model.ModelListView;
import com.venky.swf.views.model.ModelShowView;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 *
 * @author venky
 */
public class ModelController<M extends Model> extends Controller {

    private Class<M> modelClass;
    private ModelReflector<M> reflector;
    private boolean indexedModel = false;
    private IntegrationAdaptor<M, ?> integrationAdaptor = null;
    private IntegrationAdaptor<M, ?> returnIntegrationAdaptor = null ;

    @Override
    public String getTemplateDirectory() {
        return getTemplateDirectory(getReflector().getTableName().toLowerCase());
    }
    
    public boolean isIndexedModel() {
        return indexedModel;
    }
    
    public ModelController(Path path) {
        super(path);
        modelClass = getPath().getModelClass();
        reflector = ModelReflector.instance(modelClass);
        indexedModel = !reflector.getIndexedFieldGetters().isEmpty();
        if (path.getProtocol() != MimeType.TEXT_HTML) {
            integrationAdaptor = IntegrationAdaptor.instance(modelClass, FormatHelper.getFormatClass(path.getProtocol()));
        }
        if (path.getReturnProtocol() != MimeType.TEXT_HTML) {
            returnIntegrationAdaptor = IntegrationAdaptor.instance(modelClass, FormatHelper.getFormatClass(path.getReturnProtocol()));
        }
        if (returnIntegrationAdaptor == null){
            returnIntegrationAdaptor = integrationAdaptor;
        }
    }

    public IntegrationAdaptor<M, ?> getIntegrationAdaptor() {
        return this.integrationAdaptor;
    }

    public IntegrationAdaptor<M, ?> getReturnIntegrationAdaptor() {
        return this.returnIntegrationAdaptor;
    }


    protected ModelReflector<M> getReflector() {
        return reflector;
    }

    public View exportxls() {
        return exportxls(null);
    }
    protected View exportxls(List<M> records){
        ensureUI();
        Workbook wb = new XSSFWorkbook();
        super.exportxls(getModelClass(), wb,records);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            wb.write(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new BytesView(getPath(), os.toByteArray(), MimeType.APPLICATION_XLSX, "content-disposition", "attachment; filename=" + getModelClass().getSimpleName() + ".xlsx");
    }

    @Depends("save")
    @Override
    public View importxls() {
        ensureUI();
        return super.importxls();
    }

    protected void ensureUI() {
        if (integrationAdaptor != null) {
            throw new RuntimeException("Action is only available from UI");
        }
    }

    @Override

    @RequireLogin
    public View index() {
        if (returnIntegrationAdaptor == null && TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/index.html")){
            return html("index");
        }

        Timer index = cat.startTimer(getReflector().getTableName() + ".index");
        try {
            if (indexedModel) {
                return search();
            } else {
                return list();
            }
        } finally {
            index.stop();
        }
    }

    public int getMaxListRecords() {
        Map<String, Object> formData = getFormFields();
        int maxRecords = MAX_LIST_RECORDS;
        if (!formData.isEmpty()) {
            Object mr = formData.get("maxRecords");
            if (!ObjectUtil.isVoid(mr)) {
                maxRecords = Integer.parseInt(StringUtil.valueOf(mr));
                if (maxRecords == 0){
                    maxRecords = (int)Database.getTable(getModelClass()).recordCount();
                }
            }
        }

        return maxRecords;
    }


    public View search() {
        Map<String, Object> formData = new HashMap<>(getFormFields());
        int maxRecords = getMaxListRecords();
        if (!formData.isEmpty()) {
            rewriteQuery(formData);
        }
        return search(formData, maxRecords);
    }

    public View search(String strQuery) {
        getFormFields().put("q", strQuery);
        Map<String, Object> formData = new HashMap<String, Object>(getFormFields());
        rewriteQuery(formData);

        return search(formData, getMaxListRecords());
    }

    public LuceneIndexer getIndexer(){
        return LuceneIndexer.instance(getModelClass());
    }
    
    @SuppressWarnings("unchecked")
    protected Query getQuery(Map<String,Object> formData){
        String q = (String)formData.get("q");
        Map<String,List<String>> termQueries = (Map<String,List<String>>)formData.get("termQueries");
        ModelReflector<M> reflector = getReflector();
        
        
        if (q != null){
            return getIndexer().constructQuery(q);
        }else if (termQueries != null && !termQueries.isEmpty()){
            try (StandardAnalyzer analyzer = new StandardAnalyzer()) {
                Builder builder = new BooleanQuery.Builder();
                ObjectHolder<Integer> maxNumTerms = new ObjectHolder<>(0);
                termQueries.forEach((field,values)->{
                    Bucket numTerms = new Bucket();
                    for (int i = 0 ; i < values.size() ; i ++){
                        String value = values.get(i);
                        
                        Term term = new Term(field, analyzer.normalize(field, value));
                        float boost = boostTable.get(values.size()-i);
                        numTerms.increment();
                        addQueries(builder, term, boost);
                        //BoostQuery boostQuery = new BoostQuery(new TermQuery(term),boostTable.get(values.size()-i));
                    }
                    maxNumTerms.set(Math.max(maxNumTerms.get(),numTerms.intValue()));
                });
                //builder.setMinimumNumberShouldMatch((int)Math.ceil(0.6 * maxNumTerms.get().doubleValue())); Too Restrictive
                finalizeQuery(builder);
                return builder.build();
            }
        }else {
            return null;
        }
    }
    
    protected void finalizeQuery(Builder builder) {
    }
    
    public void addQueries(Builder builder , Term term ,float boost){
        if (term.text().length() > 6) { //Changed from 7 to 6
            builder.add(new BoostQuery(new ConstantScoreQuery(new FuzzyQuery(term)), boost), Occur.SHOULD);
        }else {
            builder.add(new BoostQuery(new ConstantScoreQuery(new PrefixQuery(term)),boost),Occur.SHOULD);
        }
    }
    static Map<Integer,Float> boostTable = new UnboundedCache<>() {
        @Override
        protected Float getValue(Integer key) {
            return Double.valueOf(Math.pow(1.2,key)).floatValue();
        }
    };
    
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    protected List<M>   searchRecords(Query q, int maxRecords){
        return searchRecords(q, maxRecords,0);
    }
    protected List<M>   searchRecords(Query q,  int maxRecords , int minDistinctScores){
        return searchRecords(q,createCollector(maxRecords,minDistinctScores));
    }
    protected ModelCollector<M> createCollector(int maxRecords,int minDistinctScores){
        return new ModelCollector<M>(getModelClass(),maxRecords,minDistinctScores,500,getWhereClause());
    }
    protected List<M>   searchRecords(Query q, ModelCollector<M> collector ){
        Timer timer = cat.startTimer("Fetching query " + q);
        getIndexer().fire(q,collector.getBatchSize(),collector);
        return collector.getRecords();
    }
    /*
    protected List<M>   searchRecords(Query q,  int maxRecords , int minDistinctScores){
        int batchSize = Math.min(500,maxRecords);
        
        final Map<Long, M> recordMap  = new HashMap<>();
        final SequenceMap<Long,ScoreDoc> allIdsInspected = new SequenceMap<>();
        Set<Float> scores = new HashSet<>();
        
        Timer timer = cat.startTimer("Fetching query " + q);
        getIndexer().fire(q, batchSize , new ResultCollector() {
            final SequenceMap<Long,ScoreDoc> idsBeingProcessed = new SequenceMap<>();
            
            @Override
            public void collect(Document d, ScoreDoc scoreDoc) {
                TimerUtils.time(cat,"Collecting Record Ids" , ()->{
                    Long id = Long.valueOf(d.getField("ID").stringValue());
                    idsBeingProcessed.put(id,scoreDoc);
                    return null;
                });
            }
            
            public int count() {
                if (!idsBeingProcessed.isEmpty()) {
                    Select sel = TimerUtils.time(cat, "Forming Select Statement for collected ids" , () ->{
                        Select select = new Select();
                        select.from(getModelClass());
                        select.where(new Expression(getReflector().getPool(), Conjunction.AND)
                                .add(Expression.createExpression(getReflector().getPool(), "ID", Operator.IN, idsBeingProcessed.keySet().toArray()))
                                .add(getWhereClause()));
                        select.orderBy(getReflector().getOrderBy());
                        return select;
                    });
                    
                    List<M> tmpList = TimerUtils.time(cat, "Executing Select for collected ids " , ()->{
                        return sel.execute(getModelClass(), getFilter());
                    });
                    
                    for (M tmp : tmpList) {
                        recordMap.put(tmp.getId(), tmp);
                        if (minDistinctScores > 0) {
                            scores.add(idsBeingProcessed.get(tmp.getId()).score);
                        }
                    }
                    allIdsInspected.putAll(idsBeingProcessed);
                    idsBeingProcessed.clear();
                }
                int totalNumRecords = recordMap.size();
                int totalScoreCount = scores.size();
                
                if (totalNumRecords >= maxRecords && totalScoreCount >= minDistinctScores) {
                    return batchSize; //No more needed.
                }else {
                    return Math.min(totalNumRecords,batchSize-1);
                }
            }
        });
        List<M> records = new ArrayList<>();
        int numRecordsAdded = 0;
        for (long id : allIdsInspected.keySet()){
            M record = recordMap.remove(id); //Retains the order from lucene based on scores.
            if (record != null){
                record.setTxnProperty("scoreDoc",allIdsInspected.get(id));
                records.add(record);
                numRecordsAdded ++ ;
            }
            if (numRecordsAdded >= maxRecords){
                break;
            }
        }
        
        return records;
    }
    
     */
    
    protected View search(Map<String, Object> formData, int maxRecords) {
        Query query =  TimerUtils.time(cat, "getQueries" , ()-> getQuery(formData));
        if (query != null) {
            List<M> records = TimerUtils.time(cat, "Searching across all queries" ,  ()-> searchRecords(query, maxRecords));
            if (!records.isEmpty()) {
                return list(records, maxRecords == 0 || records.size() < maxRecords);
            } else {
                return list(new ArrayList<>(), true);
            }
        }else {
            return list(maxRecords);
        }
    }
    
    protected void rewriteQuery(Map<String, Object> formData) {
        if (!formData.containsKey("q")){
            return;
        }
        String strQuery = StringUtil.valueOf(formData.get("q"));
        if (ObjectUtil.isVoid(strQuery)){
            formData.remove("q");
            return;
        }
        if (strQuery.contains(":")){
            //Already in lucene syntax;
            //Do nothing
            return;
        }
        
        Map<String,List<String>> termQueries = new UnboundedCache<String, List<String>>() {
            @Override
            protected List<String> getValue(String field) {
                return new LinkedList<>();
            }
        };
        
        for (String f : getReflector().getIndexedFields()) {
            String luceneTerm = f;
            Method referredModelIdGetter = getReflector().getFieldGetter(f);
            boolean isReferenceField = false;
            if (getReflector().getReferredModelGetterFor(referredModelIdGetter) != null) {
                luceneTerm = f.substring(0,f.length()-"_ID".length());
                isReferenceField = true;
            }
            
            TypeRef<?> ref = reflector.getJdbcTypeHelper().getTypeRef(referredModelIdGetter.getReturnType());
            

            for (StringTokenizer tk = new StringTokenizer(strQuery,", ()\t\r\n\f"); tk.hasMoreTokens();) {
                String token  = tk.nextToken();
                boolean canAdd = true;
                
                if (!isReferenceField && ref.isNumeric()) {
                    try {
                        canAdd = !reflector.isVoid(ref.getTypeConverter().valueOf(token));
                    }catch (Exception ex){
                        canAdd = false;
                    }
                }
                if (canAdd) {
                    termQueries.get(luceneTerm).add(token);
                }
            }
        }
        if (isIndexedModel()) {
            try {
                Integer.valueOf(strQuery);
                termQueries.get("ID").add(strQuery);
            } catch (NumberFormatException ex) {
                // Nothing to do.
            }
        }
        formData.put("termQueries", termQueries);
        formData.remove("q");
        cat.fine(formData.toString());
    }

    public View list() {
        return list(Select.MAX_RECORDS_ALL_RECORDS);
    }

    protected View list(int maxRecords) {
        List<M> records = null;
        Select.ResultFilter<M> filter = getFilter();
        if (!reflector.isVirtual()) {
            Select q = new Select(getColumnsToList()).from(modelClass);
            records = q.where(getWhereClause()).orderBy(getReflector().getOrderBy()).execute(modelClass, maxRecords, filter);
        } else {
            records = getChildrenFromParent();
            Expression where = getWhereClause();
            Iterator<M> i = records.iterator();
            while (i.hasNext()) {
                M record = i.next();
                if (!where.eval(record)) {
                    i.remove();
                } else if (!filter.pass(record)) {
                    i.remove();
                }
            }
        }

        return list(records, maxRecords == 0 || records.size() < maxRecords);
    }

    protected Expression getWhereClause(){
        Expression expression = new Expression(getReflector().getPool(),Conjunction.AND);
        expression.add(getPath().getWhereClause());
        return expression;
    }

    private String[] getColumnsToList() {
        List<String> columns = new ArrayList<>();
        ModelReflector<M> reflector = getReflector();
        String[] iFields = getIncludedFields();
        List<String> includedFields =  iFields == null?  reflector.getRealFields() : Arrays.asList(iFields);

        //Only include requested fields if passed.
        for (String field : includedFields){
            if (reflector.isFieldVirtual(field)){
                //API may have requested virtual fields also
                continue;
            }
            if (InputStream.class.isAssignableFrom(reflector.getFieldGetter(field).getReturnType()) && iFields == null ){
                continue; //Don't include stream fields by default unless asked; It kills performance
            }
            ColumnDescriptor cd = reflector.getColumnDescriptor(field);
            columns.add(cd.getEscapedName());
        }
        return columns.toArray(new String[]{});
    }

    protected Select.ResultFilter<M> getFilter() {
        return new DefaultModelFilter<M>(getModelClass());
    }

    private List<M> getChildrenFromParent() {
        List<M> children = new ArrayList<M>();
        Map<Class<? extends Model>, List<Method>> childListMethodsOnParentClass = new UnboundedCache<Class<? extends Model>, List<Method>>() {
            private static final long serialVersionUID = 1040614841128288969L;

            @Override
            protected List<Method> getValue(Class<? extends Model> parentClass) {
                return new SequenceSet<>();
            }
        };

        for (Method rmg : reflector.getReferredModelGetters()) {
            Class<? extends Model> referredModelClass = reflector.getReferredModelClass(rmg);
            ModelReflector<? extends Model> ref = ModelReflector.instance(referredModelClass);
            if (ref.isVirtual()) {
                continue;
            }
            for (Method cg : ref.getChildGetters()) {
                ModelReflector<? extends Model> childRef = ModelReflector.instance(ref.getChildModelClass(cg));

                if (StringUtil.equals(reflector.getTableName(), childRef.getTableName())) {
                    HIDDEN hidden = ref.getAnnotation(cg, HIDDEN.class);
                    if (hidden == null || !hidden.value()) {
                        childListMethodsOnParentClass.get(referredModelClass).add(cg);
                    }
                }
            }
        }
        List<ControllerInfo> controllerElements = new ArrayList<ControllerInfo>(getPath().getControllerElements());
        Collections.reverse(controllerElements);
        Iterator<ControllerInfo> cInfoIter = controllerElements.iterator();
        ControllerInfo selfModel = null;
        if (cInfoIter.hasNext()) {
            selfModel = cInfoIter.next();// The last model was self.
        }
        while (cInfoIter.hasNext()) {
            ControllerInfo info = cInfoIter.next();
            for (Class<? extends Model> parentClass : childListMethodsOnParentClass.keySet()) {
                String simpleModelName = info.getModelClass() == null ? null : info.getModelClass().getSimpleName();
                if (StringUtil.equals(parentClass.getSimpleName(), simpleModelName)) {
                    //Name of  the model class is sacred and 
                    Long id = info.getId();
                    if (id == null) {
                        continue;
                    }
                    Model parent = Database.getTable(parentClass).get(info.getId());
                    for (Method childGetter : childListMethodsOnParentClass.get(parentClass)) {
                        try {
                            if (ObjectUtil.equals(selfModel.getModelClass().getSimpleName(),
                                    ((Class) ((ParameterizedType) childGetter.getGenericReturnType()).getActualTypeArguments()[0]).getSimpleName())) {
                                children = (List<M>) childGetter.invoke(parent);
                                break;
                            }
                        } catch (Exception e) {
                            //
                        }
                    }
                    break;
                }
            }
        }
        return children;
    }
    protected View list(List<M> records, boolean isCompleteList) {
        return list(records,isCompleteList,getReturnIntegrationAdaptor());
    }
    protected <T> View list(List<M> records, boolean isCompleteList , IntegrationAdaptor<M,T> overrideIntegrationAdaptor) {
        View v = null;
        if (overrideIntegrationAdaptor != null) {
            v = overrideIntegrationAdaptor.createResponse(getPath(), records, getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()),
                    getIgnoredParentModels(), getConsideredChildModels(), getIncludedModelFields());
        } else {
            View lv = null;
            if (ObjectUtil.equals("Y",getPath().getFormFields().get("exportxls"))){
                lv = exportxls(records);
            }else {
                lv = constructModelListView(records, isCompleteList);
            }
            if (lv instanceof HtmlView) {
                v = dashboard((HtmlView) lv);
            } else {
                // To support View Redirection.!!
                v = lv;
            }
        }
        return v;
    }

    protected View constructModelListView(List<M> records, boolean isCompleteList) {
        return new ModelListView<M>(getPath(), getIncludedFields(), records, isCompleteList);
    }

    protected String[] getIncludedFields() {
        Map<Class<? extends Model>, List<String>> map  = getIncludedModelFields();
        if (map.containsKey(getModelClass())){
            return map.get(getModelClass()).toArray(new String[]{});
        }else {
            return null;
        }
    }



    protected Class<M> getModelClass() {
        return modelClass;
    }

    @SingleRecordAction(icon = "glyphicon-eye-open", tooltip = "See this record")
    @Depends("index")
    public View show(long id) {
        if (returnIntegrationAdaptor ==  null && TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/show.html")){
            return redirectTo("html/show?id="+id);//+"&"+ Encode.forUri(getPath().getRequest().getQueryString()));
        }

        M record = Database.getTable(modelClass).get(id);
        if (record == null) {
            throw new RecordNotFoundException();
        }
        if (getSessionUser() != null && !record.isAccessibleBy(getSessionUser(), modelClass)) {
            throw new AccessDeniedException();
        }

        // Either it is unsecured or is accessable
        return show(record);
    }

    protected View show(M record) {
        return show(record,getReturnIntegrationAdaptor());
    }

    protected <T> View show(M record, IntegrationAdaptor<M,T> returnIntegrationAdaptor){
        if (returnIntegrationAdaptor != null) {
            return returnIntegrationAdaptor.createResponse(getPath(), record, true,
                    getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()),
                    getIgnoredParentModels(), getConsideredChildModels(), getIncludedModelFields());
        }else{
            return dashboard(createModelShowView(record));
        }
    }

    protected Map<Class<? extends Model>,List<Class <? extends Model>>> getConsideredChildModels() {
        return getReflector().getChildrenToBeConsidered(getIncludedModelFields());
    }

    protected Set<Class<? extends Model>> getIgnoredParentModels() {
        return new HashSet<>();
    }

    protected Map<Class<? extends Model>, List<String>> getIncludedModelFields() {
        Map<Class<? extends Model>,List<String>> map = getIncludedModelFieldsFromRequest();
        if (map == null) {
            map = new HashMap<>();
        }
        return map;
    }
    @SuppressWarnings("unchecked")
    public Map<Class<? extends Model>,List<String>> getIncludedModelFieldsFromRequest(){
        Map<Class<? extends Model>,List<String>> map = null;
        String template = getPath().getHeader("IncludedModelFields");
        if (template != null){
            JSONObject jsonObject = (JSONObject) JSONValue.parse(new InputStreamReader(new ByteArrayInputStream(Base64.getDecoder().decode(template.getBytes(StandardCharsets.UTF_8)))));
            if (jsonObject != null){
                map = new HashMap<>();
                for (Object key : jsonObject.keySet()){

                    List<String> modelClasses = Config.instance().getModelClasses(
                            FormatHelper.change_case((String)key, Config.instance().getApiKeyCase(), KeyCase.CAMEL));
                    if (!modelClasses.isEmpty()){
                        try {
                            Class<? extends Model> modelClass = (Class<? extends Model>) Class.forName(modelClasses.get(0));
                            JSONArray fields = (JSONArray)jsonObject.get(key);
                            List<String> fieldNames = new ArrayList<>();
                            fields.forEach(f->fieldNames.add(StringUtil.underscorize((String)f)));
                            map.put(modelClass,fieldNames);
                        } catch (ClassNotFoundException e) {
                            //
                        }
                    }
                }
            }
        }
        return map;
    }


    protected HtmlView createModelShowView(M record) {
        return constructModelShowView(getPath(), record);
    }

    protected HtmlView constructModelShowView(Path path, M record) {
        return new ModelShowView<M>(path, getIncludedFields(), record);
    }

    @Depends("index")
    public View view(long id) {
        return view(id, null);
    }

    protected boolean isViewableInLine(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        if (mimeType.startsWith("image")) {
            return true;
        } else if (mimeType.startsWith("video")) {
            return true;
        } else if (mimeType.equals("audio/ogg")) {
            return true;
        }

        return false;
    }

    /**
     *
     * @param record Any db object. Need not be of type M
     * @param asAttachment
     * @param <T>
     * @return
     */
    protected <T extends Model> View  view(T record,Boolean asAttachment){
        ModelReflector<T> ref = record.getReflector();

        if (getSessionUser() == null || record.isAccessibleBy(getSessionUser(), ref.getModelClass())) {
            try {
                for (Method getter : ref.getFieldGetters()) {
                    if (InputStream.class.isAssignableFrom(getter.getReturnType())) {
                        String fieldName = ref.getFieldName(getter);
                        String fileName = ref.getContentName(record, fieldName);
                        String mimeType = ref.getContentType(record, fieldName);
                        if (ref.getDefaultContentType().equals(mimeType) && fileName != null) {
                            mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
                        }

                        if (asAttachment == null) {
                            asAttachment = !isViewableInLine(mimeType);
                        }
                        if (fileName != null && asAttachment) {
                            return new BytesView(getPath(), StringUtil.readBytes((InputStream) getter.invoke(record)), mimeType,
                                    "content-disposition", "attachment; filename=\"" + fileName + "\"");
                        } else {
                            return new BytesView(getPath(), StringUtil.readBytes((InputStream) getter.invoke(record)), mimeType);
                        }
                    }else if (Reader.class.isAssignableFrom(getter.getReturnType())){
                        String fieldName = ref.getFieldName(getter);
                        return new BytesView(getPath(),StringUtil.read((Reader) ref.get(record,fieldName)).getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        } else {
            throw new AccessDeniedException();
        }
        return getSuccessView();
    }

    private View view(long id, Boolean asAttachment) {
        M record = Database.getTable(modelClass).get(id);
        return view(record,asAttachment);
    }

    @SingleRecordAction(icon = "glyphicon-edit")
    @Depends("save,index")
    public View edit(long id) {
        ensureUI();
        if (returnIntegrationAdaptor == null && TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/edit.html")){
            return redirectTo("html/edit?id="+id);
        }
        return dashboard(createModelEditView(id, "save"));
    }

    protected HtmlView createModelEditView(long id, String formAction) {
        M record = Database.getTable(modelClass).get(id);
        return createModelEditView(record, formAction);
    }

    protected HtmlView createModelEditView(M record, String formAction) {
        return createModelEditView(getPath(), record, formAction);
    }

    protected HtmlView createModelEditView(Path path, M record, String formAction) {
        if (record.isAccessibleBy(getSessionUser(), getModelClass())) {
            return constructModelEditView(path, record, formAction);
        } else {
            throw new AccessDeniedException();
        }
    }

    protected HtmlView constructModelEditView(Path path, M record, String formAction) {
        return new ModelEditView<M>(path, getIncludedFields(), record, formAction);
    }

    @SingleRecordAction(icon = "glyphicon-duplicate", tooltip = "Duplicate")
    @Depends("save,index")
    public View clone(long id) {
        M record = Database.getTable(modelClass).get(id);
        M newrecord = clone(record);
        return blank(newrecord);
    }

    public M clone(M record) {
        Table<M> table = Database.getTable(modelClass);
        M newrecord = table.newRecord();

        Record oldRaw = record.getRawRecord();
        Record newRaw = newrecord.getRawRecord();

        for (String f : oldRaw.getFieldNames()) { //Fields in raw records are column names.
            if (getReflector().isFieldCopiedWhileCloning(getReflector().getFieldName(f))) {
                newRaw.put(f, oldRaw.get(f));
            }
        }
        newRaw.setNewRecord(true);
        return newrecord;
    }

    @Depends("save")
    public View blank() {
        if (returnIntegrationAdaptor == null && TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/blank.html")){
            return redirectTo("html/blank");
        }
        M record = Database.getTable(modelClass).newRecord();
        return blank(record);
    }
    public Entity meta(Long id){
        return super.meta(id,getReflector());
    }
    @RequireLogin(false)
    public View describe(){
        return new BytesView(getPath(),meta(null).toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }
    @RequireLogin(false)
    public View describe(long id){
        return new BytesView(getPath(),meta(id).toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }

    protected void defaultFields(M record){
        record.defaultFields();
        getPath().fillDefaultsForReferenceFields(record, getModelClass());
        if (getSessionUser() != null){
            record.setCreatorUserId(getSessionUser().getId());
            record.setUpdaterUserId(getSessionUser().getId());
        }
    }
    protected View blank(M record) {
        defaultFields(record);
        if (returnIntegrationAdaptor != null) {
            return returnIntegrationAdaptor.createResponse(getPath(), record, true,null, new HashSet<>(),new HashMap<>(), new HashMap<>(),true);
        } else {
            return dashboard(createBlankView(record, "save"));
        }
    }

    protected HtmlView createBlankView(M record, String formAction) {
        return createBlankView(getPath(), record, formAction);
    }

    protected HtmlView createBlankView(Path path, M record, String formAction) {
        HtmlView mev = constructModelEditView(path, record, formAction);
        for (String field : reflector.getFields()) {
            if (reflector.isHouseKeepingField(field)) {
                if (mev instanceof AbstractModelView) {
                    ((AbstractModelView<M>) mev).getIncludedFields().remove(field);
                }
            }
        }
        return mev;
    }

    public View truncate() {
        Select q = new Select().from(modelClass);
        List<M> records = q.where(getWhereClause()).execute(modelClass, new Select.AccessibilityFilter<M>());
        for (M record : records) {
            if (getPath().canAccessControllerAction("destroy", String.valueOf(record.getId()))) {
                record.destroy();
            } else {
                throw new AccessDeniedException("Don't have permission to destroy record " + record.getId());
            }
        }
        return back();
    }

    @SingleRecordAction(icon = "glyphicon-trash")
    @Depends("index")
    public View destroy(long id) {
        M record = Database.getTable(modelClass).get(id);
        if (record == null) {
            throw new RecordNotFoundException();
        }
        destroy(record);
        return getSuccessView();
    }

    protected void destroy(M record) {
        if (record != null) {
            if (record.isAccessibleBy(getSessionUser(), modelClass)) {
                record.destroy();
            } else {
                throw new AccessDeniedException();
            }
        }
    }

    protected View getSuccessView() {
        View ret = null;
        if (returnIntegrationAdaptor != null) {
            ret = returnIntegrationAdaptor.createStatusResponse(getPath(), null);
        } else {
            ret = back();
        }
        return ret;
    }

    protected RedirectorView redirectTo(String action) {
        RedirectorView v = new RedirectorView(getPath(), action);
        return v;
    }

    protected View forwardTo(String action) {
        return new ForwardedView(getPath(), action);
    }

    public interface Action<M> {

        public View noAction(M m);

        public void act(M m);

        public <C extends Model> void actOnChild(M parent, Class<C> childModelClass, Model c);

        public View error(M m,boolean isNew);
    }

    public class SaveAction implements Action<M> {

        public View noAction(M m) {
            return noActionView(m);
        }

        @Override
        public void act(M m) {
            save(m, getModelClass());
        }

        @SuppressWarnings("unchecked")
        public <C extends Model> void actOnChild(M parent, Class<C> childModelClass, Model c) {
            ModelReflector<C> childReflector = ModelReflector.instance(childModelClass);
            for (String f : childReflector.getReferenceFields(getModelClass())) {
                if (childReflector.isFieldSettable(f)) {
                    Object oldValue = childReflector.get(c, f);
                    if (Database.getJdbcTypeHelper(childReflector.getPool()).isVoid(oldValue)) {
                        childReflector.set(c, f, parent.getId());
                    }
                }
            }
            save((C) c, childModelClass);
        }

        @Override
        public View error(M m,boolean isNew) {
            HtmlView errorView = null;
            if (isNew) {
                errorView = createBlankView(getPath().createRelativePath("blank"), m, "save");
            } else {
                errorView = createModelEditView(getPath().createRelativePath("edit/" + m.getId()), m, "save");
            }
            return errorView;
        }

    }

    protected View saveModelFromForm() {
        return performPostAction(getSaveAction());
    }

    protected Action<M> getSaveAction() {
        return new SaveAction();
    }

    protected View performPostAction(Action<M> action) {
        Map<String, Object> formFields = getFormFields();
        String id = (String) formFields.get("ID");
        String lockId = (String) formFields.get("LOCK_ID");

        M record = null;
        if (ObjectUtil.isVoid(id)) {
            record = Database.getTable(modelClass).newRecord();
        } else {
            record = Database.getTable(modelClass).get(Long.valueOf(id));
            if (!ObjectUtil.isVoid(lockId)) {
                if (record.getLockId() != Long.parseLong(lockId)) {
                    throw new RuntimeException("Stale record update prevented. Please reload and retry!");
                }
            }
            if (!record.isAccessibleBy(getSessionUser(), modelClass)) {
                throw new AccessDeniedException();
            }
        }
        List<String> setableFields = reflector.getRealFields();
        for (String virtualField : reflector.getVirtualFields()) {
            if (reflector.isFieldSettable(virtualField)) {
                setableFields.add(virtualField);
            }
        }

        Iterator<String> e = formFields.keySet().iterator();
        String buttonName = "_SUBMIT_NO_MORE";
        String digest = null;
        MultiException dataValidationExceptions = new MultiException("Invalid input: ");
        boolean hasUserModifiedData = false;
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = setableFields.contains(name) && !reflector.isHouseKeepingField(name) ? name : null;
            if (fieldName != null) {
                try {
                    validateEnteredData(reflector, record, fieldName, formFields);
                } catch (Exception ex) {
                    dataValidationExceptions.add(ex);
                    hasUserModifiedData = true;
                }
                Object value = formFields.get(fieldName);
                Class<?> fieldClass = reflector.getFieldGetter(fieldName).getReturnType();
                if (value == null && (Reader.class.isAssignableFrom(fieldClass) || InputStream.class.isAssignableFrom(fieldClass))) {
                    continue;
                }
                reflector.set(record, fieldName, value);
            } else if (name.startsWith("_SUBMIT")) {
                buttonName = name;
            } else if (name.startsWith("_FORM_DIGEST")) {
                digest = (String) formFields.get("_FORM_DIGEST");
            }
        }
        boolean isNew = record.getRawRecord().isNewRecord();
        hasUserModifiedData = hasUserModifiedData || hasUserModifiedData(formFields, digest);
        if (hasUserModifiedData || isNew) {
            try {
                if (!dataValidationExceptions.isEmpty()) {
                    throw dataValidationExceptions;
                }
                action.act(record);
                for (Class<? extends Model> childModelClass : reflector.getChildModels(true, false)) {
                    @SuppressWarnings("unchecked")
                    Map<Integer, Map<String, Object>> childFormRecords = (Map<Integer, Map<String, Object>>) formFields.get(childModelClass.getSimpleName());
                    if (childFormRecords != null) {
                        for (Integer key : childFormRecords.keySet()) {
                            Map<String, Object> childFormFields = childFormRecords.get(key);
                            action.actOnChild(record, childModelClass, loadChildFromFormFields(childModelClass, childFormFields));
                        }
                    }
                }

                if (isNew && hasUserModifiedData) {
                    if (buttonName.equals("_SUBMIT_MORE") && getPath().canAccessControllerAction("blank", String.valueOf(record.getId()))) {
                        //Usability Logic: If user is not modifying data shown, then why be in data entry mode.
                        getPath().addInfoMessage(getModelClass().getSimpleName() + " created sucessfully, press Done when finished.");
                        return redirectTo("clone/" + record.getId());
                    } else if (buttonName.equals("_SUBMIT_NO_MORE") && getPath().canAccessControllerAction("show", String.valueOf(record.getId())) && !record.getReflector().getChildModels(false, true).isEmpty()) {
                        //May want to add children
                        return redirectTo("show/" + record.getId());
                    }
                }
            } catch (RuntimeException ex) {
                if (hasUserModifiedData) {
                    Throwable th = ExceptionUtil.getRootCause(ex);
                    Config.instance().printStackTrace(getClass(), th);
                    String message = th.getMessage();
                    if (message == null) {
                        message = th.toString();
                    }
                    Database.getInstance().getCurrentTransaction().rollback(th);
                    View eView = null;
                    if (getReturnIntegrationAdaptor() != null){
                        throw ex;
                    }else {
                        eView = action.error(record,isNew);
                    }
                    if (eView instanceof HtmlView) {
                        getPath().addMessage(StatusType.ERROR, message);
                        return dashboard((HtmlView) eView);
                    } else {
                        return eView;
                    }
                }
            }
            return afterPersistDBView(record);
        } else {
            View view = action.noAction(record);
            if (view instanceof HtmlView) {
                return dashboard((HtmlView) view);
            } else {
                return view;
            }
        }
    }

    private <T extends Model> T loadChildFromFormFields(Class<T> childModelClass, Map<String, Object> formFields) {

        String id = (String) formFields.get("ID");
        String lockId = (String) formFields.get("LOCK_ID");

        T record = null;
        if (ObjectUtil.isVoid(id)) {
            record = Database.getTable(childModelClass).newRecord();
        } else {
            record = Database.getTable(childModelClass).get(Long.valueOf(id));
            if (!ObjectUtil.isVoid(lockId)) {
                if (record.getLockId() != Long.parseLong(lockId)) {
                    throw new RuntimeException("Stale record update prevented. Please reload and retry!");
                }
            }
            if (!record.isAccessibleBy(getSessionUser(), childModelClass)) {
                throw new AccessDeniedException();
            }
        }
        ModelReflector<T> childReflector = ModelReflector.instance(childModelClass);
        List<String> setableFields = childReflector.getRealFields();
        for (String virtualField : childReflector.getVirtualFields()) {
            if (childReflector.isFieldSettable(virtualField)) {
                setableFields.add(virtualField);
            }
        }

        Iterator<String> e = formFields.keySet().iterator();
        MultiException dataValidationExceptions = new MultiException("Invalid input: ");
        while (e.hasNext()) {
            String name = e.next();
            String fieldName = setableFields.contains(name) && !childReflector.isHouseKeepingField(name) ? name : null;
            if (fieldName != null) {
                try {
                    validateEnteredData(childReflector, record, fieldName, formFields);
                } catch (Exception ex) {
                    dataValidationExceptions.add(ex);
                }
                Object value = formFields.get(fieldName);
                Class<?> fieldClass = childReflector.getFieldGetter(fieldName).getReturnType();
                if (value == null && (Reader.class.isAssignableFrom(fieldClass) || InputStream.class.isAssignableFrom(fieldClass))) {
                    continue;
                }
                childReflector.set(record, fieldName, value);
            }
        }
        if (!dataValidationExceptions.isEmpty()) {
            throw dataValidationExceptions;
        }
        return record;
    }

    protected View defaultActionView(M record) {
        View v = null;
        if (returnIntegrationAdaptor != null) {
            v = returnIntegrationAdaptor.createResponse(getPath(), record, true,
                    getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()),
                    getIgnoredParentModels(), getConsideredChildModels(), getIncludedModelFields());
        } else {
            v = back();
        }
        return v;
    }

    protected View noActionView(M record) {
        return defaultActionView(record);
    }

    protected View afterPersistDBView(M record) {
        return defaultActionView(record);
    }

    private static void computeHash(StringBuilder hash, ModelReflector<? extends Model> reflector, Map<String, Object> formFields, String fieldPrefix) {
        for (String field : reflector.getFields()) {
            if (!formFields.containsKey(field) || !reflector.isFieldEditable(field)) {
                continue;
            }
            Object currentValue = formFields.get(field);
            if (hash.length() > 0) {
                hash.append(",");
            }
            if (fieldPrefix != null) {
                hash.append(fieldPrefix);
            }
            hash.append(field).append("=").append(StringUtil.valueOf(currentValue));

            String autoCompleteHelperField = "_AUTO_COMPLETE_" + field;
            if (formFields.containsKey(autoCompleteHelperField)) {
                hash.append(",");
                String autoCompleteHelperFieldValue = StringUtil.valueOf(formFields.get(autoCompleteHelperField));
                if (fieldPrefix != null) {
                    hash.append(fieldPrefix);
                }
                hash.append(autoCompleteHelperField).append("=").append(autoCompleteHelperFieldValue);
            }
        }

        for (Class<? extends Model> modelClass : reflector.getChildModels(true, false)) {
            String modelName = modelClass.getSimpleName();
            ModelReflector<? extends Model> childModelReflector = ModelReflector.instance(modelClass);

            @SuppressWarnings("unchecked")
            SortedMap<Integer, Map<String, Object>> records = (SortedMap<Integer, Map<String, Object>>) formFields.get(modelName);
            if (records == null) {
                continue;
            }
            for (Integer key : records.keySet()) {
                Map<String, Object> childFormFields = records.get(key);
                computeHash(hash, childModelReflector, childFormFields, modelClass.getSimpleName() + "[" + key + "].");
            }
        }
    }

    protected boolean hasUserModifiedData(Map<String, Object> formFields, String oldDigest) {
        StringBuilder hash = new StringBuilder();
        computeHash(hash, reflector, formFields, null);
        String newDigest = hash == null ? null : Encryptor.encrypt(hash.toString());
        return !ObjectUtil.equals(newDigest, oldDigest);
    }

    private <T extends Model> void validateEnteredData(ModelReflector<T> reflector, T record, String field, Map<String, Object> formFields) {
        String autoCompleteHelperField = "_AUTO_COMPLETE_" + field;
        if (!formFields.containsKey(autoCompleteHelperField)) {
            return;
        }
        if (!reflector.isFieldEditable(field)) {
            return;
        }
        Object autoCompleteHelperFieldValue = formFields.get(autoCompleteHelperField);
        Object currentValue = Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(Long.class).getTypeConverter().valueOf(formFields.get(field));

        Method referredModelIdGetter = reflector.getFieldGetter(field);
        Method referredModelGetter = referredModelIdGetter == null ? null : reflector.getReferredModelGetterFor(referredModelIdGetter);
        Class<? extends Model> referredModelClass = referredModelGetter == null ? null : reflector.getReferredModelClass(referredModelGetter);

        if (referredModelClass == null) {
            return;//Defensive. not really needed due to first condition. of check existance of autoCompleteHelperField in formFields.
        }
        ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);

        String descriptionField = referredModelReflector.getDescriptionField();
        Method descriptionFieldGetter = referredModelReflector.getFieldGetter(descriptionField);

        Model referredModel = null;

        String fieldLiteral = referredModelGetter.getName().substring("get".length());

        if (Database.getJdbcTypeHelper(referredModelReflector.getPool()).isVoid(autoCompleteHelperFieldValue)) {
            if (!Database.getJdbcTypeHelper(referredModelReflector.getPool()).isVoid(currentValue)) {
                formFields.put(field, "");
            }
            return;
        }
        //In some tables that don't have description column or have non string description columns, this would have caused an issue of class cast in db..
        autoCompleteHelperFieldValue = Database.getJdbcTypeHelper(referredModelReflector.getPool()).getTypeRef(descriptionFieldGetter.getReturnType()).getTypeConverter().valueOf(autoCompleteHelperFieldValue);

        //autoCompleteHelperFieldValue is not void.
        Expression where = new Expression(referredModelReflector.getPool(), Conjunction.AND);

        //getAutoCompleteBaseWhere calling always is a performance killer!!.
        if (!Database.getJdbcTypeHelper(referredModelReflector.getPool()).isVoid(currentValue)) {
            where.add(new Expression(referredModelReflector.getPool(), "ID", Operator.EQ, currentValue));
        } else if (!referredModelReflector.isFieldVirtual(descriptionField)) {
            where.add(new Expression(referredModelReflector.getPool(), referredModelReflector.getColumnDescriptor(descriptionField).getName(), Operator.EQ,
                    autoCompleteHelperFieldValue));
        } else {
            where.add(getAutoCompleteBaseWhere(reflector, record, field));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<? extends Model> models = new Select().from(referredModelReflector.getRealModelClass()).where(where).execute(referredModelClass, 2, new Select.AccessibilityFilter());

        if (models.size() == 1) {
            referredModel = models.get(0);
            currentValue = StringUtil.valueOf(referredModel.getId());
            formFields.put(field, currentValue);
        }

        if (referredModel == null) {
            throw new RuntimeException("Please choose " + fieldLiteral + " from lookup.");
        } else {
            Object descriptionValue = referredModelReflector.get(referredModel, descriptionField);

            String sDescriptionValue = Database.getJdbcTypeHelper(referredModelReflector.getPool()).getTypeRef(descriptionFieldGetter.getReturnType()).getTypeConverter().toString(descriptionValue);
            String sAutoCompleteFieldDesc = Database.getJdbcTypeHelper(referredModelReflector.getPool()).getTypeRef(descriptionFieldGetter.getReturnType()).getTypeConverter().toString(autoCompleteHelperFieldValue);
            if (!ObjectUtil.equals(sDescriptionValue, sAutoCompleteFieldDesc)) {
                throw new RuntimeException("Please choose " + fieldLiteral + " from lookup.");
            }
        }
    }

    public View save() {
        HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        if (integrationAdaptor != null) {
            return saveModelsFromRequest();
        } else {
            return saveModelFromForm();
        }
    }

    protected  <T> View saveModelsFromRequest() {
        List<M> models = persistModelsFromRequest();
        return returnIntegrationAdaptor.createResponse(getPath(), models,
                getIncludedFields() == null ? null : Arrays.asList(getIncludedFields()),
                getIgnoredParentModels(), getConsideredChildModels(), getIncludedModelFields());
    }

    protected <T> List<M>   persistModelsFromRequest() {
        List<M> models = integrationAdaptor.readRequest(getPath(),true);
        persistModels(models);
        return models;
    }

    protected void persistModels(List<M> models) {
        for (M m : models) {
            try {
                // We would have already save these models while reading.
                save(m, getModelClass());
            } catch (RuntimeException ex) {
                Database.getInstance().getCache(getReflector()).clear();
                throw ex;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public View onAutoCompleteSelect() {
        ensureUI();
        List<String> fields = reflector.getFields();
        Map<String, Object> formData = getFormFields();
        M model = null;
        if (formData.containsKey("ID")) {
            model = Database.getTable(modelClass).get(Long.valueOf(formData.get("ID").toString()));
            model = model.cloneProxy();
        } else {
            model = Database.getTable(modelClass).newRecord();
        }

        String autoCompleteFieldName = null;
        for (String fieldName : formData.keySet()) {
            if (fields.contains(fieldName)) {
                Object ov = formData.get(fieldName);
                if (reflector.isFieldSettable(fieldName)) {
                    reflector.set(model, fieldName, ov);
                }
            } else if (fieldName.startsWith("_AUTO_COMPLETE_")) {
                autoCompleteFieldName = fieldName.split("_AUTO_COMPLETE_")[1];
            }
        }
        List<String> fieldsToSet = new ArrayList<>();
        Record record = model.getRawRecord();
        fieldsToSet.addAll(reflector.getVirtualFields());
        fieldsToSet.addAll(record.getDirtyFields());

        Method autoCompleteFieldGetter = reflector.getFieldGetter(autoCompleteFieldName);
        OnLookupSelect onlookup = reflector.getAnnotation(autoCompleteFieldGetter, OnLookupSelect.class);
        if (onlookup != null) {
            try {
                OnLookupSelectionProcessor<M> processor = (OnLookupSelectionProcessor<M>) Class.forName(onlookup.processor()).newInstance();
                processor.process(autoCompleteFieldName, model);
            } catch (Exception e) {
                //
            }
        }

        fieldsToSet.addAll(record.getDirtyFields());

        TypeConverter<Long> longTypeConverter = (TypeConverter<Long>) Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(Long.class).getTypeConverter();
        JSONObject obj = new JSONObject();
        for (String f : fieldsToSet) {
            Object value = reflector.get(record, f);
            Class<?>  type =  reflector.getFieldGetter(f).getReturnType();
            obj.put(f, reflector.getJdbcTypeHelper().getTypeRef(type).getTypeConverter().toString(value));

            Method fieldGetter = reflector.getFieldGetter(f);
            Method referredModelGetter = reflector.getReferredModelGetterFor(fieldGetter);
            if (referredModelGetter != null) {
                Class<? extends Model> referredModelClass = reflector.getReferredModelClass(referredModelGetter);
                ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);

                long referredModelId = longTypeConverter.valueOf(value);
                Model referredModel = Database.getTable(referredModelClass).get(referredModelId);

                String referredModelDescriptionField = referredModelReflector.getDescriptionField();
                obj.put("_AUTO_COMPLETE_" + f, referredModelReflector.get(referredModel, referredModelDescriptionField));
            }
        }
        return new BytesView(getPath(), obj.toString().getBytes(), MimeType.APPLICATION_JSON);
    }

    public View autocomplete() {
        //ensureUI();
        List<String> fields = reflector.getFields();
        Map<String, Object> formData = getFormFields();
        M model = null;
        if (formData.containsKey("ID")) {
            model = Database.getTable(modelClass).get(Long.valueOf(formData.get("ID").toString()));
        } else {
            model = Database.getTable(modelClass).newRecord();
        }
        model = model.cloneProxy();

        String autoCompleteFieldName = null;
        String value = "";
        for (String fieldName : formData.keySet()) {
            if (fields.contains(fieldName)) {
                Object ov = formData.get(fieldName);
                if (reflector.isFieldSettable(fieldName)) {
                    reflector.set(model, fieldName, ov);
                }
            } else if (fieldName.startsWith("_AUTO_COMPLETE_")) {
                autoCompleteFieldName = fieldName.split("_AUTO_COMPLETE_")[1];
                value = StringUtil.valueOf(formData.get(fieldName));
            }
        }
        cat.info(autoCompleteFieldName + "=" + value);
        model.getRawRecord().remove(autoCompleteFieldName);
        Expression where = getAutoCompleteBaseWhere(reflector, model, autoCompleteFieldName);
        ModelReflector<? extends Model> autoCompleteModelReflector = getAutoCompleteModelReflector(reflector, autoCompleteFieldName);
        return super.autocomplete(autoCompleteModelReflector.getModelClass(), where, autoCompleteModelReflector.getDescriptionField(), value);
    }

    private <T extends Model> Expression getAutoCompleteBaseWhere(ModelReflector<T> reflector, T model, String autoCompleteFieldName) {
        Expression where = new Expression(reflector.getPool(), Conjunction.AND);
        where.add(getAutoCompleteFieldParticipationWhere(reflector, model, autoCompleteFieldName));

        Path autoCompletePath = getAutoCompleteModelPath(reflector, autoCompleteFieldName);
        where.add(autoCompletePath.getWhereClause());
        return where;
    }

    private <T extends Model> Expression getAutoCompleteFieldParticipationWhere(ModelReflector<T> reflector, T model, String autoCompleteFieldName) {
        Expression where = new Expression(reflector.getPool(), Conjunction.AND);
        Method autoCompleteFieldGetter = reflector.getFieldGetter(autoCompleteFieldName);
        PARTICIPANT participant = reflector.getAnnotation(autoCompleteFieldGetter, PARTICIPANT.class);
        if (participant != null) {
            Cache<String, Map<String, List<Long>>> pOptions = getSessionUser().getParticipationOptions(reflector.getModelClass(), model);
            if (pOptions.get(participant.value()).containsKey(autoCompleteFieldName)) {
                List<Long> autoCompleteFieldValues = pOptions.get(participant.value()).get(autoCompleteFieldName);
                if (autoCompleteFieldValues != null){
                    if (!autoCompleteFieldValues.isEmpty()) {
                        autoCompleteFieldValues.remove(null); // We need not try to use null for lookups.
                        where.add(Expression.createExpression(reflector.getPool(), "ID", Operator.IN, autoCompleteFieldValues.toArray()));
                    } else {
                        where.add(new Expression(reflector.getPool(), "ID", Operator.EQ));
                    }
                }
            }
        }
        return where;
    }

    private <T extends Model> ModelReflector<? extends Model> getAutoCompleteModelReflector(ModelReflector<T> reflector, String autoCompleteFieldName) {
        Method autoCompleteFieldGetter = reflector.getFieldGetter(autoCompleteFieldName);
        return getAutoCompleteModelReflector(reflector, autoCompleteFieldGetter);
    }

    private <T extends Model> ModelReflector<? extends Model> getAutoCompleteModelReflector(ModelReflector<T> reflector, Method autoCompleteFieldGetter) {
        Class<? extends Model> autoCompleteModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(autoCompleteFieldGetter));
        ModelReflector<? extends Model> autoCompleteModelReflector = ModelReflector.instance(autoCompleteModelClass);
        return autoCompleteModelReflector;
    }

    private <T extends Model> Path getAutoCompleteModelPath(ModelReflector<T> reflector, String autoCompleteFieldName) {
        return getAutoCompleteModelPath(getAutoCompleteModelReflector(reflector, autoCompleteFieldName));
    }

    private Path getAutoCompleteModelPath(ModelReflector<? extends Model> autoCompleteModelReflector) {
        Path referredModelPath = getPath().createRelativePath((getPath().parameter() != null ? "" : getPath().action()) + "/" + LowerCaseStringCache.instance().get(autoCompleteModelReflector.getTableName()) + "/index");
        return referredModelPath;
    }

    @Override
    protected ImportSheetFilter getDefaultImportSheetFilter() {
        return new ImportSheetFilter() {

            @Override
            public boolean filter(Sheet sheet) {
                return sheet.getSheetName().equals(StringUtil.pluralize(getModelClass().getSimpleName()));
            }
        };
    }

    public View detail() {
        if (!getPath().getRequest().getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Api only supports POST method");
        }
        IntegrationAdaptor<M, ?> integrationAdaptor = getIntegrationAdaptor();
        IntegrationAdaptor<M, ?> returnIntegrationAdaptor = getReturnIntegrationAdaptor();
        if (returnIntegrationAdaptor == null){
            returnIntegrationAdaptor = integrationAdaptor;
        }

        if (integrationAdaptor != null) {
            List<M> models = integrationAdaptor.readRequest(getPath());
            for (Iterator<M> i = models.iterator(); i.hasNext();) {
                M m = i.next();
                if (m.getRawRecord().isNewRecord()) {
                    i.remove();
                }
            }
            return returnIntegrationAdaptor.createResponse(getPath(), models, null,
                    getIgnoredParentModels(), getConsideredChildModels(),getIncludedModelFields());
        }
        throw new AccessDeniedException("Cannot call this api from ui");

    }

    public View persist() {
        if (!getPath().getRequest().getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Api only supports POST method");
        }
        IntegrationAdaptor<M, ?> integrationAdaptor = getIntegrationAdaptor();
        if (integrationAdaptor != null) {
            List<M> models = persistModelsFromRequest();
            Config.instance().getLogger(getReflector().getModelClass().getName()).log(Level.INFO, "Persisted {0} records.", models.size());
            return getReturnIntegrationAdaptor().createStatusResponse(getPath(), null);
        }
        throw new AccessDeniedException("Cannot call this api from ui");

    }

    protected void ensureIntegrationMethod(HttpMethod httpmethod) throws RuntimeException {
        if (!getPath().getRequest().getMethod().equalsIgnoreCase(httpmethod.toString())){
            throw new RuntimeException("Request must be invoked as a http " + httpmethod.toString() + " method.");
        }
        if (getIntegrationAdaptor() == null){
            throw new RuntimeException("Request must be invoked as a http api content-type cannot be " + getPath().getProtocol().toString() );
        }
    }

    public View reindex(long id) throws IOException {
        M record = Database.getTable(getModelClass()).get(id);
        LuceneIndexer.instance(getModelClass()).updateDocument(record.getRawRecord());
        return  show(record);
    }
    public View reindex() {
        TaskManager.instance().executeAsync(new ReindexTask<>(getModelClass()),false);
        if (getReturnIntegrationAdaptor() == null) {
            return back();
        }else {
            return getReturnIntegrationAdaptor().createStatusResponse(getPath(),null);
        }
    }

    @RequireLogin(false)
    public View erd(){
        return super.erd(getReflector());
    }
}
