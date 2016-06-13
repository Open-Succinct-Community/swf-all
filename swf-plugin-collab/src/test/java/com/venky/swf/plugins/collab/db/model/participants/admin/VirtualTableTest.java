package com.venky.swf.plugins.collab.db.model.participants.admin;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;

public class VirtualTableTest {

	@SuppressWarnings("unchecked")
	@Test
	public void test() {
		Class<? extends Model> modelClass = (Class<? extends Model>) Table.modelClass("COUNTS");
		ModelReflector<? extends Model> ref = ModelReflector.instance((Class<? extends Model>) modelClass);
		assertTrue("modelClass is null",ref != null);
		
	}

}
