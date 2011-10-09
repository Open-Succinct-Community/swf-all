/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author venky
 */
public class ReflectionTest {
    
    public ReflectionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void test1() throws Exception{
        
        Method m = getClass().getMethod("getList");
        ParameterizedType type = (ParameterizedType)m.getGenericReturnType();
        System.out.println(type);
        System.out.println(type.getActualTypeArguments()[0]);
    }
    
    public List<Integer> getList(){
        return null;
    }
}
