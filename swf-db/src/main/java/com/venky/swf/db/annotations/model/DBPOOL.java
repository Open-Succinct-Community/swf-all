package com.venky.swf.db.annotations.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by venky on 21/5/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})

public @interface DBPOOL {
    public String value() default  "" ;
}
