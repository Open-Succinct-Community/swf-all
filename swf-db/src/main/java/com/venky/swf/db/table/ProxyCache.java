package com.venky.swf.db.table;

import com.venky.cache.Cache;
import com.venky.swf.db.model.Model;

public class ProxyCache extends Cache<Class<? extends Model>,Model> {
    /**
     *
     */
    private static final long serialVersionUID = -1436596145516567205L;
    Record record;
    public ProxyCache(){
        //For Serialization.
    }
    public ProxyCache(Record record){
        this.record = record;
    }
    @Override
    protected Model getValue(Class<? extends Model> k) {
        return ModelInvocationHandler.getProxy(k, record);
    }

}