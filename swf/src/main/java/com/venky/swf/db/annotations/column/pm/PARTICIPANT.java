package com.venky.swf.db.annotations.column.pm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PARTICIPANT {
	public String value() default "DEFAULT";
	public boolean redundant() default false;
	public boolean defaultable() default true;
}
