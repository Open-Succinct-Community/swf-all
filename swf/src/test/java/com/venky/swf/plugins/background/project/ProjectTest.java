package com.venky.swf.plugins.background.project;

import com.venky.swf.plugins.background.core.Task;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectTest {

    @Test
    public void test(){
        Project project = new Project();
        for (int i = 0; i < 5 ; i ++){
            project.submit(Arrays.asList(new RandomIntTask()));
        }
        project.awaitCompletion();
        System.out.println("All Done!! Hurray");
    }

    static AtomicInteger idGen = new AtomicInteger();
    public static class RandomIntTask implements Task {
        int id = idGen.getAndIncrement();
        public RandomIntTask(){

        }
        @Override
        public void execute() {
            try {
                Thread.sleep(3000);
                System.out.println("Id: " + id + " Complete!," + System.currentTimeMillis());
                System.out.flush();
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
    }


}
