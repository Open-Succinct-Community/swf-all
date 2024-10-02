package com.venky.swf.db.model.status;

import com.venky.swf.db.model.status.AsyncServerStatus.AsyncServerStatuses;
import in.succinct.json.JSONObjectWrapper;

public class ServerStatus extends JSONObjectWrapper {


    public RouterStatus getRouterStatus(){
        return getEnum(RouterStatus.class, "router_status");
    }
    public void setRouterStatus(RouterStatus router_status){
        setEnum("router_status",router_status);
    }

    public AsyncServerStatuses getAsyncServerStatuses(){
        return get(AsyncServerStatuses.class, "async_server_statuses");
    }
    public void setAsyncServerStatuses(AsyncServerStatuses async_server_statuses){
        set("async_server_statuses",async_server_statuses);
    }

}