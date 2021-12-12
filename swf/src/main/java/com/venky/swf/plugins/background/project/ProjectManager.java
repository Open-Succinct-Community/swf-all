package com.venky.swf.plugins.background.project;

import com.venky.cache.Cache;
import com.venky.swf.plugins.background.project.Project.Status;

public class ProjectManager {
    private static ProjectManager instance = new ProjectManager();
    private  ProjectManager(){

    }
    public static ProjectManager instance(){
        return instance;
    }

    Cache<String,Project> projectCache = new Cache<String, Project>(0,0) {
        @Override
        protected Project getValue(String id) {
            return new Project(id);
        }
    };
    public Project getProject(String id){
        return projectCache.get(id);
    }
    public void notify(String id){
        notify(getProject(id));
    }
    public void notify(Project project){
        synchronized (project){
            if (project.getStatus() == Status.COMPLETED && project.getStatus() == Status.CANCELLED) {
                projectCache.remove(project.getId());
            }
            project.notifyAll(); //Wake up tasks waiting on the project
        }
    }
}
