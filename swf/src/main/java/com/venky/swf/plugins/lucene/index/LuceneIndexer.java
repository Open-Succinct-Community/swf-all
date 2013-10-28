package com.venky.swf.plugins.lucene.index;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.plugins.lucene.index.background.IndexManager;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;

public class LuceneIndexer {
	static {
    	Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown",new Extension(){
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
	public static <M extends Model> LuceneIndexer instance (Class<? extends Model> modelClass){
		return instance(ModelReflector.instance(modelClass));
	}
	public static <M extends Model> LuceneIndexer instance (ModelReflector<? extends Model> modelReflector){
		return (LuceneIndexer)indexerCache.get(modelReflector.getTableName());
	}
	
	
	private IgnoreCaseSet indexedColumns = new IgnoreCaseSet();
	private Map<String,Class<? extends Model>> indexedReferenceColumns = new IgnoreCaseMap<Class<? extends Model>>(); 
	private final String tableName ;

	public String getTableName() {
		return tableName;
	}
	
	private LuceneIndexer(String tableName) {
		if (ObjectUtil.isVoid(tableName)){
			throw new NullPointerException("Table name cannot be null!");
		}
		try {
			this.tableName = tableName;
			for (Class<? extends Model> mClass: Database.getTable(tableName).getReflector().getModelClasses()){
				ModelReflector<? extends Model> ref = ModelReflector.instance(mClass);
				for (Method indexedFieldGetter: ref.getIndexedFieldGetters()){
					String indexedColumnName = ref.getColumnDescriptor(ref.getFieldName(indexedFieldGetter)).getName();
					indexedColumns.add(indexedColumnName);
					if (ref.getReferredModelGetters().size() > 0){
						Method referredModelGetter = ref.getReferredModelGetterFor(indexedFieldGetter) ; 
						if (referredModelGetter != null){
							Class<? extends Model> referredModelClass = ref.getReferredModelClass(referredModelGetter);
							indexedReferenceColumns.put(indexedColumnName, referredModelClass);
						}
					}
				}
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasIndexedFields(){
		return !indexedColumns.isEmpty();
	}
	
	public Set<String> getIndexedColumns(){
		return indexedColumns;
	}
	
	private Document getDocument(Record r) throws IOException {
		if (!hasIndexedFields()){
			return null;
		}
		Document doc = new Document();
		boolean addedFields = false;
		for (String columnName: indexedColumns){
			ModelReflector<?> reflector = Database.getTable(tableName).getReflector();
			
			String fieldName = reflector.getFieldName(columnName);
			Object value = reflector.get(r, fieldName);
			
			if (!ObjectUtil.isVoid(value) ){
				TypeRef<?> ref = Database.getJdbcTypeHelper().getTypeRef(reflector.getFieldGetter(fieldName).getReturnType());
				TypeConverter<?> converter = ref.getTypeConverter();
				if (!ref.isBLOB()){
					addedFields = true;
					if (Reader.class.isAssignableFrom(ref.getJavaClass())){
						doc.add(new Field(fieldName,(Reader)converter.valueOf(value)));
					}else{
						Class<? extends Model> referredModelClass = indexedReferenceColumns.get(columnName);
						String sValue = converter.toString(value);
						if (ref.isNumeric() && referredModelClass != null){
							ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
							Model referred = Database.getTable(referredModelClass).get(((Number)converter.valueOf(value)).intValue());
							if (referred != null){ 
								doc.add(new Field(fieldName.substring(0,fieldName.length()-"_ID".length()),
										StringUtil.valueOf(referred.getRawRecord().get(referredModelReflector.getDescriptionField())), 
										Field.Store.YES,Field.Index.ANALYZED));
							}
						}
						doc.add(new Field(fieldName,sValue, Field.Store.YES,Field.Index.ANALYZED));
					}
				}
			}else {
				addedFields = true;
				if (indexedReferenceColumns.containsKey(fieldName)){
					doc.add(new Field(fieldName.substring(0,fieldName.length()-"_ID".length()),
							"NULL", Field.Store.YES,Field.Index.ANALYZED));
				}
				doc.add(new Field(fieldName,"NULL",Field.Store.YES,Field.Index.ANALYZED));
			}
		}
		if (addedFields){
			doc.add(new Field("ID",StringUtil.valueOf(r.getId()), Field.Store.YES,Field.Index.NOT_ANALYZED));
		}else {
			doc = null;
		}
		return doc;
	}
	
	@SuppressWarnings("unchecked")
	public List<Document> getDocuments(String luceneOperation){
		Cache<String,List<Document>> documentsByTable = (Cache<String,List<Document>>)Database.getInstance().getCurrentTransaction().getAttribute(luceneOperation);
		if (documentsByTable == null){
			documentsByTable = new Cache<String, List<Document>>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 3445427618501574899L;

				@Override
				protected List<Document> getValue(String k) {
					return new ArrayList<Document>();
				}
				
			};
			Database.getInstance().getCurrentTransaction().setAttribute(luceneOperation,documentsByTable);
		}
		return documentsByTable.get(tableName);
	}
	
	public void addDocument(Record r) throws IOException{
		if (!hasIndexedFields()){
			return;
		}
		Document doc = getDocument(r);
		if (doc != null){
			getDocuments("lucene.added").add(doc);
		}
	}
	public void updateDocument(Record r) throws IOException{
		if (!hasIndexedFields()){
			return;
		}
		Document doc = getDocument(r);
		if (doc != null){
			getDocuments("lucene.updated").add(doc);
		}
	}
	public void removeDocument(Record r) throws IOException{
		if (!hasIndexedFields()){
			return;
		}
		Document doc = getDocument(r);
		if (doc != null){
			getDocuments("lucene.removed").add(doc);
		}
	}
	
	public List<Integer> findIds(Query q, int numHits){
		final List<Integer> ids = new ArrayList<Integer>();
		fire(q ,numHits,new ResultCollector() {
			public void found(Document d) {
				ids.add(Integer.valueOf(d.getFieldable("ID").stringValue()));
			}
		});
		return ids;
	}
	public Query constructQuery(String queryString){
		String descriptionField = Database.getTable(tableName).getReflector().getDescriptionField();
		String defaultField = null;
		if (indexedColumns.contains(descriptionField)){
			defaultField = descriptionField;
		}else if (!indexedColumns.isEmpty()) {
			defaultField = indexedColumns.first();
			if (defaultField.endsWith("_ID")){
				defaultField = defaultField.substring(0,defaultField.length() - "_ID".length());
			}
		}else {
			defaultField = "ID";
		}
		try {
			return new QueryParser(Version.LUCENE_35,defaultField,new StandardAnalyzer(Version.LUCENE_35)).parse(queryString);
		} catch (ParseException e) {
			MultiException ex = new MultiException("Could not form lucene query for:\n" + queryString + "\n");
			ex.add(e);
			throw ex;
		}
	}
	public void fire(Query q ,int numHits, ResultCollector callback) {
		try {
			if (!hasIndexedFields()){
				return;
			}
			IndexManager.instance().fire(tableName, q, numHits, callback);
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}
	public static void dispose() {
		LuceneIndexer.indexerCache.clear();
	}

}
