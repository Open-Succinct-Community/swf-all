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
	public WriterDaemon(Directory directory){
		super();
		try {
			this.writer = new IndexWriter(directory,new IndexWriterConfig(Version.LUCENE_35,new StandardAnalyzer(Version.LUCENE_35)));
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
			switch(op){
			case ADD:
				writer.addDocument(document);
				break;
			case MODIFY:
				writer.deleteDocuments(new Term("ID",document.getFieldable("ID").stringValue()));
				writer.addDocument(document);
				break;
			case DELETE: 
				writer.deleteDocuments(new Term("ID",document.getFieldable("ID").stringValue()));
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
			while(jobs.isEmpty()){
				try {
					if (writer != null){
						writer.commit();
					}
					Database.getInstance().close();/*Release connection back to pool!. 
													We will auto open on demand.*/
					if (shutingDown){
						break;
					}
					wait();
				}catch (InterruptedException e) {
					if (shutingDown || !jobs.isEmpty()){
						break;
					}
				}catch (IOException e) {
					throw new RuntimeException(e);
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
