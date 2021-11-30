package com.venky.swf.plugins.background.project;

import com.venky.swf.plugins.background.core.Task;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectTest {

    @Test
    public void test(){
        Project project = ProjectManager.instance().getProject("1");
        for (int i = 0; i < 10 ; i ++){
            project.submit(Arrays.asList(new RandomIntTask(project)));
        }
        project.awaitCompletion();
        System.out.println("All Done!! Hurray");
    }

    static AtomicInteger idGen = new AtomicInteger();
    public static class RandomIntTask implements Task {
        int id = idGen.getAndIncrement();
        Project project;
        public RandomIntTask(Project project){
            this.project = project;
        }
        @Override
        public void execute() {
            try {
                try {
                    if (id%10 == 9) {
                        synchronized (project) {
                            project.wait(100);
                        }
                    }
                }catch (InterruptedException ex){
                }finally {
                    System.out.println("Id: " + id + " Complete!," + System.currentTimeMillis());
                    System.out.flush();
                    ProjectManager.instance().notify(project);
                }
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
    }


}
