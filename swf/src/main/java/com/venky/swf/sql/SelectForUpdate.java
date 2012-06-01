package com.venky.swf.sql;

public class SelectForUpdate extends Select{
	protected void finalizeParameterizedSQL(){
		super.finalizeParameterizedSQL();
		getQuery().append(" FOR UPDATE ");
	}
}
