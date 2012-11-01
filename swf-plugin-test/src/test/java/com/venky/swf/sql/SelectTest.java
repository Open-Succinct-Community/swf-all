package com.venky.swf.sql;

import static junit.framework.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Router;
import com.venky.swf.test.db.model.SomeTransactionModel;

public class SelectTest {
	
	@BeforeClass
	public static void beforeAllTests(){
		Router.instance().setLoader(SelectTest.class.getClassLoader());
		Database.getTable(SomeTransactionModel.class).truncate();
		Database.getInstance().getCurrentTransaction().commit();
		Database.getInstance().close();
	}
	@Before
	public void setup(){
		User root = new Select().from(User.class).where(new Expression("NAME",Operator.EQ,"root")).execute(User.class).get(0);
		Database.getInstance().open(root);
	}
	
	@After
	public void tearDown(){
		Database.getInstance().getCurrentTransaction().rollback(null);
		Database.getInstance().close();
	}
	@Test
	public void testCacheRollback() {
		Transaction t2 = Database.getInstance().createTransaction();
		Transaction t3 = Database.getInstance().createTransaction();

		assertEquals(t3,Database.getInstance().getCurrentTransaction());

		ModelReflector<SomeTransactionModel> ref = ModelReflector.instance(SomeTransactionModel.class);
		Select sel = new Select().from(SomeTransactionModel.class);
		assertEquals(0,sel.execute().size());
		
		Table<SomeTransactionModel> t = Database.getTable(SomeTransactionModel.class);
		SomeTransactionModel r = t.newRecord();
		r.setSomeInt(0);
		r.save();
		assertEquals(1,sel.execute().size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());

		t3.rollback(null);
		assertEquals(t2,Database.getInstance().getCurrentTransaction());

		assertNull(Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false));
		assertEquals(0,sel.execute().size());
		assertEquals(0,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());
		
		t2.rollback(null);
	}
	@Test
	public void testCacheCommit() {
		ModelReflector<SomeTransactionModel> ref = ModelReflector.instance(SomeTransactionModel.class);
		Select sel = new Select().from(SomeTransactionModel.class);
		Table<SomeTransactionModel> t = Database.getTable(SomeTransactionModel.class);
		t.truncate();
		
		Transaction t2 = Database.getInstance().createTransaction();
		assertEquals(t2,Database.getInstance().getCurrentTransaction());
		assertEquals(0,sel.execute().size());
		
		SomeTransactionModel r = t.newRecord();
		r.setSomeInt(0);
		r.save();
		assertEquals(1,sel.execute().size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());
		
		Transaction t3 = Database.getInstance().createTransaction();
		assertEquals(t3,Database.getInstance().getCurrentTransaction());
		assertEquals(1,sel.execute().size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(1,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());
		
		SomeTransactionModel m2 = t.newRecord();
		m2.setSomeInt(0);
		m2.save();
		assertEquals(2,sel.execute().size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());

		t3.commit();
		
		assertEquals(t2,Database.getInstance().getCurrentTransaction());

		assertNotNull(Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true));
		assertEquals(2,sel.execute().size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());
		
		t2.commit();
		
		assertNotNull(Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true));
		assertEquals(2,sel.execute().size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, true).size());
		assertEquals(2,Database.getInstance().getCurrentTransaction().getCache(ref).getCachedResult(null, 0, false).size());
		
	}

}
