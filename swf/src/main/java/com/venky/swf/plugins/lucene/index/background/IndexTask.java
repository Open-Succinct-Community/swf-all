package com.venky.swf.plugins.lucene.index.background;

import java.io.IOException;
import java.util.List;

import com.venky.cache.Cache;
import com.venky.cache.UnboundedCache;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LiveIndexWriterConfig;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.venky.swf.plugins.background.core.Task;

public class IndexTask implements Task{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5049941893423693143L;
	private String directory;
	private List<Record> documents;
	private Operation operation;
	public static enum Operation { 
		ADD,
		MODIFY,
		DELETE
	}

	public IndexTask() {
	
	}
	private IndexWriter getIndexWriter() throws CorruptIndexException, LockObtainFailedException, IOException{
		DatabaseDirectory.getIndexDirectory(directory,true);
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		config.setCommitOnClose(true);
		config.setMergeScheduler(new SerialMergeScheduler());
		return new IndexWriter(IndexManager.instance().getDirectory(directory), config);
	}

	@Override
	public void execute() {
		try {
			IndexWriter w = getIndexWriter();
			for (Record record: getDocuments()){
				Document document = LuceneIndexer.instance(Database.getTable(getDirectory()).getModelClass()).getDocument(record);
				switch(getOperation()){
					case ADD:
						w.addDocument(document);
						break;
					case MODIFY:
						w.deleteDocuments(new Term("ID",document.getField("ID").stringValue()));
						w.addDocument(document);
						break;
					case DELETE:
						w.deleteDocuments(new Term("ID",document.getField("ID").stringValue()));
						break;
				}
			}
			w.prepareCommit();
			w.commit();
			w.close();
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public List<Record> getDocuments() {
		return documents;
	}

	public void setDocuments(List<Record> documents) {
		this.documents = documents;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	
}
