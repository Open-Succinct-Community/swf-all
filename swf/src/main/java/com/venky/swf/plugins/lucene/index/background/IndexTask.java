package com.venky.swf.plugins.lucene.index.background;

import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.IndexTaskManager;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IndexTask implements Task{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5049941893423693143L;
	private String directory;
	private List<Document> documents;
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
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer(new CharArraySet(new ArrayList<>(),false)));
		config.setCommitOnClose(true);
		config.setMergeScheduler(new SerialMergeScheduler());
		return new IndexWriter(IndexManager.instance().getDirectory(directory), config);
	}

	@Override
	public void execute() {
		try {
			IndexWriter w = getIndexWriter();
			for (Document document: getDocuments()){
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

	public List<Document> getDocuments() {
		return documents;
	}

	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	@Override
	public Priority getTaskPriority() {
		return Priority.LOW;
	}

	@Override
	public IndexTaskManager getAsyncTaskManager() {
		return AsyncTaskManagerFactory.getInstance().get(IndexTaskManager.class);
	}
}
