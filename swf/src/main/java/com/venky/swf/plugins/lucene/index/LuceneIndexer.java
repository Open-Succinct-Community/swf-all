package com.venky.swf.plugins.lucene.index;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.index.background.IndexManager;
import com.venky.swf.plugins.lucene.index.common.IndexUpdateTracker;
import com.venky.swf.plugins.lucene.index.common.IndexUpdateTracker.IndexOperation;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserTokenManager;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LuceneIndexer {
    static {
        Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown", new Extension() {
            @Override
            public void invoke(Object... context) {
                dispose();
            }
        });
    }

    private static Cache<String, LuceneIndexer> indexerCache = new Cache<String, LuceneIndexer>() {

        /**
         *
         */
        private static final long serialVersionUID = -1207573047245047343L;

        @Override
        protected LuceneIndexer getValue(String tableName) {
            return new LuceneIndexer(tableName);
        }
    };

    public static <M extends Model> LuceneIndexer instance(Class<? extends Model> modelClass) {
        return instance(ModelReflector.instance(modelClass));
    }

    public static <M extends Model> LuceneIndexer instance(ModelReflector<? extends Model> modelReflector) {
        return indexerCache.get(modelReflector.getTableName());
    }
    public static LuceneIndexer instance(String tableName) {
        return indexerCache.get(tableName);
    }


    private final IgnoreCaseSet indexedColumns = new IgnoreCaseSet();
    private final Map<String, Class<? extends Model>> indexedReferenceColumns = new IgnoreCaseMap<Class<? extends Model>>();
    private final String tableName;

    public String getTableName() {
        return tableName;
    }

    private LuceneIndexer(String tableName) {
        if (ObjectUtil.isVoid(tableName)) {
            throw new NullPointerException("Table name cannot be null!");
        }
        try {
            this.tableName = tableName;
            ModelReflector<? extends Model> ref = Objects.requireNonNull(Database.getTable(tableName)).getReflector();
            for (Method indexedFieldGetter : ref.getIndexedFieldGetters()) {
                String indexedColumnName = ref.getColumnDescriptor(ref.getFieldName(indexedFieldGetter)).getName();
                indexedColumns.add(indexedColumnName);
                if (!ref.getReferredModelGetters().isEmpty()) {
                    Method referredModelGetter = ref.getReferredModelGetterFor(indexedFieldGetter);
                    if (referredModelGetter != null) {
                        Class<? extends Model> referredModelClass = ref.getReferredModelClass(referredModelGetter);
                        indexedReferenceColumns.put(indexedColumnName, referredModelClass);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasIndexedFields() {
        return !indexedColumns.isEmpty();
    }

    public Set<String> getIndexedColumns() {
        return indexedColumns;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder();
        boolean isAlphaNumericOnly = true;
        boolean isNumeric = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= '0' && c<= '9') || c == '.'){
                sanitized.append(c);
            }else if ((c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') || (c == ' ')) {
                isNumeric = false;
                sanitized.append(c);
            } else if (!sanitized.isEmpty()) {
                isNumeric = false;
                isAlphaNumericOnly = false;
                if (sanitized.charAt(sanitized.length() - 1) != ' ') {
                    sanitized.append(' ');
                }
            }
        }

        if (!isNumeric){
            sanitized.append(" LUCENE_START ").append(value).append(" LUCENE_END"); //To allow exact match queries with special characters also.
        }
        return sanitized.toString();
    }

    public void addTextField(Document doc, String fieldName, String value, Store store){
        doc.add(new TextField(fieldName,sanitize(value),store));
    }
    public void addDateField(Document doc, String fieldName , String dateValue, Store store){
        doc.add(new TextField(fieldName, sanitizeTs(dateValue), Field.Store.YES));
    }
    public void addLongField(Document doc, String name, Long value) {
        doc.add(new LongPoint(name,value));
    }

    public Document getDocument(Record r) throws IOException {
        if (!hasIndexedFields()) {
            return null;
        }
        Document doc = new Document();
        boolean addedFields = false;
        ModelReflector<?> reflector = Objects.requireNonNull(Database.getTable(tableName)).getReflector();
        for (String columnName : indexedColumns) {
            String fieldName = reflector.getFieldName(columnName);
            Object value = reflector.get(r, fieldName);

            String parentName = fieldName.endsWith("_ID") ? fieldName.substring(0, fieldName.length() - "_ID".length()) : null;

            if (!ObjectUtil.isVoid(value)) {
                TypeRef<?> ref = Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(reflector.getFieldGetter(fieldName).getReturnType());
                TypeConverter<?> converter = ref.getTypeConverter();
                if (!ref.isBLOB()) {
                    addedFields = true;
                    if (Reader.class.isAssignableFrom(ref.getJavaClass())) {
                        addTextField(doc,fieldName,converter.toString(value), Store.NO);
                    } else if (String[].class.isAssignableFrom(ref.getJavaClass())) {
                        StringBuilder builder = new StringBuilder();
                        String[] sValue = (String[])value;
                        for (String s: sValue){
                            builder.append(sanitize(s)).append(" ");
                        }
                        if (!builder.isEmpty()) {
                            doc.add(new TextField(fieldName,builder.toString(),Store.YES));
                        }else {
                            addTextField(doc,fieldName,"NULL",Store.YES);
                        }
                    } else {
                        Class<? extends Model> referredModelClass = indexedReferenceColumns.get(columnName);
                        String sValue = converter.toStringISO(value);
                        if (ref.isNumeric()) {
                            if (referredModelClass != null) {
                                ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
                                Model referred = Database.getTable(referredModelClass).get(((Number) converter.valueOf(value)).longValue());
                                if (referred != null) {
                                    addTextField(doc,parentName,StringUtil.valueOf(referredModelReflector.get(referred,referredModelReflector.getDescriptionField())),Store.YES);
                                }
                            }
                            addTextField(doc,fieldName,sValue,Store.YES);
                        } else if (ref.isDate() || ref.isTimestamp()) {
                            addDateField(doc,fieldName,sValue,Store.YES);
                        } else {
                            addTextField(doc,fieldName,sValue,Store.YES);
                        }
                    }
                }
            } else {
                addedFields = true;
                if (indexedReferenceColumns.containsKey(fieldName)) {
                    addTextField(doc,parentName,"NULL",Store.YES);
                }
                addTextField(doc,fieldName,"NULL",Store.YES);
            }
        }
        if (addedFields) {
            addTextField(doc,"ID",StringUtil.valueOf(r.getId()), Store.YES);
            addLongField(doc,"_ID",r.getId());
            if (GeoLocation.class.isAssignableFrom(reflector.getModelClass())){
                if (indexedColumns.contains("LAT") || indexedColumns.contains("LNG")){
                    addGeoField(doc, reflector ,r.get("LAT") , r.get("LNG"));
                }
            }
        } else {
            doc = null;
        }
        return doc;
    }
    
    private void addGeoField(Document doc, ModelReflector<?> reflector, Object lat, Object lng) {
        if (lat != null && lng != null) {
            TypeConverter<BigDecimal> converter = reflector.getJdbcTypeHelper().getTypeRef(BigDecimal.class).getTypeConverter();
            doc.add(new LatLonPoint("_GEO_LOCATION_", converter.valueOf(lat).doubleValue(), converter.valueOf(lng).doubleValue()));
        }
    }
    
    
    private String sanitizeTs(String value) {
        return value.replaceAll("[- :]", "");
    }

    public void update(Record record, IndexOperation operation){
        IndexUpdateTracker tracker = Database.getInstance().getCurrentTransaction().getAttribute(IndexUpdateTracker.class.getName());
        if (tracker == null){
            tracker = new IndexUpdateTracker();
            Database.getInstance().getCurrentTransaction().setAttribute(IndexUpdateTracker.class.getName(),tracker);
        }
        tracker.update(tableName,record,operation);
    }


    public void addDocument(Record r) throws IOException {
        if (!hasIndexedFields() || r == null) {
            return;
        }
        update(r,IndexOperation.added);
    }

    public void updateDocument(Record r) throws IOException {
        if (!hasIndexedFields() || r == null) {
            return;
        }
        update(r,IndexOperation.updated);
    }

    public void removeDocument(Record r) throws IOException {
        if (!hasIndexedFields() || r == null) {
            return;
        }
        update(r,IndexOperation.removed);
    }

    public List<Long> findIds(Query q, int numHits) {
        final List<Long> ids = new SequenceSet<>();
        
        ResultCollector resultCollector  =new ResultCollector() {
            
            public void collect(Document d, ScoreDoc scoreDoc) {
                ids.add(Long.valueOf(d.getField("ID").stringValue()));
            }
            
            @Override
            public boolean isEnough() {
                return ids.size() > numHits;
            }
            
        };
        fire(q, numHits, resultCollector);
        return ids;
    }
    

    public Query constructQuery(String queryString) {
        String descriptionField = Database.getTable(tableName).getReflector().getDescriptionField();
        String defaultField = null;
        if (indexedColumns.contains(descriptionField)) {
            defaultField = descriptionField;
        } else if (!indexedColumns.isEmpty()) {
            defaultField = indexedColumns.first();
            if (defaultField.endsWith("_ID")) {
                defaultField = defaultField.substring(0, defaultField.length() - "_ID".length());
            }
        } else {
            defaultField = "ID";
        }
        try {
            //return new QueryParser(Version.LUCENE_35,defaultField,new StandardAnalyzer(Version.LUCENE_35)).parse(queryString);
            return new IntelligentQueryParser(defaultField, new StandardAnalyzer(new CharArraySet(new ArrayList<String>(),false))).parse(queryString);

        } catch (ParseException e) {
            MultiException ex = new MultiException("Could not form lucene query for:\n" + queryString + "\n");
            ex.add(e);
            throw ex;
        }
    }
    
    public int count(Query q) {
        if (!hasIndexedFields()){
            return 0;
        }
        return IndexManager.getInstance().count(tableName,q);
    }
    public void fire(Query q, int numHits, ResultCollector callback) {
        if (!hasIndexedFields()) {
            return;
        }
        IndexManager.getInstance().fire(tableName, q, numHits, callback);
    }

    public static void dispose() {
        LuceneIndexer.indexerCache.clear();
    }


    public static class IntelligentQueryParser extends QueryParser {

        public IntelligentQueryParser(String f, Analyzer a) {
            super(f, a);
        }


        protected IntelligentQueryParser(QueryParserTokenManager tm) {
            super(tm);
        }

        // take over super constructors
        @Override
        protected org.apache.lucene.search.Query newRangeQuery(String field,
                                                               String part1, String part2, boolean part1Inclusive, boolean part2Inclusive) {
            if ("_ID".equals(field)) {
                return LongPoint.newRangeQuery(field, Long.parseLong(part1), Long.parseLong(part2));
            }
            return super.newRangeQuery(field, part1, part2, part1Inclusive, part2Inclusive);
        }



        @Override
        protected org.apache.lucene.search.Query newTermQuery(
                org.apache.lucene.index.Term term,float boost) {
            if ("_ID".equals(term.field())) {
                return LongPoint.newExactQuery(term.field(), Long.parseLong(term.text()));
            }
            return super.newTermQuery(term,boost);
        }
    }

    public boolean isIndexingEnabled(){
        Boolean enabled = Database.getInstance().getCurrentTransaction().getAttribute(getTableName() +".indexing_enabled");
        if (enabled == null){
            enabled = true;
        }
        return enabled;
    }

    public void setIndexingEnabled(boolean enabled){
        Database.getInstance().getCurrentTransaction().setAttribute(getTableName()+".indexing_enabled",enabled);
    }

}
