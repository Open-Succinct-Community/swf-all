package com.venky.swf.db.model.status;

import in.succinct.json.JSONObjectWrapper;

public class ServerStatus extends JSONObjectWrapper {
    public String getAsyncServer() {
        return get("async_server");
    }

    public void setAsyncServer(String async_server) {
        set("async_server", async_server);
    }

    public int getJobCount() {
        return getInteger("job_count");
    }

    public void setJobCount(int job_count) {
        set("job_count", job_count);
    }


    public int getNumWorkers(){
        return getInteger("num_workers");
    }
    public void setNumWorkers(int num_workers){
        set("num_workers",num_workers);
    }

    public RouterStatus getRouterStatus(){
        return getEnum(RouterStatus.class, "router_status");
    }
    public void setRouterStatus(RouterStatus router_status){
        setEnum("router_status",router_status);
    }

}