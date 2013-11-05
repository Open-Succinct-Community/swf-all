package com.venky.swf.db.annotations.column.relationship;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface CONNECTED_VIA {
	public String value(); 
	public String additional_join() default "" ;
}
