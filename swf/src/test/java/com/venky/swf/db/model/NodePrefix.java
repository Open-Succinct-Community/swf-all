package com.venky.swf.db.model;

import org.junit.Test;

public class NodePrefix {
    @Test
    public void test1(){
        System.out.println(Long.toHexString((1L << 44) | 1L));
    }
}
