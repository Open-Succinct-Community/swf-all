package com.venky.swf.db.annotations.column.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface CONTENT_TYPE {
	public MimeType value() default MimeType.TEXT_PLAIN;
}
