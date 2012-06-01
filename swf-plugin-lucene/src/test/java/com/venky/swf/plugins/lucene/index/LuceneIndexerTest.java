package com.venky.swf.plugins.lucene.index;

import java.sql.SQLException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import com.venky.swf.plugins.lucene.db.model.Sample;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.routing.Router;

public class LuceneIndexerTest {
	@Before
	public void setUp(){ 
		Router.instance().setLoader(getClass().getClassLoader());
		Database.getTable(IndexFile.class).truncate();
		try {
			Database.getInstance().getCurrentTransaction().commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		createSample("Jack");
		createSample("Venky");
	}
	
	private void createSample(String name){
		Sample sample = Database.getTable(Sample.class).newRecord();
		sample.setName(name);
		sample.save();
	}

	@Test
	public void test() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			//Simple sleep.
		}
		Database db = null ;
		try {
			db = Database.getInstance();
			db.open(null);
			
			LuceneIndexer indexer = LuceneIndexer.instance(Sample.class);
			Query q = indexer.constructQuery("j*");
			indexer.fire(q, 10, new ResultCollector() {
				int i =1;
				public void found(Document doc) {
					System.out.println("Document #"+ i);
					for (Fieldable f:doc.getFields()){
						System.out.println(f.name() +":" + f.stringValue());
					}
					
				}
			});
			Database.getInstance().getCurrentTransaction().commit();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}finally{
			Database.getInstance().close();
		}
	}

}
