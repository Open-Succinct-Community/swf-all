package com.venky.swf.views.model;

import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;

public interface FieldUIMetaProvider {
	
	public boolean isFieldVisible(String fieldName) ;

	public boolean isFieldEditable(String fieldName) ;

	public Kind getFieldProtection(String fieldName) ;
	
}
