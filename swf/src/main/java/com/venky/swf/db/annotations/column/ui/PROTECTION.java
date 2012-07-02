package com.venky.swf.db.annotations.column.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface PROTECTION {
	public Kind value() default Kind.DISABLED;
	public static enum Kind { 
		EDITABLE,
		DISABLED,
		NON_EDITABLE,
	}
}
