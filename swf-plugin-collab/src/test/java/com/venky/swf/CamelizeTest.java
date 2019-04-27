package com.venky.swf;

import com.venky.core.string.StringUtil;
import org.junit.Test;

public class CamelizeTest {
    @Test
    public void test(){
        System.out.println(StringUtil.camelize("WELLNESS CARE-SHAMPOO, SKINCOAT, IMMUNO BOOSTER, ANTI-TICK POWDER, ANTI-TICK SPOT ON 0 - 10 Kg"));
    }
}
