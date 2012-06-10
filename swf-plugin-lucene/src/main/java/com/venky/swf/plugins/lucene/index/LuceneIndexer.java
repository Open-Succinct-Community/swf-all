package com.venky.swf.plugins.lucene.index;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.ModelReflector.FieldGetterMatcher;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.db.annotations.Index;
import com.venky.swf.plugins.lucene.index.background.IndexManager;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;

public class LuceneIndexer {
	private static Cache<String, LuceneIndexer> indexerCache = new Cache<String, LuceneIndexer>() {

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
	
	
	private SequenceSet<String> indexedFields = new SequenceSet<String>();
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
				SequenceSet<Method> indexedFieldGetters = new SequenceSet<Method>();
				ref.loadMethods(indexedFieldGetters, matcher);
				for (Method indexedFieldGetter: indexedFieldGetters){
					indexedFields.add(ref.getFieldName(indexedFieldGetter));
				}
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasIndexedFields(){
		return !indexedFields.isEmpty();
	}
	
	private IndexedGetterMatcher matcher = new IndexedGetterMatcher();
	public class IndexedGetterMatcher extends FieldGetterMatcher {
		@Override
        public boolean matches(Method method){
			return method.isAnnotationPresent(Index.class) && super.matches(method);
		}
	}
	
	
	private Document getDocument(Record r) throws IOException {
		if (!hasIndexedFields()){
			return null;
		}
		Document doc = new Document();
		boolean addedFields = false;
		for (String fieldName: indexedFields){
			Object value = r.get(fieldName);
			if (!ObjectUtil.isVoid(value) ){
				TypeRef ref = Database.getJdbcTypeHelper().getTypeRef(value.getClass());
				TypeConverter converter = ref.getTypeConverter();
				if (!ObjectUtil.equals(value,converter.valueOf(null))){
					if (!ref.isBLOB()){
						addedFields = true;
						if (Reader.class.isAssignableFrom(ref.getJavaClass())){
							doc.add(new Field(fieldName,(Reader)converter.valueOf(value)));
						}else {
							String sValue = converter.toString(value);
							doc.add(new Field(fieldName,sValue, Field.Store.YES,Field.Index.ANALYZED));
						}
					}
				}
			}
		}
		if (addedFields){
			doc.add(new Field("ID",StringUtil.valueOf(r.getId()), Field.Store.YES,Field.Index.NOT_ANALYZED));
		}else {
			doc = null;
		}
		return doc;
	}
	
	public List<Document> getDocuments(String luceneOperation){
		Cache<String,List<Document>> documentsByTable = (Cache<String,List<Document>>)Database.getInstance().getCurrentTransaction().getAttribute(luceneOperation);
		if (documentsByTable == null){
			documentsByTable = new Cache<String, List<Document>>() {
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
		String descriptionField = Database.getTable(tableName).getReflector().getDescriptionColumn();
		String defaultField = null;
		if (indexedFields.contains(descriptionField)){
			defaultField = descriptionField;
		}else if (!indexedFields.isEmpty()) {
			defaultField = indexedFields.first();
		}else {
			defaultField = "ID";
		}
		try {
			return new QueryParser(Version.LUCENE_35,defaultField,new StandardAnalyzer(Version.LUCENE_35)).parse(queryString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
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

}
