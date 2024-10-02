package com.venky.swf.db.model.status;

import in.succinct.json.JSONObjectWrapper;
import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;

public class AsyncServerStatus extends JSONObjectWrapper {
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
    public static class AsyncServerStatuses extends ObjectWrappers<AsyncServerStatus>{
        public AsyncServerStatuses() {
        }

        public AsyncServerStatuses(JSONArray value) {
            super(value);
        }

        public AsyncServerStatuses(String payload) {
            super(payload);
        }
    }
}
