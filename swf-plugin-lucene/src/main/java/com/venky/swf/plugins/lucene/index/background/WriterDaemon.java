package com.venky.swf.plugins.lucene.index.background;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import com.venky.swf.db.Database;


public class WriterDaemon extends Thread {

	private IndexWriter writer = null;
	private final Directory directory ;
	public WriterDaemon(Directory directory){
		super();
		this.directory = directory;
	}
	
	public IndexWriter getIndexWriter(){
		try {
			if (writer == null){
				writer = new IndexWriter(directory,new IndexWriterConfig(Version.LUCENE_35,new StandardAnalyzer(Version.LUCENE_35)));
			}
			return writer;
		}catch (IOException ex){
			throw new RuntimeException(ex);
		}
	}
	
	private static enum Operation { 
		ADD,
		MODIFY,
		DELETE
	}
	private class Job {
		Operation op;
		Document document;
		public Job(Operation op,Document document){
			this.op = op;
			this.document = document;
		}
		public void perform() throws IOException{
			IndexWriter w = getIndexWriter();
			switch(op){
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
	}
	

	private LinkedList<Job> jobs = new LinkedList<WriterDaemon.Job>();
	private boolean addJob(Job job){
		if (shutingDown){
			return false;
		}
		synchronized (this) {
			try {
				return jobs.add(job);
			}finally {
				notify();
			}
		}
	}
	private boolean shutingDown = false;
	public void shutdown(){
		synchronized (this) {
			shutingDown = true;
			notify();
		}
	}

	public boolean addDocument(Document document){
		return addJob(new Job(Operation.ADD,document));
	}
	
	public boolean updateDocument(Document document){
		return addJob(new Job(Operation.MODIFY,document));
	}
	
	public boolean removeDocument(Document document){
		return addJob(new Job(Operation.DELETE,document));
	}
	
	private Job next(){
		synchronized (this) {
			while(jobs.isEmpty() && !shutingDown){
				try {
					if (writer != null){
						writer.commit();
						Database db = Database.getInstance();
						db.getCurrentTransaction().commit();
						db.close();
					}
					wait();
				}catch (InterruptedException e) {
					//
				}catch (IOException e) {
					shutdown();
				}
			}
			if (!jobs.isEmpty()){
				return jobs.remove();
			}
			return null;
		}
	}
	public void run() {
		Job job = null;
		while((job = next()) != null){
			try {
				job.perform();
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).severe(e.getMessage());
				shutdown();
			}
		}
	}

}
