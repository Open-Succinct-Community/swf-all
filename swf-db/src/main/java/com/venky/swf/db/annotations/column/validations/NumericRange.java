package com.venky.swf.db.annotations.column.validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface NumericRange {
	public double min() default Double.NEGATIVE_INFINITY;
	public double max() default Double.POSITIVE_INFINITY;
} 
