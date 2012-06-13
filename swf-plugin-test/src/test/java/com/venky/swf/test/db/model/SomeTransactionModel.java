package com.venky.swf.test.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.TRANSACTION;
import com.venky.swf.db.model.Model;

@TRANSACTION
public interface SomeTransactionModel extends Model {
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getSomeInt();
	public void setSomeInt(int someInt);
}
