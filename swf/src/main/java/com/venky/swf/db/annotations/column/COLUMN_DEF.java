package com.venky.swf.db.annotations.column;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.venky.swf.db.annotations.column.defaulting.StandardDefault;



@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface COLUMN_DEF {
	StandardDefault value()  default StandardDefault.NULL;
	String someValue() default "";
}
