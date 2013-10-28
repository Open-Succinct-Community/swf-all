package com.venky.swf.plugins.lucene.index.background;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
		return new IndexWriter(IndexManager.instance().getDirectory(directory),
				new IndexWriterConfig(Version.LUCENE_35,new StandardAnalyzer(Version.LUCENE_35)));
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
					w.deleteDocuments(new Term("ID",document.getFieldable("ID").stringValue()));
					w.addDocument(document);
					break;
				case DELETE: 
					w.deleteDocuments(new Term("ID",document.getFieldable("ID").stringValue()));
					break;
				}
			}
			w.close(false);
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
	
}
