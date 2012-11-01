package com.venky.swf.db.annotations.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ORDER_BY {
	public static final String DEFAULT = " ID DESC ";
	public String value() default DEFAULT ;
}
